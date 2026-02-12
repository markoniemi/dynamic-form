import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { ItemForm } from '../../src/pages/ItemForm';
import { useAuth } from 'react-oidc-context';
import { BrowserRouter, useParams } from 'react-router-dom';
import { itemClient } from '../../src/services/itemClient';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

// Mock dependencies
vi.mock('react-oidc-context');
vi.mock('../../src/services/itemClient', () => ({
  itemClient: {
    getItem: vi.fn(),
    createItem: vi.fn(),
    updateItem: vi.fn(),
  },
}));

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useParams: vi.fn(),
  };
});

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
    },
  },
});

function renderItemForm() {
  render(
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <ItemForm />
      </BrowserRouter>
    </QueryClientProvider>
  );
}

describe('ItemForm Component', () => {
  const mockUser = {
    access_token: 'mock-token',
    profile: { sub: 'test-user' },
  };

  beforeEach(() => {
    vi.clearAllMocks();
    (useAuth as any).mockReturnValue({
      isAuthenticated: true,
      user: mockUser,
    });
    (useParams as any).mockReturnValue({});
  });

  it('renders add item form by default', () => {
    renderItemForm();
    expect(screen.getByText('Add New Item')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Enter item name')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Enter item description')).toBeInTheDocument();
  });

  it('validates required fields', async () => {
    renderItemForm();
    
    fireEvent.click(screen.getByText('Save Item'));

    await waitFor(() => {
      expect(screen.getByText('Name is required')).toBeInTheDocument();
    });
  });

  it('submits new item successfully', async () => {
    vi.mocked(itemClient.createItem).mockResolvedValue({ id: 1, name: 'New Item', description: 'Desc' } as any);
    
    renderItemForm();

    fireEvent.change(screen.getByPlaceholderText('Enter item name'), { target: { value: 'New Item' } });
    fireEvent.change(screen.getByPlaceholderText('Enter item description'), { target: { value: 'Desc' } });
    
    fireEvent.click(screen.getByText('Save Item'));

    await waitFor(() => {
      expect(itemClient.createItem).toHaveBeenCalledWith(
        { name: 'New Item', description: 'Desc' },
        'mock-token'
      );
      expect(mockNavigate).toHaveBeenCalledWith('/');
    });
  });

  it('loads and updates existing item', async () => {
    (useParams as any).mockReturnValue({ id: '1' });
    vi.mocked(itemClient.getItem).mockResolvedValue({ id: 1, name: 'Existing Item', description: 'Old Desc' } as any);
    vi.mocked(itemClient.updateItem).mockResolvedValue({ id: 1, name: 'Updated Item', description: 'New Desc' } as any);

    renderItemForm();

    // Wait for loading to finish and form to populate
    await waitFor(() => {
      expect(screen.getByDisplayValue('Existing Item')).toBeInTheDocument();
    });

    fireEvent.change(screen.getByPlaceholderText('Enter item name'), { target: { value: 'Updated Item' } });
    fireEvent.change(screen.getByPlaceholderText('Enter item description'), { target: { value: 'New Desc' } });
    
    fireEvent.click(screen.getByText('Save Item'));

    await waitFor(() => {
      expect(itemClient.updateItem).toHaveBeenCalledWith(
        1,
        { name: 'Updated Item', description: 'New Desc' },
        'mock-token'
      );
      expect(mockNavigate).toHaveBeenCalledWith('/');
    });
  });

  it('handles API errors', async () => {
    vi.mocked(itemClient.createItem).mockRejectedValue(new Error('API Error'));
    
    renderItemForm();

    fireEvent.change(screen.getByPlaceholderText('Enter item name'), { target: { value: 'New Item' } });
    fireEvent.click(screen.getByText('Save Item'));

    await waitFor(() => {
      expect(screen.getByText('API Error')).toBeInTheDocument();
    });
  });
});

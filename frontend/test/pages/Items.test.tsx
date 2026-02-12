import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { Items } from '../../src/pages/Items';
import { useAuth } from 'react-oidc-context';
import { BrowserRouter } from 'react-router-dom';
import { itemClient } from '../../src/services/itemClient';

import { ItemDto } from '../../src/types/ItemDto.ts';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

// Mock dependencies
vi.mock('react-oidc-context');
vi.mock('../../src/services/itemClient', () => ({
  itemClient: {
    getItems: vi.fn(),
    deleteItem: vi.fn(),
  },
}));

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...(actual as any),
    useNavigate: () => mockNavigate,
  };
});
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
    },
  },
});

function renderItems() {
  render(
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Items />
      </BrowserRouter>
    </QueryClientProvider>
  );
}

describe('Items Component', () => {
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
    window.confirm = vi.fn(() => true); // Auto-confirm deletions
    queryClient.clear();
  });

  it('renders loading state initially', async () => {
    vi.mocked(itemClient.getItems).mockImplementation(() => new Promise(() => {})); // Never resolves
    renderItems();

    expect(screen.getByRole('status')).toBeInTheDocument();
  });

  it('renders items when loaded successfully', async () => {
    const mockItems: ItemDto[] = [
      { id: 1, name: 'Item 1', description: 'Desc 1', createdAt: new Date().toISOString() },
      { id: 2, name: 'Item 2', description: 'Desc 2', createdAt: new Date().toISOString() },
    ];

    vi.mocked(itemClient.getItems).mockResolvedValue(mockItems);
    renderItems();

    await waitFor(() => {
      expect(screen.getByText('Item 1')).toBeInTheDocument();
      expect(screen.getByText('Item 2')).toBeInTheDocument();
    });
  });

  it('renders error message when loading fails', async () => {
    vi.mocked(itemClient.getItems).mockRejectedValue(new Error('Failed to fetch'));
    renderItems();

    await waitFor(() => {
      expect(screen.getByText('Failed to fetch')).toBeInTheDocument();
    });
  });

  it('renders "No items found" when list is empty', async () => {
    vi.mocked(itemClient.getItems).mockResolvedValue([]);
    renderItems();

    await waitFor(() => {
      expect(screen.getByText('No items found')).toBeInTheDocument();
    });
  });

  it('deletes an item when delete button is clicked', async () => {
    const mockItems: ItemDto[] = [
      { id: 1, name: 'Item to Delete', description: 'Desc', createdAt: new Date().toISOString() },
    ];

    vi.mocked(itemClient.getItems)
      .mockResolvedValueOnce(mockItems) // For initial fetch
      .mockResolvedValueOnce([]); // For refetch after deletion

    vi.mocked(itemClient.deleteItem).mockResolvedValue(undefined);

    renderItems();

    // Wait for the item to appear
    await screen.findByText('Item to Delete');

    // There is only one item, so only one delete button
    fireEvent.click(screen.getByText('Delete'));

    // Wait for the item to be removed from the document
    await waitFor(() => {
      expect(screen.queryByText('Item to Delete')).not.toBeInTheDocument();
    });

    // Verify that the delete function was called
    expect(itemClient.deleteItem).toHaveBeenCalledWith(1, 'mock-token');
  });
});

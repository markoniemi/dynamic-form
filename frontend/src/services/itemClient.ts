import { http } from './http';
import { ItemDto } from '../types/ItemDto';

export const itemClient = {
  async getItems(token: string): Promise<ItemDto[]> {
    return http.request<ItemDto[]>('/items', { token });
  },

  async getItem(id: number, token: string): Promise<ItemDto> {
    return http.request<ItemDto>(`/items/${id}`, { token });
  },

  async createItem(
    item: Omit<ItemDto, 'id' | 'createdAt' | 'updatedAt'>,
    token: string
  ): Promise<ItemDto> {
    return http.request<ItemDto>('/items', {
      method: 'POST',
      body: JSON.stringify(item),
      token,
    });
  },

  async updateItem(id: number, item: Partial<ItemDto>, token: string): Promise<ItemDto> {
    return http.request<ItemDto>(`/items/${id}`, {
      method: 'PUT',
      body: JSON.stringify(item),
      token,
    });
  },

  async deleteItem(id: number, token: string): Promise<void> {
    await http.request<void>(`/items/${id}`, {
      method: 'DELETE',
      token,
    });
  },
};

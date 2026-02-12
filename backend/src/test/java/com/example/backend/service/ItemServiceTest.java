package com.example.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.example.backend.entity.Item;
import com.example.backend.repository.ItemRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {
  @Mock private ItemRepository itemRepository;

  @InjectMocks private ItemService itemService;
  private Item item;

  @BeforeEach
  void setUp() {
    item = new Item("Test Item");
    item.setId(1L);
  }

  @Test
  void getItemById() {
    when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
    Optional<Item> found = itemService.getItemById(1L);
    assertTrue(found.isPresent(), "Item should be present");
    assertEquals(item.getName(), found.get().getName(), "Item name should match");
  }

  @Test
  void createItem() {
    when(itemRepository.save(any(Item.class))).thenReturn(item);
    Item created = itemService.createItem(new Item());
    assertEquals(item.getName(), created.getName(), "Created item name should match");
  }
}

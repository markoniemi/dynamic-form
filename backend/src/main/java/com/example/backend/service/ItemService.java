package com.example.backend.service;

import com.example.backend.entity.Item;
import com.example.backend.repository.ItemRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.example.log.InterfaceLog;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@InterfaceLog
public class ItemService {
  private final ItemRepository itemRepository;

  @InterfaceLog
  public Item createItem(@Valid Item item) {
    Validate.isTrue(itemRepository.findByName(item.getName()) == null, "existing.item.name");
    return itemRepository.save(item);
  }

  @InterfaceLog
  public Optional<Item> getItemById(@NotNull Long id) {
    return itemRepository.findById(id);
  }

  @InterfaceLog
  public List<Item> getAllItems() {
    return itemRepository.findAllByOrderByCreatedAtDesc();
  }

  @InterfaceLog
  public Item updateItem(@NotNull Long id,@Valid Item item) {
    Validate.isTrue(itemRepository.findByName(item.getName()) != null, "existing.item.name");
    return itemRepository
        .findById(id)
        .map(
            existing -> {
              existing.setName(item.getName());
              existing.setDescription(item.getDescription());
              return itemRepository.save(existing);
            })
        .orElseThrow(() -> new IllegalArgumentException("Item not found: " + id));
  }

  @InterfaceLog
  public void deleteItem(@NotNull Long id) {
    itemRepository.deleteById(id);
  }
}

package com.example.backend.controller;

import com.example.backend.dto.ItemDto;
import com.example.backend.mapper.ItemMapper;
import com.example.backend.service.ItemService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.log.InterfaceLog;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
@Slf4j
@InterfaceLog
public class ItemController {
  private final ItemService itemService;
  private final ItemMapper itemMapper;

  @GetMapping
  @InterfaceLog
  public List<ItemDto> getAllItems() {
    return itemService.getAllItems().stream().map(itemMapper::toDto).collect(Collectors.toList());
  }

  @GetMapping("/{id}")
  @InterfaceLog
  public ItemDto getItemById(@PathVariable("id") Long id) {
    return itemService
        .getItemById(id)
        .map(itemMapper::toDto)
        .orElseThrow(() -> new IllegalArgumentException("Item not found: " + id));
  }

  @PostMapping
  @InterfaceLog
  public ItemDto createItem(@RequestBody @Valid ItemDto itemDto) {
    return itemMapper.toDto(itemService.createItem(itemMapper.toEntity(itemDto)));
  }

  @PutMapping("/{id}")
  @InterfaceLog
  public ItemDto updateItem(@PathVariable Long id, @RequestBody ItemDto itemDto) {
    return itemMapper.toDto(itemService.updateItem(id, itemMapper.toEntity(itemDto)));
  }

  @DeleteMapping("/{id}")
  @InterfaceLog
  public void deleteItem(@PathVariable Long id) {
    itemService.deleteItem(id);
  }
}

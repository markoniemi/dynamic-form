package com.example.backend.controller;

import static org.mockito.Mockito.when;

import com.example.backend.dto.ItemDto;
import com.example.backend.entity.Item;
import com.example.backend.mapper.ItemMapper;
import com.example.backend.service.ItemService;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebMvcTest(ItemController.class)
class ItemControllerTest {
  @Autowired private WebTestClient webTestClient;
  @MockitoBean private ItemService itemService;
  @MockitoBean private ItemMapper itemMapper;

  @Test
  @WithMockUser
  void getAllItems() {
    Item item = new Item(1L, "Test Item", "Description", null, null);
    ItemDto itemDto = new ItemDto(1L, "Test Item", "Description");

    when(itemService.getAllItems()).thenReturn(Collections.singletonList(item));
    when(itemMapper.toDto(item)).thenReturn(itemDto);

    webTestClient
        .get()
        .uri("/api/items")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$[0].id")
        .isEqualTo(itemDto.getId())
        .jsonPath("$[0].name")
        .isEqualTo(itemDto.getName())
        .jsonPath("$[0].description")
        .isEqualTo(itemDto.getDescription());
  }
}

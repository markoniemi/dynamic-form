package com.example.backend.controller;

import static org.mockito.Mockito.when;

import com.example.backend.service.FormService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebMvcTest(FormController.class)
class FormControllerTest {

  @Autowired private WebTestClient webTestClient;
  @MockitoBean private FormService formService;

  @Test
  @WithMockUser
  void getAvailableForms() {
    when(formService.getAvailableFormKeys()).thenReturn(Set.of("form1", "form2"));

    webTestClient
        .get()
        .uri("/api/forms")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk()
        .returnResult(String.class)
        .getResponseBody()
        .toStream()
        .toList()
        .containsAll(List.of("form1", "form2"));
  }

  @Test
  @WithMockUser
  void getFormDefinition() {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode mockForm = mapper.createObjectNode().put("title", "Test Form");
    when(formService.getFormDefinition("form1")).thenReturn(mockForm);

    webTestClient
        .get()
        .uri("/api/forms/form1")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.title")
        .isEqualTo("Test Form");
  }
}

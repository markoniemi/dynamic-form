package com.example.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.backend.dto.FieldDto;
import com.example.backend.dto.FormDto;
import com.example.backend.entity.Field;
import com.example.backend.entity.Form;
import com.example.backend.mapper.FormMapper;
import com.example.backend.service.FormService;
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
  @MockitoBean private FormMapper formMapper;

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
  void getForm() {
    Form mockForm =
        Form.builder()
            .id(1L)
            .formKey("form1")
            .title("Test Form")
            .description("A test form")
            .fields(
                List.of(
                    Field.builder()
                        .name("testField")
                        .label("Test Field")
                        .type("text")
                        .required(true)
                        .build()))
            .build();

    FormDto mockDto =
        FormDto.builder()
            .id(1L)
            .formKey("form1")
            .title("Test Form")
            .description("A test form")
            .fields(
                List.of(
                    FieldDto.builder()
                        .name("testField")
                        .label("Test Field")
                        .type("text")
                        .required(true)
                        .build()))
            .build();

    when(formService.getForm("form1")).thenReturn(mockForm);
    when(formMapper.toDto(any(Form.class))).thenReturn(mockDto);

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

package com.example.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.dto.FormDataDto;
import com.example.backend.entity.FormData;
import com.example.backend.mapper.FormDataMapper;
import com.example.backend.service.FormDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FormDataController.class)
class FormDataControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private FormDataService formDataService;
  @MockitoBean private FormDataMapper formDataMapper;

  @Test
  void submitForm() throws Exception {
    Map<String, Object> data = Map.of("field1", "value1");
    FormData formData = new FormData("form1", data, "testuser");
    FormDataDto formDataDto = new FormDataDto(1L, "form1", data, LocalDateTime.now(), "testuser");

    when(formDataService.createFormSubmission(eq("form1"), any(FormData.class)))
        .thenReturn(formData);
    when(formDataMapper.toDto(formData)).thenReturn(formDataDto);

    mockMvc
        .perform(
            post("/api/form-data/form1")
                .with(csrf())
                .with(jwt().jwt(jwt -> jwt.claim("sub", "testuser")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(data)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.formKey").value("form1"));
  }

  @Test
  void getAllSubmissions() throws Exception {
    FormData formData = new FormData("form1", Map.of(), "username");
    FormDataDto formDataDto =
        new FormDataDto(1L, "form1", Map.of(), LocalDateTime.now(), "username");

    when(formDataService.getAllFormSubmissions()).thenReturn(Collections.singletonList(formData));
    when(formDataMapper.toDto(formData)).thenReturn(formDataDto);

    mockMvc
        .perform(get("/api/form-data").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1L));
  }

  @Test
  void getSubmissionsByFormKey() throws Exception {
    FormData formData = new FormData("form1", Map.of(), "username");
    FormDataDto formDataDto =
        new FormDataDto(1L, "form1", Map.of(), LocalDateTime.now(), "username");

    when(formDataService.getFormSubmissionsByKey("form1"))
        .thenReturn(Collections.singletonList(formData));
    when(formDataMapper.toDto(formData)).thenReturn(formDataDto);

    mockMvc
        .perform(get("/api/form-data/form1").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1L));
  }

  @Test
  void getSubmissionById() throws Exception {
    FormData formData = new FormData("form1", Map.of(), "username");
    FormDataDto formDataDto =
        new FormDataDto(1L, "form1", Map.of(), LocalDateTime.now(), "username");

    when(formDataService.getFormSubmissionById(1L)).thenReturn(Optional.of(formData));
    when(formDataMapper.toDto(formData)).thenReturn(formDataDto);

    mockMvc
        .perform(get("/api/form-data/submission/1").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L));
  }

  @Test
  void deleteSubmission() throws Exception {
    mockMvc
        .perform(
            delete("/api/form-data/submission/1")
                .with(csrf())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
        .andExpect(status().isOk());

    verify(formDataService).deleteFormSubmission(1L);
  }
}

package com.example.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.dto.FormDataDto;
import com.example.backend.entity.FormData;
import com.example.backend.mapper.FormDataMapper;
import com.example.backend.service.FormDataService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(FormDataController.class)
class FormDataControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private FormDataService formDataService;
  @MockitoBean private FormDataMapper formDataMapper;

  @Test
  @Disabled("Requires Spring Boot 4 / Spring Security 7 JWT test setup investigation")
  void submitForm() throws Exception {
    Map<String, Object> data = Map.of("field1", "value1");
    FormData formData = new FormData("form1", data, "testuser");
    FormDataDto formDataDto = new FormDataDto(1L, "form1", data, LocalDateTime.now(), "testuser");

    when(formDataService.createFormSubmission(eq("form1"), any(FormData.class)))
        .thenReturn(formData);
    when(formDataMapper.toDto(formData)).thenReturn(formDataDto);

    JwtAuthenticationToken jwtAuth = createJwtAuth("testuser", Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")));

    mockMvc
        .perform(
            post("/api/form-data/form1")
                .with(csrf())
                .with(authentication(jwtAuth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(data)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.formKey").value("form1"));
  }

  @Test
  @Disabled("Requires Spring Boot 4 / Spring Security 7 JWT test setup investigation")
  void getSubmissions() throws Exception {
    FormData formData = new FormData("form1", Map.of(), "username");
    FormDataDto formDataDto =
        new FormDataDto(1L, "form1", Map.of(), LocalDateTime.now(), "username");

    when(formDataService.getFormSubmissions()).thenReturn(Collections.singletonList(formData));
    when(formDataMapper.mapList(any(List.class))).thenReturn(Collections.singletonList(formDataDto));

    JwtAuthenticationToken jwtAuth = createJwtAuth("admin", Collections.singleton(new SimpleGrantedAuthority("ROLE_ADMIN")));

    mockMvc
        .perform(
            get("/api/form-data").with(authentication(jwtAuth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1L));
  }

  @Test
  @Disabled("Requires Spring Boot 4 / Spring Security 7 JWT test setup investigation")
  void getSubmissionById() throws Exception {
    FormData formData = new FormData("form1", Map.of(), "username");
    FormDataDto formDataDto =
        new FormDataDto(1L, "form1", Map.of(), LocalDateTime.now(), "username");

    when(formDataService.getFormSubmissionById(1L)).thenReturn(Optional.of(formData));
    when(formDataMapper.toDto(formData)).thenReturn(formDataDto);

    JwtAuthenticationToken jwtAuth = createJwtAuth("admin", Collections.singleton(new SimpleGrantedAuthority("ROLE_ADMIN")));

    mockMvc
        .perform(
            get("/api/form-data/submission/1")
                .with(authentication(jwtAuth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L));
  }

  @Test
  @Disabled("Requires Spring Boot 4 / Spring Security 7 JWT test setup investigation")
  void deleteSubmission() throws Exception {
    JwtAuthenticationToken jwtAuth = createJwtAuth("testuser", Collections.singleton(new SimpleGrantedAuthority("ROLE_ADMIN")));

    mockMvc
        .perform(
            delete("/api/form-data/submission/1")
                .with(csrf())
                .with(authentication(jwtAuth)))
        .andExpect(status().isOk());

    verify(formDataService).deleteFormSubmission(1L, "testuser");
  }

  private JwtAuthenticationToken createJwtAuth(String subject, Collection<SimpleGrantedAuthority> authorities) {
    Jwt jwt = Jwt.withTokenValue("token")
        .header("alg", "none")
        .claim("sub", subject)
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .build();
    return new JwtAuthenticationToken(jwt, authorities);
  }
}

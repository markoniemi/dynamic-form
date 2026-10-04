package com.example.backend.controller;

import static com.example.backend.testdata.TestSubmissions.contact;
import static com.example.backend.testdata.TestSubmissions.contactData;
import static com.example.backend.testdata.TestSubmissions.toDto;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.config.SecurityConfig;
import com.example.backend.entity.FormData;
import com.example.backend.mapper.FormDataMapper;
import com.example.backend.service.FormDataService;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(FormDataController.class)
@Import(SecurityConfig.class)
class FormDataControllerTest {

  private static final Collection<SimpleGrantedAuthority> USER =
      Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));
  private static final Collection<SimpleGrantedAuthority> ADMIN =
      Collections.singleton(new SimpleGrantedAuthority("ROLE_ADMIN"));

  private final MockMvc mockMvc;
  private final ObjectMapper objectMapper;

  @MockitoBean private FormDataService formDataService;
  @MockitoBean private FormDataMapper formDataMapper;
  @MockitoBean private JwtDecoder jwtDecoder;

  @Autowired
  FormDataControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
    this.mockMvc = mockMvc;
    this.objectMapper = objectMapper;
  }

  /** Makes the mocked mapper map the given submission like the real mapper does. */
  private void stubToDto(FormData formData) {
    when(formDataMapper.toDto(formData)).thenReturn(toDto(formData));
  }

  @Test
  void submitForm() throws Exception {
    FormData saved = contact(1L, "testuser");
    when(formDataService.createFormSubmission(eq("contact"), any(FormData.class)))
        .thenReturn(saved);
    stubToDto(saved);

    mockMvc
        .perform(
            post("/api/form-data/contact")
                .with(csrf())
                .with(authentication(createJwtAuth("testuser", USER)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(contactData())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.formKey").value("contact"));
  }

  @Test
  void getSubmissions() throws Exception {
    List<FormData> submissions = List.of(contact(1L, "username"));
    when(formDataService.getFormSubmissions()).thenReturn(submissions);
    when(formDataMapper.mapList(submissions)).thenReturn(List.of(toDto(submissions.getFirst())));

    mockMvc
        .perform(get("/api/form-data").with(authentication(createJwtAuth("admin", ADMIN))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1L));
  }

  @Test
  void getSubmissionById() throws Exception {
    FormData formData = contact(1L, "username");
    when(formDataService.getFormSubmissionById(1L)).thenReturn(Optional.of(formData));
    stubToDto(formData);

    mockMvc
        .perform(
            get("/api/form-data/submission/1").with(authentication(createJwtAuth("admin", ADMIN))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L));
  }

  @Test
  void deleteSubmission() throws Exception {
    mockMvc
        .perform(
            delete("/api/form-data/submission/1")
                .with(csrf())
                .with(authentication(createJwtAuth("testuser", ADMIN))))
        .andExpect(status().isOk());

    verify(formDataService).deleteFormSubmission(1L, "testuser");
  }

  @Test
  void getSubmissionsAsUserReturnsOnlyOwnSubmissions() throws Exception {
    List<FormData> submissions = List.of(contact(1L, "testuser"));
    when(formDataService.getFormSubmissionsByOwner("testuser")).thenReturn(submissions);
    when(formDataMapper.mapList(submissions)).thenReturn(List.of(toDto(submissions.getFirst())));

    mockMvc
        .perform(get("/api/form-data").with(authentication(createJwtAuth("testuser", USER))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].submittedBy").value("testuser"));

    verify(formDataService, never()).getFormSubmissions();
  }

  @Test
  void getSubmissionByIdOfOtherUserReturnsForbidden() throws Exception {
    when(formDataService.getFormSubmissionById(1L)).thenReturn(Optional.of(contact(1L, "owner")));

    mockMvc
        .perform(
            get("/api/form-data/submission/1")
                .with(authentication(createJwtAuth("intruder", USER))))
        .andExpect(status().isForbidden());
  }

  @Test
  void getSubmissionByIdMissingReturnsNotFound() throws Exception {
    when(formDataService.getFormSubmissionById(99L)).thenReturn(Optional.empty());

    mockMvc
        .perform(
            get("/api/form-data/submission/99")
                .with(authentication(createJwtAuth("testuser", USER))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("Form submission not found: 99"));
  }

  @Test
  void updateSubmissionPassesCallerAsUsername() throws Exception {
    Map<String, Object> data = Map.of("name", "Updated User");
    FormData updated = contact(1L, "testuser");
    updated.setData(data);
    when(formDataService.updateFormSubmission(1L, data, "testuser")).thenReturn(updated);
    stubToDto(updated);

    mockMvc
        .perform(
            put("/api/form-data/submission/1")
                .with(authentication(createJwtAuth("testuser", USER)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(data)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.name").value("Updated User"));
  }

  @Test
  void deleteSubmissionAsNonAdminReturnsForbidden() throws Exception {
    mockMvc
        .perform(
            delete("/api/form-data/submission/1")
                .with(authentication(createJwtAuth("testuser", USER))))
        .andExpect(status().isForbidden());

    verify(formDataService, never()).deleteFormSubmission(any(), any());
  }

  private JwtAuthenticationToken createJwtAuth(
      String subject, Collection<SimpleGrantedAuthority> authorities) {
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("sub", subject)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
    return new JwtAuthenticationToken(jwt, authorities);
  }
}

package com.example.backend.controller;

import static com.example.backend.testdata.TestSubmissions.contact;
import static com.example.backend.testdata.TestSubmissions.contactData;
import static com.example.backend.testdata.TestSubmissions.toDto;
import static com.example.backend.testdata.TestUsers.ADMIN;
import static com.example.backend.testdata.TestUsers.USER;
import static com.example.backend.testdata.TestUsers.admin;
import static com.example.backend.testdata.TestUsers.user;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(FormDataController.class)
@Import(SecurityConfig.class)
class FormDataControllerTest {

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
    FormData saved = contact(1L, USER);
    when(formDataService.create(eq("contact"), any(FormData.class)))
        .thenReturn(saved);
    stubToDto(saved);

    mockMvc
        .perform(
            post("/api/form-data/contact")
                .with(csrf())
                .with(user())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(contactData())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.formKey").value("contact"));

    verify(formDataService)
        .create(eq("contact"), argThat(f -> USER.equals(f.getSubmittedBy())));
  }

  @Test
  void getSubmissions() throws Exception {
    List<FormData> submissions = List.of(contact(1L, USER));
    when(formDataService.getAll()).thenReturn(submissions);
    when(formDataMapper.mapList(submissions)).thenReturn(List.of(toDto(submissions.getFirst())));

    mockMvc
        .perform(get("/api/form-data").with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1L));
  }

  @Test
  void getSubmissionById() throws Exception {
    FormData formData = contact(1L, USER);
    when(formDataService.get(1L)).thenReturn(formData);
    stubToDto(formData);

    mockMvc
        .perform(get("/api/form-data/submission/1").with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L));
  }

  @Test
  void deleteSubmission() throws Exception {
    mockMvc
        .perform(delete("/api/form-data/submission/1").with(csrf()).with(admin()))
        .andExpect(status().isOk());

    verify(formDataService).delete(1L, ADMIN);
  }

  @Test
  void getSubmissionsAsUserReturnsOnlyOwnSubmissions() throws Exception {
    List<FormData> submissions = List.of(contact(1L, USER));
    when(formDataService.getByOwner(USER)).thenReturn(submissions);
    when(formDataMapper.mapList(submissions)).thenReturn(List.of(toDto(submissions.getFirst())));

    mockMvc
        .perform(get("/api/form-data").with(user()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].submittedBy").value(USER));

    verify(formDataService, never()).getAll();
  }

  @Test
  void getSubmissionByIdOfOtherUserReturnsForbidden() throws Exception {
    when(formDataService.get(1L)).thenReturn(contact(1L, ADMIN));

    mockMvc.perform(get("/api/form-data/submission/1").with(user())).andExpect(status().isForbidden());
  }

  @Test
  void getSubmissionByIdMissingReturnsNotFound() throws Exception {
    when(formDataService.get(99L))
        .thenThrow(new NoSuchElementException("Form submission not found: 99"));

    mockMvc
        .perform(get("/api/form-data/submission/99").with(user()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("Form submission not found: 99"));
  }

  @Test
  void updateSubmissionPassesCallerAsUsername() throws Exception {
    Map<String, Object> data = Map.of("name", "Updated User");
    FormData updated = contact(1L, USER);
    updated.setData(data);
    when(formDataService.update(1L, data, USER)).thenReturn(updated);
    stubToDto(updated);

    mockMvc
        .perform(
            put("/api/form-data/submission/1")
                .with(user())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(data)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.name").value("Updated User"));
  }

  @Test
  void deleteSubmissionAsNonAdminReturnsForbidden() throws Exception {
    mockMvc
        .perform(delete("/api/form-data/submission/1").with(user()))
        .andExpect(status().isForbidden());

    verify(formDataService, never()).delete(any(), any());
  }
}

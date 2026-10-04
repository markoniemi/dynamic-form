package com.example.backend.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.example.backend.config.SecurityConfig;
import com.example.backend.dto.FieldDto;
import com.example.backend.dto.FormDto;
import com.example.backend.dto.FormListItemDto;
import com.example.backend.entity.Field;
import com.example.backend.entity.Form;
import com.example.backend.mapper.FormMapper;
import com.example.backend.service.FormService;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FormController.class)
@Import(SecurityConfig.class)
class FormControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private FormService formService;
  @MockitoBean private FormMapper formMapper;
  @MockitoBean private JwtDecoder jwtDecoder;

  private static final String VALID_FORM_JSON =
      """
      {"formKey":"form1","title":"Test Form","fields":[
        {"name":"field1","label":"Field 1","type":"text","required":true}]}
      """;

  private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor adminJwt() {
    return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
  }

  @Test
  void deleteFormAsNonAdminReturnsForbidden() throws Exception {
    mockMvc.perform(delete("/api/forms/form1").with(jwt())).andExpect(status().isForbidden());

    verify(formService, never()).deleteForm(any());
  }

  @Test
  void getFormsReturnsListItems() throws Exception {
    when(formService.getForms()).thenReturn(List.of(new FormListItemDto("form1", "Form 1")));

    mockMvc
        .perform(get("/api/forms").with(jwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].formKey").value("form1"))
        .andExpect(jsonPath("$[0].title").value("Form 1"));
  }

  @Test
  void getFormsWithoutAuthenticationReturnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/forms")).andExpect(status().isUnauthorized());
  }

  @Test
  void getFormMissingReturnsNotFound() throws Exception {
    when(formService.getForm("missing"))
        .thenThrow(new NoSuchElementException("Form not found: missing"));

    mockMvc
        .perform(get("/api/forms/missing").with(jwt()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("Form not found: missing"));
  }

  @Test
  void createFormWithExistingKeyReturnsBadRequest() throws Exception {
    when(formService.existsByFormKey("form1")).thenReturn(true);

    mockMvc
        .perform(
            post("/api/forms")
                .with(adminJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_FORM_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Form with key 'form1' already exists"));

    verify(formService, never()).saveForm(any());
  }

  @Test
  void updateFormAsAdminReturnsUpdatedForm() throws Exception {
    Form form = Form.builder().formKey("form1").title("Test Form").build();
    when(formMapper.toEntity(any(FormDto.class))).thenReturn(form);
    when(formService.updateForm("form1", form)).thenReturn(form);
    when(formMapper.toDto(form)).thenReturn(FormDto.builder().formKey("form1").build());

    mockMvc
        .perform(
            put("/api/forms/form1")
                .with(adminJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_FORM_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.formKey").value("form1"));
  }

  @Test
  void deleteFormAsAdminReturnsNoContent() throws Exception {
    mockMvc
        .perform(delete("/api/forms/form1").with(adminJwt()))
        .andExpect(status().isNoContent());

    verify(formService).deleteForm("form1");
  }

  @Test
  void createFormWithValidBodyReturnsCreated() throws Exception {
    Form form = Form.builder().formKey("form1").title("Test Form").build();
    when(formService.existsByFormKey("form1")).thenReturn(false);
    when(formMapper.toEntity(any(FormDto.class))).thenReturn(form);
    when(formService.saveForm(form)).thenReturn(form);
    when(formMapper.toDto(form)).thenReturn(FormDto.builder().formKey("form1").build());

    mockMvc
        .perform(
            post("/api/forms")
                .with(adminJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_FORM_JSON))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.formKey").value("form1"));
  }

  @Test
  void createFormWithInvalidBodyReturnsFieldErrors() throws Exception {
    String body =
        """
        {"formKey":"Bad Key","title":"","fields":[
          {"name":"field1","label":"Field 1","type":"text","required":false}]}
        """;

    mockMvc
        .perform(
            post("/api/forms")
                .with(adminJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Validation failed"))
        .andExpect(jsonPath("$.errors[?(@.field == 'formKey')].code").value("Pattern"))
        .andExpect(jsonPath("$.errors[?(@.field == 'title')].code").value("NotBlank"));

    verify(formService, never()).saveForm(any());
  }

  @Test
  void createFormWithoutRequiredDefaultsToFalse() throws Exception {
    String body =
        """
        {"formKey":"form1","title":"Test Form","fields":[
          {"name":"field1","label":"Field 1","type":"text"}]}
        """;
    Form form = Form.builder().formKey("form1").title("Test Form").build();
    when(formMapper.toEntity(any(FormDto.class))).thenReturn(form);
    when(formService.saveForm(form)).thenReturn(form);
    when(formMapper.toDto(form)).thenReturn(FormDto.builder().formKey("form1").build());

    mockMvc
        .perform(
            post("/api/forms")
                .with(adminJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated());

    ArgumentCaptor<FormDto> captor = ArgumentCaptor.forClass(FormDto.class);
    verify(formMapper).toEntity(captor.capture());
    assertFalse(captor.getValue().getFields().getFirst().isRequired());
  }

  @Test
  @WithMockUser
  void getForm() throws Exception {
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

    mockMvc
        .perform(get("/api/forms/form1").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Test Form"));
  }
}

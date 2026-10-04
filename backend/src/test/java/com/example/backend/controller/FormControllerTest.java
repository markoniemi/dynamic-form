package com.example.backend.controller;

import static com.example.backend.testdata.TestForms.contact;
import static com.example.backend.testdata.TestForms.contactDto;
import static com.example.backend.testdata.TestForms.listItem;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.config.SecurityConfig;
import com.example.backend.dto.FormDto;
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
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(FormController.class)
@Import(SecurityConfig.class)
class FormControllerTest {

  private final MockMvc mockMvc;
  private final ObjectMapper objectMapper;
  @MockitoBean private FormService formService;
  @MockitoBean private FormMapper formMapper;
  @MockitoBean private JwtDecoder jwtDecoder;

  @Autowired
  FormControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
    this.mockMvc = mockMvc;
    this.objectMapper = objectMapper;
  }

  private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor adminJwt() {
    return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
  }

  private String contactJson() {
    return objectMapper.writeValueAsString(contactDto());
  }

  /** Makes the mocked mapper round-trip any request body to the contact form. */
  private Form stubMapperWithContact() {
    Form form = contact();
    when(formMapper.toEntity(any(FormDto.class))).thenReturn(form);
    when(formMapper.toDto(form)).thenReturn(contactDto());
    return form;
  }

  @Test
  void deleteFormAsNonAdminReturnsForbidden() throws Exception {
    mockMvc.perform(delete("/api/forms/contact").with(jwt())).andExpect(status().isForbidden());

    verify(formService, never()).deleteForm(any());
  }

  @Test
  void getFormsReturnsListItems() throws Exception {
    Form contact = contact();
    when(formService.getForms()).thenReturn(List.of(listItem(contact)));

    mockMvc
        .perform(get("/api/forms").with(jwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].formKey").value(contact.getFormKey()))
        .andExpect(jsonPath("$[0].title").value(contact.getTitle()));
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
    when(formService.existsByFormKey("contact")).thenReturn(true);

    mockMvc
        .perform(
            post("/api/forms")
                .with(adminJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(contactJson()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Form with key 'contact' already exists"));

    verify(formService, never()).saveForm(any());
  }

  @Test
  void updateFormAsAdminReturnsUpdatedForm() throws Exception {
    Form form = stubMapperWithContact();
    when(formService.updateForm("contact", form)).thenReturn(form);

    mockMvc
        .perform(
            put("/api/forms/contact")
                .with(adminJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(contactJson()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.formKey").value("contact"));
  }

  @Test
  void deleteFormAsAdminReturnsNoContent() throws Exception {
    mockMvc
        .perform(delete("/api/forms/contact").with(adminJwt()))
        .andExpect(status().isNoContent());

    verify(formService).deleteForm("contact");
  }

  @Test
  void createFormWithValidBodyReturnsCreated() throws Exception {
    Form form = stubMapperWithContact();
    when(formService.existsByFormKey("contact")).thenReturn(false);
    when(formService.saveForm(form)).thenReturn(form);

    mockMvc
        .perform(
            post("/api/forms")
                .with(adminJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(contactJson()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.formKey").value("contact"))
        .andExpect(jsonPath("$.fields.length()").value(contact().getFields().size()));
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
        {"formKey":"simple","title":"Simple Form","fields":[
          {"name":"name","label":"Name","type":"text"}]}
        """;
    Form form = stubMapperWithContact();
    when(formService.saveForm(form)).thenReturn(form);

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
    when(formService.getForm("contact")).thenReturn(contact());
    when(formMapper.toDto(any(Form.class))).thenReturn(contactDto());

    mockMvc
        .perform(get("/api/forms/contact").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value(contact().getTitle()));
  }
}

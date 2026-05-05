package com.example.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.example.backend.dto.FieldDto;
import com.example.backend.dto.FormDto;
import com.example.backend.entity.Field;
import com.example.backend.entity.Form;
import com.example.backend.mapper.FormMapper;
import com.example.backend.service.FormService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FormController.class)
class FormControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private FormService formService;
  @MockitoBean private FormMapper formMapper;

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

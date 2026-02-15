package com.example.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.backend.entity.Form;
import com.example.backend.entity.Field;
import com.example.backend.repository.FormRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FormServiceTest {

  @Mock private FormRepository formRepository;
  @InjectMocks private FormService formService;

  @Test
  void getAvailableFormKeys() {
    Form form1 = Form.builder().formKey("form1").build();
    Form form2 = Form.builder().formKey("form2").build();
    when(formRepository.findAll()).thenReturn(List.of(form1, form2));

    Set<String> keys = formService.getAvailableFormKeys();

    assertEquals(2, keys.size());
    assertTrue(keys.contains("form1"));
    assertTrue(keys.contains("form2"));
  }

  @Test
  void getForm() {
    Form mockForm = Form.builder()
        .id(1L)
        .formKey("form1")
        .title("Test Form")
        .fields(List.of(Field.builder().name("field1").type("text").build()))
        .build();
    when(formRepository.findByFormKey("form1")).thenReturn(Optional.of(mockForm));

    Form result = formService.getForm("form1");

    assertEquals(mockForm, result);
    assertEquals("Test Form", result.getTitle());
  }

  @Test
  void getFormWithNotFoundThrowsException() {
    when(formRepository.findByFormKey("unknown")).thenReturn(Optional.empty());

    assertThrows(IllegalArgumentException.class, () -> formService.getForm("unknown"));
  }

  @Test
  void saveForm() {
    Form newForm = Form.builder()
        .formKey("new-form")
        .title("New Form")
        .fields(List.of())
        .build();
    when(formRepository.save(newForm)).thenReturn(newForm);

    Form result = formService.saveForm(newForm);

    assertEquals("new-form", result.getFormKey());
    verify(formRepository).save(newForm);
  }

  @Test
  void existsByFormKey() {
    when(formRepository.existsByFormKey("form1")).thenReturn(true);
    when(formRepository.existsByFormKey("unknown")).thenReturn(false);

    assertTrue(formService.existsByFormKey("form1"));
    assertFalse(formService.existsByFormKey("unknown"));
  }
}

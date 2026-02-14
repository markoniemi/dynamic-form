package com.example.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.backend.entity.FormDefinition;
import com.example.backend.entity.FormFieldDefinition;
import com.example.backend.repository.FormDefinitionRepository;
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

  @Mock private FormDefinitionRepository formDefinitionRepository;
  @InjectMocks private FormService formService;

  @Test
  void getAvailableFormKeys() {
    FormDefinition form1 = FormDefinition.builder().formKey("form1").build();
    FormDefinition form2 = FormDefinition.builder().formKey("form2").build();
    when(formDefinitionRepository.findAll()).thenReturn(List.of(form1, form2));

    Set<String> keys = formService.getAvailableFormKeys();

    assertEquals(2, keys.size());
    assertTrue(keys.contains("form1"));
    assertTrue(keys.contains("form2"));
  }

  @Test
  void getFormDefinition() {
    FormDefinition mockForm = FormDefinition.builder()
        .id(1L)
        .formKey("form1")
        .title("Test Form")
        .fields(List.of(FormFieldDefinition.builder().name("field1").type("text").build()))
        .build();
    when(formDefinitionRepository.findByFormKey("form1")).thenReturn(Optional.of(mockForm));

    FormDefinition result = formService.getFormDefinition("form1");

    assertEquals(mockForm, result);
    assertEquals("Test Form", result.getTitle());
  }

  @Test
  void getFormDefinitionWithNotFoundThrowsException() {
    when(formDefinitionRepository.findByFormKey("unknown")).thenReturn(Optional.empty());

    assertThrows(IllegalArgumentException.class, () -> formService.getFormDefinition("unknown"));
  }

  @Test
  void saveFormDefinition() {
    FormDefinition newForm = FormDefinition.builder()
        .formKey("new-form")
        .title("New Form")
        .fields(List.of())
        .build();
    when(formDefinitionRepository.save(newForm)).thenReturn(newForm);

    FormDefinition result = formService.saveFormDefinition(newForm);

    assertEquals("new-form", result.getFormKey());
    verify(formDefinitionRepository).save(newForm);
  }

  @Test
  void existsByFormKey() {
    when(formDefinitionRepository.existsByFormKey("form1")).thenReturn(true);
    when(formDefinitionRepository.existsByFormKey("unknown")).thenReturn(false);

    assertTrue(formService.existsByFormKey("form1"));
    assertFalse(formService.existsByFormKey("unknown"));
  }
}

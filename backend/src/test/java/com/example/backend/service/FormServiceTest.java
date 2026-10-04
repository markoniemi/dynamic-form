package com.example.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.backend.dto.FormListItemDto;
import com.example.backend.entity.Field;
import com.example.backend.entity.Form;
import com.example.backend.mapper.FormListItemMapper;
import com.example.backend.repository.FormRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FormServiceTest {

  @Mock private FormRepository formRepository;
  @Spy private FormListItemMapper formListItemMapper = Mappers.getMapper(FormListItemMapper.class);
  @InjectMocks private FormService formService;

  @Test
  void getFormsReturnsListItems() {
    when(formRepository.findAll())
        .thenReturn(
            List.of(
                Form.builder().formKey("form1").title("Form 1").build(),
                Form.builder().formKey("form2").title("Form 2").build()));

    List<FormListItemDto> result = formService.getForms();

    assertEquals(
        List.of(new FormListItemDto("form1", "Form 1"), new FormListItemDto("form2", "Form 2")),
        result);
  }

  @Test
  void getFormWithValidKeyReturnsForm() {
    Form mockForm =
        Form.builder()
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

    assertThrows(NoSuchElementException.class, () -> formService.getForm("unknown"));
  }

  @Test
  void updateFormWithNotFoundThrowsException() {
    when(formRepository.findByFormKey("unknown")).thenReturn(Optional.empty());

    assertThrows(
        NoSuchElementException.class,
        () -> formService.updateForm("unknown", Form.builder().title("New").build()));
    verify(formRepository, never()).save(any());
  }

  @Test
  void deleteFormWithNotFoundThrowsException() {
    when(formRepository.findByFormKey("unknown")).thenReturn(Optional.empty());

    assertThrows(NoSuchElementException.class, () -> formService.deleteForm("unknown"));
    verify(formRepository, never()).delete(any());
  }

  @Test
  void saveFormPersistsFormData() {
    Form newForm = Form.builder().formKey("new-form").title("New Form").fields(List.of()).build();
    when(formRepository.save(newForm)).thenReturn(newForm);

    Form result = formService.saveForm(newForm);

    assertEquals("new-form", result.getFormKey());
    verify(formRepository).save(newForm);
  }

  @Test
  void existsByFormKeyReturnsTrueForExistingKey() {
    when(formRepository.existsByFormKey("form1")).thenReturn(true);
    when(formRepository.existsByFormKey("unknown")).thenReturn(false);

    assertTrue(formService.existsByFormKey("form1"));
    assertFalse(formService.existsByFormKey("unknown"));
  }
}

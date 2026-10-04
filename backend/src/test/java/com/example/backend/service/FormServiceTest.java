package com.example.backend.service;

import static com.example.backend.testdata.TestForms.contact;
import static com.example.backend.testdata.TestForms.listItem;
import static com.example.backend.testdata.TestForms.simple;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.backend.entity.Form;
import com.example.backend.mapper.FormListItemMapper;
import com.example.backend.repository.FormRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
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
    Form simple = simple();
    Form contact = contact();
    when(formRepository.findAll()).thenReturn(List.of(simple, contact));

    assertEquals(List.of(listItem(simple), listItem(contact)), formService.getForms());
  }

  @Test
  void getFormWithValidKeyReturnsForm() {
    Form contact = contact();
    when(formRepository.findByFormKey("contact")).thenReturn(Optional.of(contact));

    assertEquals(contact, formService.getForm("contact"));
  }

  @Test
  void getFormWithNotFoundThrowsException() {
    when(formRepository.findByFormKey("unknown")).thenReturn(Optional.empty());

    assertThrows(NoSuchElementException.class, () -> formService.getForm("unknown"));
  }

  @Test
  void updateFormWithNotFoundThrowsException() {
    when(formRepository.findByFormKey("unknown")).thenReturn(Optional.empty());

    assertThrows(NoSuchElementException.class, () -> formService.updateForm("unknown", simple()));
    verify(formRepository, never()).save(any());
  }

  @Test
  void updateFormCopiesEditableFieldsAndKeepsKey() {
    Form existing = contact();
    Form update = simple();
    when(formRepository.findByFormKey("contact")).thenReturn(Optional.of(existing));
    when(formRepository.save(existing)).thenReturn(existing);

    Form result = formService.updateForm("contact", update);

    assertEquals(contact().getId(), result.getId());
    assertEquals("contact", result.getFormKey());
    assertEquals(update.getTitle(), result.getTitle());
    assertEquals(update.getDescription(), result.getDescription());
    assertEquals(update.getFields(), result.getFields());
    verify(formRepository).save(existing);
  }

  @Test
  void deleteFormDeletesExistingForm() {
    Form existing = contact();
    when(formRepository.findByFormKey("contact")).thenReturn(Optional.of(existing));

    formService.deleteForm("contact");

    verify(formRepository).delete(existing);
  }

  @Test
  void deleteFormWithNotFoundThrowsException() {
    when(formRepository.findByFormKey("unknown")).thenReturn(Optional.empty());

    assertThrows(NoSuchElementException.class, () -> formService.deleteForm("unknown"));
    verify(formRepository, never()).delete(any());
  }

  @Test
  void saveFormPersistsForm() {
    Form form = simple();
    when(formRepository.save(form)).thenReturn(form);

    assertEquals(form, formService.saveForm(form));
    verify(formRepository).save(form);
  }

  @Test
  void existsByFormKeyReturnsTrueForExistingKey() {
    when(formRepository.existsByFormKey("contact")).thenReturn(true);
    when(formRepository.existsByFormKey("unknown")).thenReturn(false);

    assertTrue(formService.existsByFormKey("contact"));
    assertFalse(formService.existsByFormKey("unknown"));
  }
}

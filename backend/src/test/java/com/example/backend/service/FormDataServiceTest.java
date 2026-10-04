package com.example.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.backend.entity.Form;
import com.example.backend.entity.FormData;
import com.example.backend.repository.FormDataRepository;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FormDataServiceTest {

  @Mock private FormDataRepository formDataRepository;
  @Mock private FormService formService;
  @InjectMocks private FormDataService formDataService;

  @Test
  void createFormSubmission() {
    String formKey = "form1";
    FormData formData = new FormData(formKey, Map.of("field", "value"), "username");
    Form mockDefinition = Form.builder().formKey(formKey).build();

    when(formService.getForm(formKey)).thenReturn(mockDefinition);
    when(formDataRepository.save(formData)).thenReturn(formData);

    FormData result = formDataService.createFormSubmission(formKey, formData);

    assertNotNull(result);
    assertEquals(formKey, result.getFormKey());
    verify(formDataRepository).save(formData);
  }

  @Test
  void getFormSubmissionById() {
    Long id = 1L;
    FormData formData = new FormData("form1", Map.of(), "username");
    when(formDataRepository.findById(id)).thenReturn(Optional.of(formData));

    Optional<FormData> result = formDataService.getFormSubmissionById(id);

    assertTrue(result.isPresent());
    assertEquals(formData, result.get());
  }

  @Test
  void getFormSubmissions() {
    FormData formData = new FormData("form1", Map.of(), "username");
    when(formDataRepository.findAll()).thenReturn(Collections.singletonList(formData));

    List<FormData> result = formDataService.getFormSubmissions();

    assertEquals(1, result.size());
    assertEquals(formData, result.getFirst());
  }

  @Test
  void deleteFormSubmission() {
    Long id = 1L;
    FormData formData = new FormData("form1", Map.of(), "username");
    when(formDataRepository.findById(id)).thenReturn(Optional.of(formData));
    formDataService.deleteFormSubmission(id, "username");
    verify(formDataRepository).deleteById(id);
  }

  @Test
  void createFormSubmissionWithUnknownFormThrowsAndDoesNotSave() {
    FormData formData = new FormData("unknown", Map.of(), "username");
    when(formService.getForm("unknown"))
        .thenThrow(new NoSuchElementException("Form not found: unknown"));

    assertThrows(
        NoSuchElementException.class,
        () -> formDataService.createFormSubmission("unknown", formData));
    verify(formDataRepository, never()).save(any());
  }

  @Test
  void updateFormSubmissionByOwnerReplacesData() {
    FormData existing = new FormData("form1", Map.of("field", "old"), "username");
    Map<String, Object> newData = Map.of("field", "new");
    when(formDataRepository.findById(1L)).thenReturn(Optional.of(existing));
    when(formDataRepository.save(existing)).thenReturn(existing);

    FormData result = formDataService.updateFormSubmission(1L, newData, "username");

    assertEquals(newData, result.getData());
    verify(formDataRepository).save(existing);
  }

  @Test
  void updateFormSubmissionByOtherUserThrowsAndDoesNotSave() {
    when(formDataRepository.findById(1L))
        .thenReturn(Optional.of(new FormData("form1", Map.of(), "owner")));

    assertThrows(
        SecurityException.class,
        () -> formDataService.updateFormSubmission(1L, Map.of(), "intruder"));
    verify(formDataRepository, never()).save(any());
  }

  @Test
  void deleteFormSubmissionByOtherUserThrowsAndDoesNotDelete() {
    when(formDataRepository.findById(1L))
        .thenReturn(Optional.of(new FormData("form1", Map.of(), "owner")));

    assertThrows(
        SecurityException.class, () -> formDataService.deleteFormSubmission(1L, "intruder"));
    verify(formDataRepository, never()).deleteById(any());
  }

  @Test
  void getFormSubmissionsByOwnerReturnsOwnersSubmissions() {
    FormData formData = new FormData("form1", Map.of(), "username");
    when(formDataRepository.findBySubmittedByOrderBySubmittedAtDesc("username"))
        .thenReturn(List.of(formData));

    assertEquals(List.of(formData), formDataService.getFormSubmissionsByOwner("username"));
  }

  @Test
  void updateFormSubmissionWithNotFoundThrowsException() {
    when(formDataRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(
        NoSuchElementException.class,
        () -> formDataService.updateFormSubmission(99L, Map.of(), "username"));
    verify(formDataRepository, never()).save(any());
  }

  @Test
  void deleteFormSubmissionWithNotFoundThrowsException() {
    when(formDataRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(
        NoSuchElementException.class, () -> formDataService.deleteFormSubmission(99L, "username"));
    verify(formDataRepository, never()).deleteById(any());
  }
}

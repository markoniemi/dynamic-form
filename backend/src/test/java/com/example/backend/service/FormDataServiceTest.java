package com.example.backend.service;

import static com.example.backend.testdata.TestSubmissions.contact;
import static com.example.backend.testdata.TestUsers.ADMIN;
import static com.example.backend.testdata.TestUsers.USER;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.backend.entity.FormData;
import com.example.backend.repository.FormDataRepository;
import com.example.backend.testdata.TestForms;
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
    FormData formData = contact(USER);
    when(formService.getForm("contact")).thenReturn(TestForms.contact());
    when(formDataRepository.save(formData)).thenReturn(formData);

    FormData result = formDataService.createFormSubmission("contact", formData);

    assertEquals("contact", result.getFormKey());
    verify(formDataRepository).save(formData);
  }

  @Test
  void getFormSubmissionById() {
    FormData formData = contact(1L, USER);
    when(formDataRepository.findById(1L)).thenReturn(Optional.of(formData));

    assertEquals(formData, formDataService.getFormSubmissionById(1L));
  }

  @Test
  void getFormSubmissionByIdWithNotFoundThrowsException() {
    when(formDataRepository.findById(99L)).thenReturn(Optional.empty());

    NoSuchElementException exception =
        assertThrows(
            NoSuchElementException.class, () -> formDataService.getFormSubmissionById(99L));
    assertEquals("Form submission not found: 99", exception.getMessage());
  }

  @Test
  void getFormSubmissions() {
    List<FormData> submissions = List.of(contact(1L, USER), contact(2L, ADMIN));
    when(formDataRepository.findAll()).thenReturn(submissions);

    assertEquals(submissions, formDataService.getFormSubmissions());
  }

  @Test
  void deleteFormSubmission() {
    when(formDataRepository.findById(1L)).thenReturn(Optional.of(contact(1L, USER)));

    formDataService.deleteFormSubmission(1L, USER);

    verify(formDataRepository).deleteById(1L);
  }

  @Test
  void createFormSubmissionWithUnknownFormThrowsAndDoesNotSave() {
    when(formService.getForm("unknown"))
        .thenThrow(new NoSuchElementException("Form not found: unknown"));

    assertThrows(
        NoSuchElementException.class,
        () -> formDataService.createFormSubmission("unknown", contact(USER)));
    verify(formDataRepository, never()).save(any());
  }

  @Test
  void updateFormSubmissionByOwnerReplacesData() {
    FormData existing = contact(1L, USER);
    Map<String, Object> newData = Map.of("name", "Updated User");
    when(formDataRepository.findById(1L)).thenReturn(Optional.of(existing));
    when(formDataRepository.save(existing)).thenReturn(existing);

    FormData result = formDataService.updateFormSubmission(1L, newData, USER);

    assertEquals(newData, result.getData());
    verify(formDataRepository).save(existing);
  }

  @Test
  void updateFormSubmissionByOtherUserThrowsAndDoesNotSave() {
    when(formDataRepository.findById(1L)).thenReturn(Optional.of(contact(1L, ADMIN)));

    assertThrows(
        SecurityException.class,
        () -> formDataService.updateFormSubmission(1L, Map.of(), USER));
    verify(formDataRepository, never()).save(any());
  }

  @Test
  void deleteFormSubmissionByOtherUserThrowsAndDoesNotDelete() {
    when(formDataRepository.findById(1L)).thenReturn(Optional.of(contact(1L, ADMIN)));

    assertThrows(
        SecurityException.class, () -> formDataService.deleteFormSubmission(1L, USER));
    verify(formDataRepository, never()).deleteById(any());
  }

  @Test
  void getFormSubmissionsByOwnerReturnsOwnersSubmissions() {
    List<FormData> submissions = List.of(contact(1L, USER));
    when(formDataRepository.findBySubmittedByOrderBySubmittedAtDesc(USER))
        .thenReturn(submissions);

    assertEquals(submissions, formDataService.getFormSubmissionsByOwner(USER));
  }

  @Test
  void updateFormSubmissionWithNotFoundThrowsException() {
    when(formDataRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(
        NoSuchElementException.class,
        () -> formDataService.updateFormSubmission(99L, Map.of(), USER));
    verify(formDataRepository, never()).save(any());
  }

  @Test
  void deleteFormSubmissionWithNotFoundThrowsException() {
    when(formDataRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(
        NoSuchElementException.class, () -> formDataService.deleteFormSubmission(99L, USER));
    verify(formDataRepository, never()).deleteById(any());
  }
}

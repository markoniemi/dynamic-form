package com.example.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.backend.entity.Form;
import com.example.backend.entity.FormData;
import com.example.backend.repository.FormDataRepository;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
  void getAllFormSubmissions() {
    FormData formData = new FormData("form1", Map.of(), "username");
    when(formDataRepository.findAll()).thenReturn(Collections.singletonList(formData));

    List<FormData> result = formDataService.getAllFormSubmissions();

    assertEquals(1, result.size());
    assertEquals(formData, result.getFirst());
  }

  @Test
  void getFormSubmissionsByKey() {
    String formKey = "form1";
    FormData formData = new FormData(formKey, Map.of(), "username");
    when(formDataRepository.findByFormKeyOrderBySubmittedAtDesc(formKey))
        .thenReturn(Collections.singletonList(formData));

    List<FormData> result = formDataService.getFormSubmissionsByKey(formKey);

    assertEquals(1, result.size());
    assertEquals(formData, result.getFirst());
  }

  @Test
  void deleteFormSubmission() {
    Long id = 1L;
    formDataService.deleteFormSubmission(id);
    verify(formDataRepository).deleteById(id);
  }
}

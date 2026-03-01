package com.example.backend.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.backend.entity.Form;
import com.example.backend.repository.FormRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DatabaseInitializerTest {

  @Mock private FormRepository formRepository;

  private DatabaseInitializer databaseInitializer;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    databaseInitializer = new DatabaseInitializer(formRepository, objectMapper);
  }

  @Test
  void runLoadsFormsFromResources() {
    when(formRepository.existsByFormKey(anyString())).thenReturn(false);
    when(formRepository.save(any(Form.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(formRepository.count()).thenReturn(3L);

    databaseInitializer.run();

    verify(formRepository, atLeastOnce()).save(any(Form.class));
  }

  @Test
  void runSkipsExistingForms() {
    when(formRepository.existsByFormKey("contact")).thenReturn(true);
    when(formRepository.existsByFormKey("feedback")).thenReturn(false);
    when(formRepository.existsByFormKey("survey")).thenReturn(false);
    when(formRepository.save(any(Form.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(formRepository.count()).thenReturn(2L);

    databaseInitializer.run();

    ArgumentCaptor<Form> captor = ArgumentCaptor.forClass(Form.class);
    verify(formRepository, atLeast(2)).save(captor.capture());

    // Verify "contact" was not saved since it already exists
    assertTrue(captor.getAllValues().stream().noneMatch(fd -> "contact".equals(fd.getFormKey())));
  }

  @Test
  void runParsesFormCorrectly() {
    when(formRepository.existsByFormKey(anyString())).thenReturn(false);
    when(formRepository.save(any(Form.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(formRepository.count()).thenReturn(3L);

    databaseInitializer.run();

    ArgumentCaptor<Form> captor = ArgumentCaptor.forClass(Form.class);
    verify(formRepository, atLeastOnce()).save(captor.capture());

    Form contactForm =
        captor.getAllValues().stream()
            .filter(fd -> "contact".equals(fd.getFormKey()))
            .findFirst()
            .orElse(null);

    assertNotNull(contactForm);
    assertEquals("Contact Us", contactForm.getTitle());
    assertNotNull(contactForm.getFields());
    assertFalse(contactForm.getFields().isEmpty());
  }
}

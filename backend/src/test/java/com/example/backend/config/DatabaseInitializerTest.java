package com.example.backend.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.backend.entity.FormDefinition;
import com.example.backend.repository.FormDefinitionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DatabaseInitializerTest {

  @Mock
  private FormDefinitionRepository formDefinitionRepository;

  private DatabaseInitializer databaseInitializer;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    databaseInitializer = new DatabaseInitializer(formDefinitionRepository, objectMapper);
  }

  @Test
  void runLoadsFormDefinitionsFromResources() {
    when(formDefinitionRepository.existsByFormKey(anyString())).thenReturn(false);
    when(formDefinitionRepository.save(any(FormDefinition.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(formDefinitionRepository.count()).thenReturn(3L);

    databaseInitializer.run();

    verify(formDefinitionRepository, atLeastOnce()).save(any(FormDefinition.class));
  }

  @Test
  void runSkipsExistingFormDefinitions() {
    when(formDefinitionRepository.existsByFormKey("contact")).thenReturn(true);
    when(formDefinitionRepository.existsByFormKey("feedback")).thenReturn(false);
    when(formDefinitionRepository.existsByFormKey("survey")).thenReturn(false);
    when(formDefinitionRepository.save(any(FormDefinition.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(formDefinitionRepository.count()).thenReturn(2L);

    databaseInitializer.run();

    ArgumentCaptor<FormDefinition> captor = ArgumentCaptor.forClass(FormDefinition.class);
    verify(formDefinitionRepository, atLeast(2)).save(captor.capture());

    // Verify "contact" was not saved since it already exists
    assertTrue(captor.getAllValues().stream()
        .noneMatch(fd -> "contact".equals(fd.getFormKey())));
  }

  @Test
  void runParsesFormDefinitionCorrectly() {
    when(formDefinitionRepository.existsByFormKey(anyString())).thenReturn(false);
    when(formDefinitionRepository.save(any(FormDefinition.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(formDefinitionRepository.count()).thenReturn(3L);

    databaseInitializer.run();

    ArgumentCaptor<FormDefinition> captor = ArgumentCaptor.forClass(FormDefinition.class);
    verify(formDefinitionRepository, atLeastOnce()).save(captor.capture());

    FormDefinition contactForm = captor.getAllValues().stream()
        .filter(fd -> "contact".equals(fd.getFormKey()))
        .findFirst()
        .orElse(null);

    assertNotNull(contactForm);
    assertEquals("Contact Us", contactForm.getTitle());
    assertNotNull(contactForm.getFields());
    assertFalse(contactForm.getFields().isEmpty());
  }
}


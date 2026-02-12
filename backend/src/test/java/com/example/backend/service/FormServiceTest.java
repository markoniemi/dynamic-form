package com.example.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FormServiceTest {
  @InjectMocks private FormService formService;

  @Test
  void getAvailableFormKeys() {
    // Manually inject data into the map via reflection
    JsonNode mockForm = mock(JsonNode.class);
    ReflectionTestUtils.setField(
        formService, "formDefinitions", java.util.Map.of("form1", mockForm));

    Set<String> keys = formService.getAvailableFormKeys();
    assertTrue(keys.contains("form1"));
  }

  @Test
  void getFormDefinition() {
    JsonNode mockForm = mock(JsonNode.class);
    ReflectionTestUtils.setField(
        formService, "formDefinitions", java.util.Map.of("form1", mockForm));

    JsonNode result = formService.getFormDefinition("form1");
    assertEquals(mockForm, result);
  }

  @Test
  void getFormDefinition_NotFound() {
    assertThrows(IllegalArgumentException.class, () -> formService.getFormDefinition("unknown"));
  }
}

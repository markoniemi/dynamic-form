package com.example.backend.testdata;

import com.example.backend.dto.FormDataDto;
import com.example.backend.entity.FormData;
import com.example.backend.mapper.FormDataMapper;
import java.time.LocalDateTime;
import java.util.Map;
import org.mapstruct.factory.Mappers;

/**
 * Ready-made submissions of {@link TestForms#contact()}. Every call returns a new instance. DTOs are
 * produced by the real {@link FormDataMapper}.
 */
public final class TestSubmissions {

  public static final LocalDateTime SUBMITTED_AT = LocalDateTime.of(2026, 1, 2, 3, 4);

  private static final FormDataMapper FORM_DATA_MAPPER = Mappers.getMapper(FormDataMapper.class);

  private TestSubmissions() {}

  /** Values for every field of {@link TestForms#contact()}. */
  public static Map<String, Object> contactData() {
    return Map.of(
        "name", "Test User",
        "email", "test@example.com",
        "phone", "+1 555 000 0001",
        "subject", "general",
        "message", "Hello from a test",
        "urgency", "low");
  }

  /** Unsaved submission, as a controller creates it: no id, no timestamp. */
  public static FormData contact(String submittedBy) {
    return new FormData("contact", contactData(), submittedBy);
  }

  /** Saved submission, as the repository returns it. */
  public static FormData contact(Long id, String submittedBy) {
    return new FormData(id, "contact", contactData(), SUBMITTED_AT, submittedBy);
  }

  public static FormDataDto toDto(FormData formData) {
    return FORM_DATA_MAPPER.toDto(formData);
  }
}

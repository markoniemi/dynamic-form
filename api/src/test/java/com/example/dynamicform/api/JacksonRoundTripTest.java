package com.example.dynamicform.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Guards the wire format for clients using a plain Jackson 2 ObjectMapper (no extra modules). */
class JacksonRoundTripTest {

  private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @Test
  void formDtoRoundTrips() throws Exception {
    FormDto form =
        FormDto.builder()
            .id(1L)
            .formKey("contact")
            .title("Contact")
            .description("d")
            .fields(
                List.of(
                    FieldDto.builder()
                        .name("color")
                        .label("Color")
                        .type("select")
                        .required(true)
                        .options(List.of(new FieldOptionDto("r", "Red")))
                        .build()))
            .build();

    assertEquals(form, mapper.readValue(mapper.writeValueAsString(form), FormDto.class));
  }

  @Test
  void formDataDtoRoundTrips() throws Exception {
    FormDataDto data =
        new FormDataDto(2L, "contact", Map.of("a", "b"), LocalDateTime.of(2026, 1, 2, 3, 4), "u");

    assertEquals(data, mapper.readValue(mapper.writeValueAsString(data), FormDataDto.class));
  }

  @Test
  void listItemAndValidationErrorRoundTrip() throws Exception {
    FormListItemDto item = new FormListItemDto("k", "t");
    ValidationErrorDto err = new ValidationErrorDto("f", "m", "NotBlank");

    assertEquals(item, mapper.readValue(mapper.writeValueAsString(item), FormListItemDto.class));
    assertEquals(err, mapper.readValue(mapper.writeValueAsString(err), ValidationErrorDto.class));
  }
}

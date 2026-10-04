package com.example.backend.mapper;

import static org.junit.jupiter.api.Assertions.*;

import com.example.backend.dto.FormDataDto;
import com.example.backend.entity.FormData;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class FormDataMapperTest {

  private static final LocalDateTime SUBMITTED_AT = LocalDateTime.of(2026, 1, 2, 3, 4);

  private final FormDataMapper formDataMapper = Mappers.getMapper(FormDataMapper.class);

  @Test
  void toDtoCopiesAllFields() {
    FormData entity = new FormData(5L, "form1", Map.of("field", "value"), SUBMITTED_AT, "user");

    assertEquals(
        new FormDataDto(5L, "form1", Map.of("field", "value"), SUBMITTED_AT, "user"),
        formDataMapper.toDto(entity));
  }

  @Test
  void toEntityCopiesAllFields() {
    FormDataDto dto = new FormDataDto(5L, "form1", Map.of("field", "value"), SUBMITTED_AT, "user");

    assertEquals(
        new FormData(5L, "form1", Map.of("field", "value"), SUBMITTED_AT, "user"),
        formDataMapper.toEntity(dto));
  }

  @Test
  void mapListMapsEachElementInOrder() {
    List<FormData> entities =
        List.of(
            new FormData(1L, "form1", Map.of(), SUBMITTED_AT, "user"),
            new FormData(2L, "form2", Map.of(), SUBMITTED_AT, "user"));

    List<FormDataDto> result = formDataMapper.mapList(entities);

    assertEquals(List.of(1L, 2L), result.stream().map(FormDataDto::getId).toList());
  }

  @Test
  void nullMapsToNull() {
    assertNull(formDataMapper.toDto(null));
    assertNull(formDataMapper.toEntity(null));
    assertNull(formDataMapper.mapList(null));
  }
}

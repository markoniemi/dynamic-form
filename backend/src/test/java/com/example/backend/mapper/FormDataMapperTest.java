package com.example.backend.mapper;

import static com.example.backend.testdata.TestSubmissions.SUBMITTED_AT;
import static com.example.backend.testdata.TestSubmissions.contact;
import static com.example.backend.testdata.TestSubmissions.contactData;
import static org.junit.jupiter.api.Assertions.*;

import com.example.backend.dto.FormDataDto;
import com.example.backend.entity.FormData;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class FormDataMapperTest {

  private final FormDataMapper formDataMapper = Mappers.getMapper(FormDataMapper.class);

  @Test
  void toDtoCopiesAllFields() {
    FormData entity = contact(5L, "user");

    assertEquals(
        new FormDataDto(5L, "contact", contactData(), SUBMITTED_AT, "user"),
        formDataMapper.toDto(entity));
  }

  @Test
  void toEntityCopiesAllFields() {
    FormDataDto dto = new FormDataDto(5L, "contact", contactData(), SUBMITTED_AT, "user");

    assertEquals(contact(5L, "user"), formDataMapper.toEntity(dto));
  }

  @Test
  void mapListMapsEachElementInOrder() {
    List<FormData> entities = List.of(contact(1L, "user"), contact(2L, "user"));

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

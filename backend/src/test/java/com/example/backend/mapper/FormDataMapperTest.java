package com.example.backend.mapper;

import static com.example.backend.testdata.TestSubmissions.SUBMITTED_AT;
import static com.example.backend.testdata.TestSubmissions.contact;
import static com.example.backend.testdata.TestSubmissions.contactData;
import static com.example.backend.testdata.TestUsers.USER;
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
    FormData entity = contact(5L, USER);

    assertEquals(
        new FormDataDto(5L, "contact", contactData(), SUBMITTED_AT, USER),
        formDataMapper.toDto(entity));
  }

  @Test
  void toEntityCopiesAllFields() {
    FormDataDto dto = new FormDataDto(5L, "contact", contactData(), SUBMITTED_AT, USER);

    assertEquals(contact(5L, USER), formDataMapper.toEntity(dto));
  }

  @Test
  void mapListMapsEachElementInOrder() {
    List<FormData> entities = List.of(contact(1L, USER), contact(2L, USER));

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

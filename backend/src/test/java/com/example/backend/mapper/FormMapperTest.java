package com.example.backend.mapper;

import static com.example.backend.testdata.TestForms.contact;
import static org.junit.jupiter.api.Assertions.*;

import com.example.backend.dto.FieldDto;
import com.example.backend.dto.FormDto;
import com.example.backend.entity.Field;
import com.example.backend.entity.Form;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class FormMapperTest {

  private final FormMapper formMapper = Mappers.getMapper(FormMapper.class);

  @Test
  void toDtoCopiesFormFieldsAndOptions() {
    Form form = contact();

    FormDto dto = formMapper.toDto(form);

    assertEquals(form.getId(), dto.getId());
    assertEquals(form.getFormKey(), dto.getFormKey());
    assertEquals(form.getTitle(), dto.getTitle());
    assertEquals(form.getDescription(), dto.getDescription());
    assertEquals(form.getFields().size(), dto.getFields().size());
    for (int i = 0; i < form.getFields().size(); i++) {
      assertFieldEquals(form.getFields().get(i), dto.getFields().get(i));
    }
  }

  @Test
  void toDtoKeepsOptionalField() {
    FieldDto phone = formMapper.toDto(contact()).getFields().get(2);

    assertEquals("phone", phone.getName());
    assertFalse(phone.isRequired());
  }

  @Test
  void toEntityReversesToDto() {
    Form original = contact();

    Form result = formMapper.toEntity(formMapper.toDto(original));

    assertEquals(original.getId(), result.getId());
    assertEquals(original.getFormKey(), result.getFormKey());
    assertEquals(original.getTitle(), result.getTitle());
    assertEquals(original.getDescription(), result.getDescription());
    assertEquals(original.getFields(), result.getFields());
  }

  @Test
  void nullMapsToNull() {
    assertNull(formMapper.toDto(null));
    assertNull(formMapper.toEntity(null));
  }

  private static void assertFieldEquals(Field expected, FieldDto actual) {
    assertEquals(expected.getName(), actual.getName());
    assertEquals(expected.getLabel(), actual.getLabel());
    assertEquals(expected.getType(), actual.getType());
    assertEquals(expected.isRequired(), actual.isRequired());
    assertEquals(expected.getPlaceholder(), actual.getPlaceholder());
    if (expected.getOptions() == null) {
      assertNull(actual.getOptions());
      return;
    }
    assertEquals(expected.getOptions().size(), actual.getOptions().size());
    for (int i = 0; i < expected.getOptions().size(); i++) {
      assertEquals(expected.getOptions().get(i).getValue(), actual.getOptions().get(i).getValue());
      assertEquals(expected.getOptions().get(i).getLabel(), actual.getOptions().get(i).getLabel());
    }
  }
}

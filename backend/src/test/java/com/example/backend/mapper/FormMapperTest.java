package com.example.backend.mapper;

import static org.junit.jupiter.api.Assertions.*;

import com.example.backend.dto.FieldDto;
import com.example.backend.dto.FormDto;
import com.example.backend.entity.Field;
import com.example.backend.entity.FieldOption;
import com.example.backend.entity.Form;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class FormMapperTest {

  private final FormMapper formMapper = Mappers.getMapper(FormMapper.class);

  private static Form form() {
    return Form.builder()
        .id(1L)
        .formKey("form1")
        .title("Title")
        .description("Description")
        .fields(
            List.of(
                Field.builder()
                    .name("color")
                    .label("Color")
                    .type("select")
                    .required(true)
                    .placeholder("Pick one")
                    .options(List.of(FieldOption.builder().value("red").label("Red").build()))
                    .build()))
        .build();
  }

  @Test
  void toDtoCopiesFormFieldsAndOptions() {
    FormDto dto = formMapper.toDto(form());

    assertEquals(1L, dto.getId());
    assertEquals("form1", dto.getFormKey());
    assertEquals("Title", dto.getTitle());
    assertEquals("Description", dto.getDescription());
    FieldDto field = dto.getFields().getFirst();
    assertEquals("color", field.getName());
    assertEquals("Color", field.getLabel());
    assertEquals("select", field.getType());
    assertTrue(field.isRequired());
    assertEquals("Pick one", field.getPlaceholder());
    assertEquals("red", field.getOptions().getFirst().getValue());
    assertEquals("Red", field.getOptions().getFirst().getLabel());
  }

  @Test
  void toEntityReversesToDto() {
    Form original = form();

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
}

package com.example.backend.testdata;

import com.example.backend.dto.FormDto;
import com.example.backend.dto.FormListItemDto;
import com.example.backend.entity.Field;
import com.example.backend.entity.FieldOption;
import com.example.backend.entity.Form;
import com.example.backend.mapper.FormMapper;
import java.util.List;
import org.mapstruct.factory.Mappers;

/**
 * Ready-made form definitions for tests. Every call returns a new instance, so a test may modify
 * its copy without affecting other tests. DTOs are produced by the real {@link FormMapper}.
 */
public final class TestForms {

  private static final FormMapper FORM_MAPPER = Mappers.getMapper(FormMapper.class);

  private TestForms() {}

  /** One required text field; use when a test only needs "a form". */
  public static Form simple() {
    return Form.builder()
        .id(1L)
        .formKey("simple")
        .title("Simple Form")
        .description("A form with one text field")
        .fields(List.of(field("name", "Name", "text", true)))
        .build();
  }

  /** Every field type with mixed required flags; mirrors {@code forms/contact.json}. */
  public static Form contact() {
    Field name = field("name", "Your Name", "text", true);
    name.setPlaceholder("John Doe");
    Field subject = field("subject", "Subject", "select", true);
    subject.setOptions(
        List.of(option("general", "General Inquiry"), option("support", "Technical Support")));
    Field urgency = field("urgency", "Priority Level", "radio", true);
    urgency.setOptions(List.of(option("low", "Low"), option("high", "High")));
    return Form.builder()
        .id(2L)
        .formKey("contact")
        .title("Contact Us")
        .description("Get in touch with our team")
        .fields(
            List.of(
                name,
                field("email", "Email Address", "email", true),
                field("phone", "Phone Number", "tel", false),
                subject,
                field("message", "Message", "textarea", true),
                urgency))
        .build();
  }

  public static FormDto simpleDto() {
    return FORM_MAPPER.toDto(simple());
  }

  public static FormDto contactDto() {
    return FORM_MAPPER.toDto(contact());
  }

  public static FormListItemDto listItem(Form form) {
    return new FormListItemDto(form.getFormKey(), form.getTitle());
  }

  private static Field field(String name, String label, String type, boolean required) {
    return Field.builder().name(name).label(label).type(type).required(required).build();
  }

  private static FieldOption option(String value, String label) {
    return FieldOption.builder().value(value).label(label).build();
  }
}

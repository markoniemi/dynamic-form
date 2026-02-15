package com.example.backend.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import com.example.backend.dto.FormDto;
import com.example.backend.entity.Form;
import org.mapstruct.Mapper;

@Mapper(componentModel = SPRING)
public interface FormMapper {
  FormDto toDto(Form entity);

  Form toEntity(FormDto dto);
}

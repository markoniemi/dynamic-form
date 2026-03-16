package com.example.backend.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import com.example.backend.dto.FormDto;
import com.example.backend.entity.Form;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = SPRING)
public interface FormMapper {
  FormDto toDto(Form entity);

  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  Form toEntity(FormDto dto);
}

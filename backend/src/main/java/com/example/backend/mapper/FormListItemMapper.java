package com.example.backend.mapper;

import com.example.backend.dto.FormListItemDto;
import com.example.backend.entity.Form;
import org.mapstruct.Mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING)
public interface FormListItemMapper {
  FormListItemDto toListItemDto(Form entity);
}

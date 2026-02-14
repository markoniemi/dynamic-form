package com.example.backend.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import com.example.backend.dto.FormDefinitionDto;
import com.example.backend.dto.FormFieldDto;
import com.example.backend.dto.FormFieldOptionDto;
import com.example.backend.entity.FormDefinition;
import com.example.backend.entity.FormFieldDefinition;
import com.example.backend.entity.FormFieldOption;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = SPRING)
public interface FormDefinitionMapper {

  FormDefinitionDto toDto(FormDefinition entity);

  FormDefinition toEntity(FormDefinitionDto dto);

  FormFieldDto toFieldDto(FormFieldDefinition entity);

  FormFieldDefinition toFieldEntity(FormFieldDto dto);

  FormFieldOptionDto toOptionDto(FormFieldOption entity);

  FormFieldOption toOptionEntity(FormFieldOptionDto dto);

  List<FormFieldDto> toFieldDtoList(List<FormFieldDefinition> entities);

  List<FormFieldDefinition> toFieldEntityList(List<FormFieldDto> dtos);

}


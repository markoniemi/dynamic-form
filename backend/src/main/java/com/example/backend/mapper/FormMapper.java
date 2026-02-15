package com.example.backend.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import com.example.backend.dto.FieldDto;
import com.example.backend.dto.FieldOptionDto;
import com.example.backend.dto.FormDto;
import com.example.backend.entity.Field;
import com.example.backend.entity.Form;
import com.example.backend.entity.FieldOption;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = SPRING)
public interface FormMapper {

  FormDto toDto(Form entity);

  Form toEntity(FormDto dto);

  FieldDto toFieldDto(Field entity);

  Field toFieldEntity(FieldDto dto);

  FieldOptionDto toOptionDto(FieldOption entity);

  FieldOption toOptionEntity(FieldOptionDto dto);

  List<FieldDto> toFieldDtoList(List<Field> entities);

  List<Field> toFieldEntityList(List<FieldDto> dtos);

}


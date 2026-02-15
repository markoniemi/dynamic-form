package com.example.backend.mapper;

import com.example.backend.dto.FieldDto;
import com.example.backend.dto.FieldOptionDto;
import com.example.backend.entity.Field;
import com.example.backend.entity.FieldOption;
import org.mapstruct.Mapper;

import java.util.List;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING)
public interface FieldMapper {
  FieldDto toFieldDto(Field entity);

  Field toFieldEntity(FieldDto dto);

  FieldOptionDto toOptionDto(FieldOption entity);

  FieldOption toOptionEntity(FieldOptionDto dto);

  List<FieldDto> toFieldDtoList(List<Field> entities);

  List<Field> toFieldEntityList(List<FieldDto> dtos);
}

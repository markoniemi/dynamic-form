package com.example.backend.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import com.example.backend.dto.FormDataDto;
import com.example.backend.entity.FormData;
import org.mapstruct.Mapper;

@Mapper(componentModel = SPRING)
public interface FormDataMapper {
    FormDataDto toDto(FormData formData);

    FormData toEntity(FormDataDto formDataDto);
}

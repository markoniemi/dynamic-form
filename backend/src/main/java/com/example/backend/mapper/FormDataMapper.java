package com.example.backend.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import com.example.dynamicform.api.FormDataDto;
import com.example.backend.entity.FormData;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = SPRING)
public interface FormDataMapper {
  FormDataDto toDto(FormData formData);
  FormData toEntity(FormDataDto formDataDto);
  List<FormDataDto> mapList(List<FormData> formDataList);
}

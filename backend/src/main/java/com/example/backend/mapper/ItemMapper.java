package com.example.backend.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import com.example.backend.dto.ItemDto;
import com.example.backend.entity.Item;
import org.mapstruct.Mapper;

@Mapper(componentModel = SPRING)
public interface ItemMapper {
  ItemDto toDto(Item item);

  Item toEntity(ItemDto itemDto);
}

package com.example.backend.dto;

import java.io.Serializable;
import lombok.*;

@Data
@AllArgsConstructor
@Value
public class ItemDto implements Serializable {
  Long id;
  @NonNull String name;
  @NonNull String description;
}

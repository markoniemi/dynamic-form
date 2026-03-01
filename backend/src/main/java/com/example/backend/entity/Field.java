package com.example.backend.entity;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Field {

  private String name;
  private String label;
  private String type;
  private boolean required;
  private String placeholder;
  private List<FieldOption> options;
}

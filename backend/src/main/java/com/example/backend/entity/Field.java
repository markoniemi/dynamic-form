package com.example.backend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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


package com.example.backend.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.NonNull;
import lombok.Value;

@Value
public class FormDataDto implements Serializable {
  Long id;
  @NonNull String formKey;
  @NonNull Map<String, Object> data;
  LocalDateTime submittedAt;
  String submittedBy;
}

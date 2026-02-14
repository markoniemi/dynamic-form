package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.Value;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
@Value
public class FormDataDto implements Serializable {
    Long id;
    @NonNull String formKey;
    @NonNull Map<String, Object> data;
    LocalDateTime submittedAt;
    String submittedBy;
}

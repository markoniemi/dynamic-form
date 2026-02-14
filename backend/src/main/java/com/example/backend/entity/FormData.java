package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "form_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
public class FormData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NonNull
    private String formKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    @NonNull
    private Map<String, Object> data;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @Column(nullable = false)
    @NonNull
    private String submittedBy;

}

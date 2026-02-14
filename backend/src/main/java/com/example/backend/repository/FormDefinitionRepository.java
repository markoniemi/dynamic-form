package com.example.backend.repository;

import com.example.backend.entity.FormDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FormDefinitionRepository extends JpaRepository<FormDefinition, Long> {

  Optional<FormDefinition> findByFormKey(String formKey);

  boolean existsByFormKey(String formKey);

}


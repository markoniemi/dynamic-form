package com.example.backend.repository;

import com.example.backend.entity.Form;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FormRepository extends JpaRepository<Form, Long> {

  Optional<Form> findByFormKey(String formKey);

  boolean existsByFormKey(String formKey);

}


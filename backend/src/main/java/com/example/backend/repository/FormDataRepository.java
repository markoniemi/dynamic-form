package com.example.backend.repository;

import com.example.backend.entity.FormData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FormDataRepository extends JpaRepository<FormData, Long> {

    List<FormData> findByFormKeyOrderBySubmittedAtDesc(String formKey);

}

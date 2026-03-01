package com.example.backend.repository;

import com.example.backend.entity.FormData;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FormDataRepository extends JpaRepository<FormData, Long> {

  List<FormData> findByFormKeyOrderBySubmittedAtDesc(String formKey);
}

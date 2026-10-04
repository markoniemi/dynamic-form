package com.example.backend.repository;

import static com.example.backend.testdata.TestSubmissions.contact;
import static com.example.backend.testdata.TestUsers.ADMIN;
import static com.example.backend.testdata.TestUsers.USER;
import static org.junit.jupiter.api.Assertions.*;

import com.example.backend.entity.FormData;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class FormDataRepositoryTest {

  private final FormDataRepository formDataRepository;

  @Autowired
  FormDataRepositoryTest(FormDataRepository formDataRepository) {
    this.formDataRepository = formDataRepository;
  }

  @Test
  void findBySubmittedByOrderBySubmittedAtDesc() {
    formDataRepository.save(contact(USER));
    formDataRepository.save(contact(USER));
    formDataRepository.save(contact(ADMIN));

    List<FormData> result = formDataRepository.findBySubmittedByOrderBySubmittedAtDesc(USER);

    assertEquals(2, result.size());
    assertTrue(result.stream().allMatch(f -> f.getSubmittedBy().equals(USER)));
  }
}

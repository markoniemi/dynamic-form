package com.example.backend.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.example.backend.entity.FormData;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class FormDataRepositoryTest {

  @Autowired private FormDataRepository formDataRepository;

  @Test
  void findBySubmittedByOrderBySubmittedAtDesc() {
    FormData formData1 = new FormData("form1", Map.of("field", "value1"), "username");
    FormData formData2 = new FormData("form1", Map.of("field", "value2"), "username");
    FormData formData3 = new FormData("form1", Map.of("field", "value3"), "otheruser");

    formDataRepository.save(formData1);
    formDataRepository.save(formData2);
    formDataRepository.save(formData3);

    List<FormData> result = formDataRepository.findBySubmittedByOrderBySubmittedAtDesc("username");

    assertEquals(2, result.size());
    assertTrue(result.stream().allMatch(f -> f.getFormKey().equals("form1")));
  }
}

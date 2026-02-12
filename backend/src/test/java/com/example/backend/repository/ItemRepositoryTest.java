package com.example.backend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.backend.entity.Item;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Import({ValidationAutoConfiguration.class})
@ExtendWith(SpringExtension.class)
@AutoConfigureDataJpa
@Transactional
@EnableJpaRepositories(basePackages = "com.example.backend.repository")
@EntityScan("com.example.backend.entity")
class ItemRepositoryTest {
  @Autowired private ItemRepository itemRepository;

  @Test
  void findByName() {
    Item item = new Item();
    item.setName("Test Item");
    item.setDescription("This is a test item.");
    itemRepository.save(item);

    Item found = itemRepository.findByName(item.getName());

    assertEquals(item.getName(), found.getName(), "Item name should match");
  }
}

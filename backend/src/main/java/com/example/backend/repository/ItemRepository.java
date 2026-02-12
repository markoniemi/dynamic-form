package com.example.backend.repository;

import com.example.backend.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findAllByOrderByCreatedAtDesc();

    Item findByName(String name);
}

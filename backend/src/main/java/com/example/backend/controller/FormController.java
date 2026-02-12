package com.example.backend.controller;

import com.example.backend.service.FormService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.log.InterfaceLog;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/forms")
@RequiredArgsConstructor
@Slf4j
@InterfaceLog
public class FormController {

    private final FormService formService;

    @GetMapping
    @InterfaceLog
    public Set<String> getAvailableForms() {
        return formService.getAvailableFormKeys();
    }

    @GetMapping("/{key}")
    @InterfaceLog
    public JsonNode getFormDefinition(@PathVariable String key) {
        return formService.getFormDefinition(key);
    }

}

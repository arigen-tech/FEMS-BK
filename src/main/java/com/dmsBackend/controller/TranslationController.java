package com.dmsBackend.controller;

import com.dmsBackend.entity.Translation;
import com.dmsBackend.service.TranslationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/translate")
public class TranslationController {

    @Autowired
    private TranslationService service;

    @PostMapping
    public ResponseEntity<Map<String, String>> translate(
            @RequestBody Map<String, String> req) {

        String text = req.get("text");
        String target = req.get("target");

        String translated = service.translate(text, target);

        return ResponseEntity.ok(Map.of("translatedText",
                translated != null ? translated : ""));
    }

    @PostMapping("/saveFallback")
    public void saveFallback(@RequestBody Translation translation) {
        service.saveFallback(translation);
    }


    @GetMapping("/all/{languageCode}")
    public ResponseEntity<Map<String, String>> getAllTranslations(
            @PathVariable String languageCode) {

        Map<String, String> result = service.getAllTranslations(languageCode);
        return ResponseEntity.ok(result);
    }


}
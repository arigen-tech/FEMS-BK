package com.dmsBackend.service;

import com.dmsBackend.entity.Translation;
import com.dmsBackend.repository.TranslationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TranslationService {

    @Autowired
    private TranslationRepository repository;

    public String translate(String text, String lang) {
        System.out.println("🔵 [BACKEND] translate called: text='" + text + "' lang='" + lang + "'");

        // Use List to avoid NonUniqueResultException on duplicate rows
        List<Translation> results =
                repository.findAllBySourceTextAndLanguageCode(text, lang);

        System.out.println("🔵 [BACKEND] DB results count: " + results.size());

        if (!results.isEmpty()) {
            // Pick best row: non-empty translated_text that differs from source
            Translation best = results.stream()
                    .filter(t -> t.getTranslatedText() != null
                            && !t.getTranslatedText().trim().isEmpty()
                            && !t.getTranslatedText().equalsIgnoreCase(text))
                    .findFirst()
                    .orElse(null);

            if (best != null) {
                String stored = best.getTranslatedText();
                System.out.println("🔵 [BACKEND] Stored value: '" + stored + "'");
                try {
                    String decoded = URLDecoder.decode(stored, "UTF-8");
                    System.out.println("✅ [BACKEND] Returning decoded: '" + decoded + "'");
                    return decoded;
                } catch (Exception e) {
                    System.out.println("⚠️ [BACKEND] Decode failed, returning raw: '" + stored + "'");
                    return stored;
                }
            } else {
                System.out.println("❌ [BACKEND] All stored values are empty or same as source");
            }
        } else {
            System.out.println("❌ [BACKEND] NOT FOUND in DB");
        }

        System.out.println("🌐 [BACKEND] Calling external API...");
        String translated = callTranslationAPI(text, lang);
        System.out.println("🌐 [BACKEND] API result: '" + translated + "'");

        if (translated != null
                && !translated.trim().isEmpty()
                && !translated.equalsIgnoreCase(text)) {

            // Delete all duplicate rows first, then save one clean row
            if (!results.isEmpty()) {
                repository.deleteAll(results);
            }

            Translation t = new Translation();
            t.setSourceText(text);
            t.setLanguageCode(lang);
            t.setTranslatedText(translated);
            repository.save(t);
            System.out.println("💾 [BACKEND] Saved to DB: '" + translated + "'");
            return translated;
        }

        System.out.println("❌ [BACKEND] Returning null for: '" + text + "'");
        return null;
    }

    private String callTranslationAPI(String text, String lang) {
        try {
            String url = "https://api.mymemory.translated.net/get?q="
                    + URLEncoder.encode(text, "UTF-8")
                    + "&langpair=en|" + lang;

            RestTemplate restTemplate = new RestTemplate();
            Map response = restTemplate.getForObject(url, Map.class);
            Map data = (Map) response.get("responseData");
            String translated = (String) data.get("translatedText");

            if (translated == null || translated.trim().isEmpty()) {
                return null;
            }

            // Decode URL encoding (%26 → &, etc.)
            try {
                return URLDecoder.decode(translated, "UTF-8");
            } catch (Exception e) {
                return translated;
            }

        } catch (Exception e) {
            System.out.println("❌ [BACKEND] API call failed: " + e.getMessage());
            return null;
        }
    }

    public void saveFallback(Translation translation) {
        System.out.println("💾 [saveFallback] sourceText='" + translation.getSourceText()
                + "' lang='" + translation.getLanguageCode()
                + "' translated='" + translation.getTranslatedText() + "'");

        if (translation.getSourceText() == null
                || translation.getSourceText().trim().isEmpty()) {
            System.out.println("❌ [saveFallback] Skipped — empty sourceText");
            return;
        }

        if (translation.getTranslatedText() == null
                || translation.getTranslatedText().trim().isEmpty()) {
            System.out.println("❌ [saveFallback] Skipped — empty translatedText");
            return;
        }

        if (translation.getTranslatedText().equalsIgnoreCase(translation.getSourceText())) {
            System.out.println("❌ [saveFallback] Skipped — translated same as source");
            return;
        }

        // Use List to avoid NonUniqueResultException
        List<Translation> existing =
                repository.findAllBySourceTextAndLanguageCode(
                        translation.getSourceText(),
                        translation.getLanguageCode());

        if (existing.isEmpty()) {
            // No row exists — save new
            repository.save(translation);
            System.out.println("✅ [saveFallback] Saved new translation");
        } else if (existing.size() == 1) {
            // One row exists — update it with latest value
            Translation t = existing.get(0);
            t.setTranslatedText(translation.getTranslatedText());
            repository.save(t);
            System.out.println("✅ [saveFallback] Updated existing translation");
        } else {
            // Multiple duplicate rows — delete all, save one clean row
            repository.deleteAll(existing);
            Translation t = new Translation();
            t.setSourceText(translation.getSourceText());
            t.setLanguageCode(translation.getLanguageCode());
            t.setTranslatedText(translation.getTranslatedText());
            repository.save(t);
            System.out.println("✅ [saveFallback] Replaced " + existing.size() + " duplicates with one clean row");
        }
    }

    public Map<String, String> getAllTranslations(String languageCode) {
        List<Translation> all = repository.findAllByLanguageCode(languageCode);

        Map<String, String> map = new LinkedHashMap<>();
        for (Translation t : all) {
            if (t.getTranslatedText() != null
                    && !t.getTranslatedText().trim().isEmpty()
                    && !t.getTranslatedText().equalsIgnoreCase(t.getSourceText())) {
                try {
                    String decoded = URLDecoder.decode(t.getTranslatedText(), "UTF-8");
                    map.put(t.getSourceText(), decoded);
                } catch (Exception e) {
                    map.put(t.getSourceText(), t.getTranslatedText());
                }
            }
        }
        return map;
    }
}
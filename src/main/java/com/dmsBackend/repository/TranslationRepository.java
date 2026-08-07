package com.dmsBackend.repository;

import com.dmsBackend.entity.Translation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TranslationRepository extends JpaRepository<Translation, Long> {

    Optional<Translation> findBySourceTextAndLanguageCode(String sourceText, String languageCode);

    List<Translation> findAllByLanguageCode(String languageCode);

    List<Translation> findAllBySourceTextAndLanguageCode(String sourceText, String languageCode);}

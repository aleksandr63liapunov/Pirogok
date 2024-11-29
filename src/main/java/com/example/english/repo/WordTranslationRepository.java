package com.example.english.repo;

import com.example.english.model.WordTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface WordTranslationRepository extends JpaRepository<WordTranslation, Long> {

    // Найти перевод по слову
    WordTranslation findByWord(String word);

    @Query("SELECT DISTINCT w.word FROM WordTranslation w ORDER BY FUNCTION('RANDOM')")
    List<WordTranslation> findAllUniqueWordsRandomly();

}
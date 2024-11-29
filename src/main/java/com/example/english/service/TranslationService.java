package com.example.english.service;

import com.example.english.model.WordTranslation;
import com.example.english.repo.WordTranslationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class TranslationService {

    private final WordTranslationRepository wordTranslationRepository;
    private final RestTemplate restTemplate;
    @Value("${api.key}")
    private String apiKey ; // Убедитесь, что ваш ключ правильный
    @Value("${api.url}")
    private String baseUrl ; // Правильный endpoint

    public TranslationService(WordTranslationRepository wordTranslationRepository, RestTemplate restTemplate) {
        this.wordTranslationRepository = wordTranslationRepository;
        this.restTemplate = restTemplate;
    }

    // Переводим слово
    public String translateWord(String word, String fromLang, String toLang) {
        try {
            // Формируем строку для языка в формате "en-ru", "ru-en" и т.д.
            String lang = fromLang + "-" + toLang;

            // Формирование полного URL запроса
            String url = baseUrl + "?key=" + apiKey + "&lang=" + lang + "&text=" + word;

            // Выполнение GET-запроса
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.GET,
                    null,
                    String.class
            );

            // Парсим ответ от API и сохраняем в базу данных
            String translation = parseTranslation(response.getBody());
            saveTranslation(word, translation);

            return translation;
        } catch (Exception e) {
            // Обработка ошибок
            return "Error fetching translation for word '" + word + "': " + e.getMessage();
        }
    }

    // Метод для парсинга ответа от API
    private String parseTranslation(String responseBody) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(responseBody);

            if (jsonNode.has("def")) {
                JsonNode definitions = jsonNode.get("def");
                if (definitions.isArray() && definitions.size() > 0) {
                    JsonNode firstDef = definitions.get(0);
                    JsonNode firstTranslation = firstDef.get("tr").get(0);

                    // Основной перевод
                    String translation = firstTranslation.get("text").asText();

                    // Проверяем наличие примеров
                    StringBuilder examples = new StringBuilder();
                    if (firstTranslation.has("ex")) {
                        JsonNode examplesArray = firstTranslation.get("ex");
                        for (JsonNode exampleNode : examplesArray) {
                            String originalText = exampleNode.get("text").asText(); // Пример на исходном языке
                            String translatedText = exampleNode.get("tr").get(0).get("text").asText(); // Пример на целевом языке

                            examples.append("- ").append(originalText).append(" -> ").append(translatedText).append("\n");
                        }
                    }

                    return translation + (examples.length() > 0 ? "\nExamples:\n" + examples : "");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Translation not found.";
    }

    // Сохраняем слово и перевод в базу данных
    private void saveTranslation(String word, String translation) {
        if (!translation.equals("Translation not found.")) {
            // Проверяем, что слово и перевод ещё не существуют в базе данных
            boolean exists = wordTranslationRepository.findAll().stream()
                    .anyMatch(wt -> wt.getWord().equals(word) && wt.getTranslation().equals(translation));

            // Если такой записи нет, сохраняем
            if (!exists) {
                WordTranslation wordTranslation = new WordTranslation();
                wordTranslation.setWord(word);
                wordTranslation.setTranslation(translation);
                wordTranslationRepository.save(wordTranslation);
            }
        }
    }

    public List<WordTranslation> getAllWordsRandom() {
        // Извлекаем все слова
        List<WordTranslation> allWords = wordTranslationRepository.findAll()
                .stream()
                .distinct() // Исключаем повторения
                .collect(Collectors.toList());
        // Перемешиваем список
        Collections.shuffle(allWords);
        return allWords;
    }
}

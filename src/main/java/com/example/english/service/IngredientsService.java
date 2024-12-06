package com.example.english.service;

import com.example.english.model.Cocktails;
import com.example.english.model.Ingredients;
import com.example.english.model.Person;
import com.example.english.repo.IngredientsRepository;
import com.example.english.repo.PersonRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@AllArgsConstructor
public class IngredientsService {
    private final IngredientsRepository ingredientsRepository;
    private final PersonRepository personRepository;

    public Ingredients findIngredientsByCocktails(Cocktails cocktails) {
        return ingredientsRepository.findIngredientsByCocktail(cocktails);
    }

    public List<Ingredients> findAllIngredients() {
        return ingredientsRepository.findAll();

    }
    public Map<String, Double> getIngredientsForPerson(Cocktails cocktail, int person) {
        // Проверяем, чтобы количество персон было больше 0
        if (person <= 0) {
            throw new IllegalArgumentException("Number of persons must be greater than 0");
        }

        // Получаем список всех ингредиентов
        List<Ingredients> ingredients = findAllIngredients();

        // Список маппинга полей и их названий
        Map<String, Function<Ingredients, Double>> ingredientFields = Map.of(
                "Treeple_sec", Ingredients::getTreeple_sec,
                "Aperol", Ingredients::getAperol,
                "Jin", Ingredients::getJin,
                "Rome", Ingredients::getRome,
                "Tequila", Ingredients::getTequila,
                "Wiskey", Ingredients::getWiskey,
                "Апельсин сок", Ingredients::getOrange_Juice,
                "Клюквен сок", Ingredients::getCranberry_Juice,
                "Ананас сок", Ingredients::getPineapple_Juice,
                "Тоник", Ingredients::getTonic
        );

        // Создаем карту для хранения итоговых ингредиентов
        Map<String, Double> personIngredients = new LinkedHashMap<>();

        // Вычисляем объем для каждого ингредиента
        ingredientFields.forEach((name, getter) -> {
            double total = Math.round(ingredients.stream()
                    .mapToDouble(getter::apply)
                    .sum() * person * 10) / 10.0;
            if (total > 0) {
                personIngredients.put(name, total);
            }
        });

        return personIngredients;
    }
//    public Map<String, Double> getIngredientsForPerson(Cocktails cocktail, int person) {
//        // Проверяем, чтобы количество персон было больше 0
//        if (person <= 0) {
//            throw new IllegalArgumentException("Number of persons must be greater than 0");
//        }
//
//        // Получаем список всех ингредиентов
//        List<Ingredients> ingredients = findAllIngredients();
//
//        // Создаем карту для хранения ингредиентов
//        Map<String, Double> personIngredients = new LinkedHashMap<>();
//
//        // Умножаем объем каждого ингредиента на количество человек и добавляем в карту
//        double treepleSecTotal = Math.round(ingredients.stream()
//                .mapToDouble(Ingredients::getTreeple_sec)
//                .sum() * person*10)/10.0;
//        if (treepleSecTotal > 0) {
//            personIngredients.put("Treeple_sec", treepleSecTotal);
//        }
//
//        double aperolTotal =  Math.round(ingredients.stream()
//                .mapToDouble(Ingredients::getAperol)
//                .sum() * person*10)/10.0;
//        if (aperolTotal > 0) {
//            personIngredients.put("Aperol", aperolTotal);
//        }
//
//        double jinTotal =  Math.round(ingredients.stream()
//                .mapToDouble(Ingredients::getJin)
//                .sum() * person*10)/10.0;
//        if (jinTotal > 0) {
//            personIngredients.put("Jin", jinTotal);
//        }
//
//        double romeTotal =  Math.round(ingredients.stream()
//                .mapToDouble(Ingredients::getRome)
//                .sum() * person*10)/10.0;
//        if (romeTotal > 0) {
//            personIngredients.put("Rome", romeTotal);
//        }
//
//        double tequilaTotal =  Math.round(ingredients.stream()
//                .mapToDouble(Ingredients::getTequila)
//                .sum() * person*10)/10.0;
//        if (tequilaTotal > 0) {
//            personIngredients.put("Tequila", tequilaTotal);
//        }
//
//        double wiskeyTotal =  Math.round(ingredients.stream()
//                .mapToDouble(Ingredients::getWiskey)
//                .sum() * person*10)/10.0;
//        if (wiskeyTotal > 0) {
//            personIngredients.put("Wiskey", wiskeyTotal);
//        }
//        double orangeTotal =  Math.round(ingredients.stream()
//                .mapToDouble(Ingredients::getOrange_Juice)
//                .sum() * person*10)/10.0;
//        if (orangeTotal > 0) {
//            personIngredients.put("Апельсин сок",orangeTotal);
//        }
//        double cranberryTotal =  Math.round(ingredients.stream()
//                .mapToDouble(Ingredients::getCranberry_Juice)
//                .sum() * person*10)/10.0;
//        if (cranberryTotal > 0) {
//            personIngredients.put("Клюквен сок",cranberryTotal);
//        }
//        double pine_appleTotal =  Math.round(ingredients.stream()
//                .mapToDouble(Ingredients::getPineapple_Juice)
//                .sum() * person*10)/10.0;
//        if (pine_appleTotal > 0) {
//            personIngredients.put("Ананас сок",pine_appleTotal);
//        }
//        double tonicTotal =  Math.round(ingredients.stream()
//                .mapToDouble(Ingredients::getTonic)
//                .sum() * person*10)/10.0;
//        if (tonicTotal > 0) {
//            personIngredients.put("Тоник",tonicTotal);
//        }
//
//        return personIngredients;
//    }

}

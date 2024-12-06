package com.example.english.repo;

import com.example.english.model.Cocktails;
import com.example.english.model.Ingredients;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientsRepository extends JpaRepository<Ingredients, Long> {
    Ingredients findIngredientsByCocktail(Cocktails cocktails);
}

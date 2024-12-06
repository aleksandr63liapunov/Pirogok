package com.example.english.repo;

import com.example.english.model.Cocktails;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CocktailsRepository extends JpaRepository<Cocktails, Long> {

}

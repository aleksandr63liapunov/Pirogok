package com.example.english.service;

import com.example.english.model.Cocktails;
import com.example.english.repo.CocktailsRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class CocktailsService {
    private final CocktailsRepository cocktailsRepository;

    public List<Cocktails> getAll(){
        return cocktailsRepository.findAll();
    }

}

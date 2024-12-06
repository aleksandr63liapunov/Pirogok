package com.example.english.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Ingredients {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = true)
    private double Rome;
    @Column(nullable = true)
    private double Aperol;
    @Column(nullable = true)
    private double Sparkling_wine;
    @Column(nullable = true)
    private double Jin;
    @Column(nullable = true)
    private double Wiskey;
    @Column(nullable = true)
    private double Tequila;
    @Column(nullable = true)
    private double Treeple_sec;
    @Column(nullable = true)
    private double Limon;
    @Column(nullable = true)
    private double Cranberry_Juice;
    @Column(nullable = true)
    private double Pineapple_Juice;
    @Column(nullable = true)
    private double Orange_Juice;
    @Column(nullable = true)
    private double Sparkling_water;
    @Column(nullable = true)
    private double Tonic;
    @Column(nullable = true)
    private double Seven_up;
    // Обратная ссылка на коктейль (опционально)
    @OneToOne(mappedBy = "ingredients")
    private Cocktails cocktail;
}

package com.example.english.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = true)
    private double romePers;
    @Column(nullable = true)
    private double aperolPers;
    @Column(nullable = true)
    private double jinPers;
    @Column(nullable = true)
    private double wiskeyPers;
    @Column(nullable = true)
    private double tequilaPers;
    @Column(nullable = true)
    private double treeple_secPers;

}

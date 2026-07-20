package com.batalha.Batalha_Naval.quiz;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CriarQuizRequest {
    private String nome;
    private String senha;
    private List<String> categorias;
    private String dificuldade;
}

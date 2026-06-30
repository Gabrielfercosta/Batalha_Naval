package com.batalha.Batalha_Naval.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TiroRequest {
    private String jogador;
    private int linha;
    private int coluna;
}

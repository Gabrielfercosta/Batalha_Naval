package com.batalha.Batalha_Naval.dto;

import lombok.Getter;

@Getter
public class ErroResponse {
    private final String mensagem;

    public ErroResponse(String mensagem) {
        this.mensagem = mensagem;
    }
}

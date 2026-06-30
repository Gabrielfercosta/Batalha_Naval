package com.batalha.Batalha_Naval.dto;

import com.batalha.Batalha_Naval.dominio.ResultadoTiro;
import com.batalha.Batalha_Naval.dominio.StatusPartida;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TiroResponse {
    private final String autor;
    private final int linha;
    private final int coluna;
    private final ResultadoTiro resultado;
    private final String turnoAtual;
    private final StatusPartida status;
    private final String vencedor;
}

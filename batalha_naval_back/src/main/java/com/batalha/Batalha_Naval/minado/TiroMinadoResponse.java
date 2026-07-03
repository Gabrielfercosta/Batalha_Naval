package com.batalha.Batalha_Naval.minado;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class TiroMinadoResponse {
    private final String autor;
    private final int linha;
    private final int coluna;
    private final ResultadoTiroMinado resultado;
    private final String turnoAtual;
    private final StatusPartidaMinada status;
    private final String vencedor;
    private final List<CasaRevelada> casasReveladas;
}

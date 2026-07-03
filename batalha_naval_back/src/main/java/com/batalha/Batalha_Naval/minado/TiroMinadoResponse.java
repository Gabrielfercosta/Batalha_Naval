package com.batalha.Batalha_Naval.minado.dto;

import com.batalha.Batalha_Naval.minado.CasaRevelada;
import com.batalha.Batalha_Naval.minado.ResultadoTiroMinado;
import com.batalha.Batalha_Naval.minado.StatusPartidaMinada;
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
    private final int tirosRestantes;
    private final List<CasaRevelada> casasReveladas;
}

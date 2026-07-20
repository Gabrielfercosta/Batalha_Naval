package com.batalha.Batalha_Naval.quiz;

import com.batalha.Batalha_Naval.dominio.ResultadoTiro;
import com.batalha.Batalha_Naval.dominio.StatusPartida;
import com.batalha.Batalha_Naval.dto.NavioRevelado;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TiroQuizResponse {
    private final String autor;
    private final int linha;
    private final int coluna;
    private final ResultadoTiro resultado;
    private final NavioRevelado navioAfundado;
    private final String proximoAtirador;
    private final StatusPartida status;
    private final String vencedor;

    public String getTipo() {
        return "TIRO";
    }
}

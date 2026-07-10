package com.batalha.Batalha_Naval.minado;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.batalha.Batalha_Naval.dominio.StatusPartida;
import java.util.List;

@Getter
@AllArgsConstructor
public class TiroMinadoResponse {
    private final String autor;
    private final int linha;
    private final int coluna;
    private final ResultadoTiroMinado resultado;
    private final String turnoAtual;
    private final StatusPartida status;
    private final String vencedor;
    private final List<CasaRevelada> casasReveladas;
}

package com.batalha.Batalha_Naval.dto;

import com.batalha.Batalha_Naval.dominio.Direcao;
import com.batalha.Batalha_Naval.dominio.TipoNavio;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PosicionarNavioRequest {
    private String jogador;
    private TipoNavio tipo;
    private int linha;
    private int coluna;
    private Direcao direcao;
}

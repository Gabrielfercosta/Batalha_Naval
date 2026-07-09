package com.batalha.Batalha_Naval.minado;

import com.batalha.Batalha_Naval.dominio.Direcao;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PosicionarNavioMinadoRequest {
    private int linha;
    private int coluna;
    private int tamanho;
    private Direcao direcao;
}

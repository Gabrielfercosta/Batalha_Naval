package com.batalha.Batalha_Naval.dominio;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class Coordenada {

    private final int linha;
    private final int coluna;
}

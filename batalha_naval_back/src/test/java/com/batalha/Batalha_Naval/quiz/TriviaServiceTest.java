package com.batalha.Batalha_Naval.quiz;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TriviaServiceTest {

    private TriviaService triviaService;

    @BeforeEach
    void setUp() {
        triviaService = new TriviaService();
    }

    @Test
    void sortearPerguntaSemFiltroRetornaPergunta() {
        PerguntaTrivia pergunta = triviaService.sortearPergunta(null, null);

        assertNotNull(pergunta);
        assertNotNull(pergunta.getPergunta());
        assertFalse(pergunta.getPergunta().isBlank());
        assertNotNull(pergunta.getOpcoes());
        assertFalse(pergunta.getOpcoes().isEmpty());
        assertNotNull(pergunta.getRespostaCorreta());
    }

    @Test
    void sortearPerguntaComCategoria() {
        PerguntaTrivia pergunta = triviaService.sortearPergunta(List.of("geral"), null);

        assertNotNull(pergunta);
        assertNotNull(pergunta.getPergunta());
    }

    @Test
    void sortearPerguntaComDificuldade() {
        PerguntaTrivia pergunta = triviaService.sortearPergunta(null, "easy");

        assertNotNull(pergunta);
        assertEquals("easy", pergunta.getDificuldade());
    }

    @Test
    void sortearPerguntaComCategoriaEDificuldade() {
        PerguntaTrivia pergunta = triviaService.sortearPergunta(List.of("geral"), "medium");

        assertNotNull(pergunta);
    }

    @Test
    void sortearPerguntaComCategoriaInexistenteRetornaQualquerUma() {
        PerguntaTrivia pergunta = triviaService.sortearPergunta(List.of("categoria_inexistente_xyz"), null);

        // Quando não encontra nenhuma na categoria, retorna qualquer uma do banco
        assertNotNull(pergunta);
        assertNotNull(pergunta.getPergunta());
    }

    @Test
    void perguntaTemOpcoes() {
        PerguntaTrivia pergunta = triviaService.sortearPergunta(null, null);

        assertTrue(pergunta.getOpcoes().size() >= 2);
        assertTrue(pergunta.getOpcoes().contains(pergunta.getRespostaCorreta()));
    }

    @Test
    void perguntasSaoAleatorias() {
        // Sorteia 10 perguntas e verifica que não são todas iguais
        long distintas = java.util.stream.IntStream.range(0, 10)
                .mapToObj(i -> triviaService.sortearPergunta(null, null).getPergunta())
                .distinct()
                .count();

        assertTrue(distintas > 1, "Deveria sortear perguntas diferentes");
    }
}

package com.batalha.Batalha_Naval.quiz;

import java.util.List;

public class PerguntaTrivia {

    private final String pergunta;
    private final List<String> opcoes;
    private final String respostaCorreta;
    private final String dificuldade;

    public PerguntaTrivia(String pergunta, List<String> opcoes, String respostaCorreta, String dificuldade) {
        this.pergunta = pergunta;
        this.opcoes = opcoes;
        this.respostaCorreta = respostaCorreta;
        this.dificuldade = dificuldade;
    }

    public String getPergunta() { return pergunta; }
    public List<String> getOpcoes() { return opcoes; }
    public String getRespostaCorreta() { return respostaCorreta; }
    public String getDificuldade() { return dificuldade; }
    public boolean estaCorreta(String resposta) { return respostaCorreta.equals(resposta); }
}

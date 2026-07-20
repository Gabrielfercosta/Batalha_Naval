package com.batalha.Batalha_Naval.quiz;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Service
public class TriviaService {

    private final List<PerguntaBanco> banco;
    private final Random random = new Random();

    public TriviaService() {
        this.banco = carregarBanco();
    }

    private List<PerguntaBanco> carregarBanco() {
        try (InputStream entrada = new ClassPathResource("perguntas.json").getInputStream()) {
            return new ObjectMapper().readValue(entrada, new TypeReference<List<PerguntaBanco>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Não consegui carregar o banco de perguntas.", e);
        }
    }

    public PerguntaTrivia sortearPergunta(List<String> categorias, String dificuldade) {
        List<PerguntaBanco> filtradas = new ArrayList<>();
        for (PerguntaBanco p : banco) {
            boolean categoriaOk = categorias == null || categorias.isEmpty() || categorias.contains(p.categoria);
            boolean dificuldadeOk = dificuldade == null || dificuldade.isBlank() || dificuldade.equals(p.dificuldade);
            if (categoriaOk && dificuldadeOk) {
                filtradas.add(p);
            }
        }
        if (filtradas.isEmpty()) {
            filtradas = banco;
        }
        return montar(filtradas.get(random.nextInt(filtradas.size())));
    }

    private PerguntaTrivia montar(PerguntaBanco p) {
        List<String> opcoes = new ArrayList<>();
        opcoes.add(p.correta);
        opcoes.addAll(p.incorretas);
        Collections.shuffle(opcoes);
        return new PerguntaTrivia(p.pergunta, opcoes, p.correta);
    }

    static class PerguntaBanco {
        public String categoria;
        public String dificuldade;
        public String pergunta;
        public String correta;
        public List<String> incorretas;
    }
}

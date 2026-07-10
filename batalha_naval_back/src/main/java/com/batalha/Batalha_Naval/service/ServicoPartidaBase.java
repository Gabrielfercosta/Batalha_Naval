package com.batalha.Batalha_Naval.service;

import com.batalha.Batalha_Naval.dominio.PartidaBase;
import com.batalha.Batalha_Naval.dominio.StatusPartida;
import com.batalha.Batalha_Naval.dto.SalaResponse;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ServicoPartidaBase<T extends PartidaBase> {

    protected final Map<String, T> partidas = new ConcurrentHashMap<>();

    protected abstract T novaPartida(String jogador, String nome, String senha);

    public String criarPartida(String jogador, String nome, String senha) {
        String id = UUID.randomUUID().toString();
        partidas.put(id, novaPartida(jogador, nome, senha));
        return id;
    }

    public T buscarPartida(String gameId) {
        T partida = partidas.get(gameId);
        if (partida == null) {
            throw new IllegalArgumentException("Partida não encontrada: " + gameId);
        }
        return partida;
    }

    public T entrarNaPartida(String gameId, String jogador, String senha) {
        T partida = buscarPartida(gameId);
        partida.entrar(jogador, senha);
        return partida;
    }

    public T marcarPronto(String gameId, String jogador) {
        T partida = buscarPartida(gameId);
        partida.marcarPronto(jogador);
        return partida;
    }

    public T sairDaPartida(String gameId, String jogador) {
        T partida = partidas.get(gameId);
        if (partida == null) return null;

        if (partida.getStatus() == StatusPartida.EM_ANDAMENTO) {
            partida.abandonar(jogador);
            return partida;
        }
        if (partida.getStatus() == StatusPartida.FINALIZADA) {
            partidas.remove(gameId);
            return null;
        }
        if (jogador.equals(partida.getJogador1())) {
            partidas.remove(gameId);
        } else if (jogador.equals(partida.getJogador2())) {
            partida.removerJogador2();
        }
        return null;
    }

    public List<SalaResponse> listarPartidasAbertas() {
        List<SalaResponse> abertas = new ArrayList<>();
        for (Map.Entry<String, T> entrada : partidas.entrySet()) {
            T p = entrada.getValue();
            if (p.getStatus() == StatusPartida.AGUARDANDO) {
                boolean temSenha = p.getSenha() != null && !p.getSenha().isBlank();
                abertas.add(new SalaResponse(entrada.getKey(), p.getNome(), temSenha, p.getJogador1(), 1));
            }
        }
        return abertas;
    }

    @Scheduled(fixedRate = 60000)
    public void limparSalasAbandonadas() {
        long agora = System.currentTimeMillis();
        long limite = 5 * 60 * 1000;
        partidas.entrySet().removeIf(e ->
                e.getValue().getStatus() == StatusPartida.AGUARDANDO
                        && agora - e.getValue().getCriadaEm() > limite);
    }
}

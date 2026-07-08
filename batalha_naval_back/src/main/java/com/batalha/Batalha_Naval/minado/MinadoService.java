package com.batalha.Batalha_Naval.minado;

import com.batalha.Batalha_Naval.dominio.Direcao;
import com.batalha.Batalha_Naval.dto.SalaResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MinadoService {

    private final Map<String, PartidaMinada> partidas = new ConcurrentHashMap<>();

    public String criarPartida(String jogador, String nome, String senha) {
        String id = UUID.randomUUID().toString();
        PartidaMinada partida = new PartidaMinada(jogador, nome, senha);
        partidas.put(id, partida);
        return id;
    }

    public PartidaMinada buscarPartida(String gameId) {
        PartidaMinada partida = partidas.get(gameId);
        if (partida == null) {
            throw new IllegalArgumentException("Partida minada não encontrada: " + gameId);
        }
        return partida;
    }

    public PartidaMinada entrarNaPartida(String gameId, String jogador, String senha) {
        PartidaMinada partida = buscarPartida(gameId);
        partida.entrar(jogador, senha);
        return partida;
    }

    public PartidaMinada sairDaPartida(String gameId, String jogador) {
        PartidaMinada partida = partidas.get(gameId);
        if (partida == null) return null;

        if (partida.getStatus() == StatusPartidaMinada.EM_ANDAMENTO) {
            partida.abandonar(jogador);
            return partida;
        }

        if (partida.getStatus() == StatusPartidaMinada.FINALIZADA) {
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

    @Scheduled(fixedRate = 60000)
    public void limparSalasAbandonadas() {
        long agora = System.currentTimeMillis();
        long limite = 5 * 60 * 1000;
        partidas.entrySet().removeIf(e ->
                e.getValue().getStatus() == StatusPartidaMinada.AGUARDANDO
                        && agora - e.getValue().getCriadaEm() > limite);
    }

    public List<SalaResponse> listarPartidasAbertas() {
        List<SalaResponse> abertas = new ArrayList<>();
        for (String id : partidas.keySet()) {
            PartidaMinada p = partidas.get(id);
            if (p.getStatus() == StatusPartidaMinada.AGUARDANDO) {
                boolean temSenha = p.getSenha() != null && !p.getSenha().isBlank();
                abertas.add(new SalaResponse(id, p.getNome(), temSenha));
            }
        }
        return abertas;
    }

    public void posicionarNavio(String gameId, String jogador, int linha, int coluna, int tamanho, Direcao direcao) {
        PartidaMinada partida = buscarPartida(gameId);
        partida.posicionarNavio(jogador, linha, coluna, tamanho, direcao);
    }

    public void posicionarMina(String gameId, String jogador, int linha, int coluna) {
        PartidaMinada partida = buscarPartida(gameId);
        partida.posicionarMina(jogador, linha, coluna);
    }

    public PartidaMinada marcarPronto(String gameId, String jogador) {
        PartidaMinada partida = buscarPartida(gameId);
        partida.marcarPronto(jogador);
        return partida;
    }

    public ResultadoTiroMinado atirar(String gameId, String jogador, int linha, int coluna) {
        PartidaMinada partida = buscarPartida(gameId);
        return partida.atirar(jogador, linha, coluna);
    }
}

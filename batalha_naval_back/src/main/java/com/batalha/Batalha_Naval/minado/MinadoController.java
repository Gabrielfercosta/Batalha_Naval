package com.batalha.Batalha_Naval.minado;

import com.batalha.Batalha_Naval.config.PartidasMetrics;
import com.batalha.Batalha_Naval.dominio.StatusPartida;
import com.batalha.Batalha_Naval.dto.CriarPartidaRequest;
import com.batalha.Batalha_Naval.dto.EntrarPartidaRequest;
import com.batalha.Batalha_Naval.dto.SalaResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/minado")
public class MinadoController {

    private final MinadoService minadoService;
    private final SimpMessagingTemplate messagingTemplate;
    private final PartidasMetrics partidasMetrics;

    public MinadoController(MinadoService minadoService, SimpMessagingTemplate messagingTemplate, PartidasMetrics partidasMetrics) {
        this.minadoService = minadoService;
        this.messagingTemplate = messagingTemplate;
        this.partidasMetrics = partidasMetrics;
    }

    @PostMapping("/create")
    public PartidaMinadaResponse criar(@RequestBody CriarPartidaRequest request, Principal principal) {
        String gameId = minadoService.criarPartida(principal.getName(), request.getNome(), request.getSenha());
        partidasMetrics.partidaCriada("minado");
        return new PartidaMinadaResponse(gameId, minadoService.buscarPartida(gameId));
    }

    @PostMapping("/{gameId}/join")
    public PartidaMinadaResponse entrar(@PathVariable String gameId, @RequestBody EntrarPartidaRequest request, Principal principal) {
        PartidaMinada p = minadoService.entrarNaPartida(gameId, principal.getName(), request.getSenha());
        return new PartidaMinadaResponse(gameId, p);
    }

    @GetMapping("/open")
    public List<SalaResponse> listarAbertas() {
        return minadoService.listarPartidasAbertas();
    }

    @GetMapping("/{gameId}")
    public PartidaMinadaResponse buscar(@PathVariable String gameId) {
        return new PartidaMinadaResponse(gameId, minadoService.buscarPartida(gameId));
    }

    @PostMapping("/{gameId}/sair")
    public void sair(@PathVariable String gameId, Principal principal) {
        PartidaMinada partida = minadoService.sairDaPartida(gameId, principal.getName());
        if (partida != null) {
            TiroMinadoResponse aviso = new TiroMinadoResponse(
                    null, -1, -1, null,
                    partida.getTurnoAtual(), partida.getStatus(), partida.getVencedor(), null);
            messagingTemplate.convertAndSend("/topic/minado/" + gameId, aviso);
        }
    }

    @PostMapping("/{gameId}/navio")
    public PartidaMinadaResponse posicionarNavio(@PathVariable String gameId, @RequestBody PosicionarNavioMinadoRequest request, Principal principal) {
        minadoService.posicionarNavio(gameId,
                principal.getName(),
                request.getLinha(),
                request.getColuna(),
                request.getTamanho(),
                request.getDirecao());
        return new PartidaMinadaResponse(gameId, minadoService.buscarPartida(gameId));
    }

    @PostMapping("/{gameId}/mina")
    public PartidaMinadaResponse posicionarMina(@PathVariable String gameId, @RequestBody PosicionarMinaRequest request, Principal principal) {
        minadoService.posicionarMina(gameId,
                principal.getName(),
                request.getLinha(),
                request.getColuna());
        return new PartidaMinadaResponse(gameId, minadoService.buscarPartida(gameId));
    }

    @PostMapping("/{gameId}/pronto")
    public PartidaMinadaResponse pronto(@PathVariable String gameId, Principal principal) {
        PartidaMinada p = minadoService.marcarPronto(gameId, principal.getName());
        if (p.getStatus() == StatusPartida.EM_ANDAMENTO) {
            TiroMinadoResponse inicio = new TiroMinadoResponse(
                    null, -1, -1, null,
                    p.getTurnoAtual(), p.getStatus(), p.getVencedor(),
                    null);
            messagingTemplate.convertAndSend("/topic/minado/" + gameId, inicio);
        }
        return new PartidaMinadaResponse(gameId, p);
    }
}

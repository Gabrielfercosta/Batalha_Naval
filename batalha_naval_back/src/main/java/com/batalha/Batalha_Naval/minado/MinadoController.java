package com.batalha.Batalha_Naval.minado;

import com.batalha.Batalha_Naval.dominio.Direcao;
import com.batalha.Batalha_Naval.dto.SalaResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/minado")
public class MinadoController {

    private final MinadoService minadoService;
    private final SimpMessagingTemplate messagingTemplate;

    public MinadoController(MinadoService minadoService, SimpMessagingTemplate messagingTemplate) {
        this.minadoService = minadoService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/create")
    public PartidaMinadaResponse criar(@RequestBody Map<String, String> body) {
        String gameId = minadoService.criarPartida(body.get("jogador"), body.get("nome"), body.get("senha"));
        return new PartidaMinadaResponse(gameId, minadoService.buscarPartida(gameId));
    }

    @PostMapping("/{gameId}/join")
    public PartidaMinadaResponse entrar(@PathVariable String gameId, @RequestBody Map<String, String> body) {
        PartidaMinada p = minadoService.entrarNaPartida(gameId, body.get("jogador"), body.get("senha"));
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
    public void sair(@PathVariable String gameId, @RequestBody java.util.Map<String, String> body) {
        minadoService.sairDaPartida(gameId, body.get("jogador"));
    }

    @PostMapping("/{gameId}/navio")
    public PartidaMinadaResponse posicionarNavio(@PathVariable String gameId, @RequestBody Map<String, Object> body) {
        minadoService.posicionarNavio(gameId,
                (String) body.get("jogador"),
                ((Number) body.get("linha")).intValue(),
                ((Number) body.get("coluna")).intValue(),
                ((Number) body.get("tamanho")).intValue(),
                Direcao.valueOf((String) body.get("direcao")));
        return new PartidaMinadaResponse(gameId, minadoService.buscarPartida(gameId));
    }

    @PostMapping("/{gameId}/mina")
    public PartidaMinadaResponse posicionarMina(@PathVariable String gameId, @RequestBody Map<String, Object> body) {
        minadoService.posicionarMina(gameId,
                (String) body.get("jogador"),
                ((Number) body.get("linha")).intValue(),
                ((Number) body.get("coluna")).intValue());
        return new PartidaMinadaResponse(gameId, minadoService.buscarPartida(gameId));
    }

    @PostMapping("/{gameId}/pronto")
    public PartidaMinadaResponse pronto(@PathVariable String gameId, @RequestBody Map<String, String> body) {
        PartidaMinada p = minadoService.marcarPronto(gameId, body.get("jogador"));
        if (p.getStatus() == StatusPartidaMinada.EM_ANDAMENTO) {
            TiroMinadoResponse inicio = new TiroMinadoResponse(
                    null, -1, -1, null,
                    p.getTurnoAtual(), p.getStatus(), p.getVencedor(),
                    null);
            messagingTemplate.convertAndSend("/topic/minado/" + gameId, inicio);
        }
        return new PartidaMinadaResponse(gameId, p);
    }
}

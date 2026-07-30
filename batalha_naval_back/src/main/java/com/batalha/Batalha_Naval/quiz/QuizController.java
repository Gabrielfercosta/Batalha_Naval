package com.batalha.Batalha_Naval.quiz;

import com.batalha.Batalha_Naval.config.PartidasMetrics;
import com.batalha.Batalha_Naval.dto.CriarPartidaRequest;
import com.batalha.Batalha_Naval.dto.EntrarPartidaRequest;
import com.batalha.Batalha_Naval.dto.PosicionarNavioRequest;
import com.batalha.Batalha_Naval.dto.SalaResponse;
import io.micrometer.core.annotation.Timed;
import lombok.AllArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/quiz")
@AllArgsConstructor
@Timed(value = "quiz.controller", description = "Tempo dos endpoints de quiz")
public class QuizController {

    private final QuizService quizService;
    private final SimpMessagingTemplate messagingTemplate;
    private final PartidasMetrics partidasMetrics;

    @PostMapping("/create")
    public PartidaQuizResponse criar(@RequestBody CriarQuizRequest request, Principal principal) {
        String gameId = quizService.criarPartidaQuiz(
                principal.getName(), request.getNome(), request.getSenha(),
                request.getCategorias(), request.getDificuldade(), request.isModoRapido());
        partidasMetrics.partidaCriada("quiz");
        return new PartidaQuizResponse(gameId, quizService.buscarPartida(gameId));
    }

    @PostMapping("/{gameId}/join")
    public PartidaQuizResponse entrar(@PathVariable String gameId, @RequestBody EntrarPartidaRequest request, Principal principal) {
        PartidaQuiz partida = quizService.entrarNaPartida(gameId, principal.getName(), request.getSenha());
        return new PartidaQuizResponse(gameId, partida);
    }

    @GetMapping("/open")
    public List<SalaResponse> listarAbertas() {
        return quizService.listarPartidasAbertas();
    }

    @GetMapping("/{gameId}")
    public PartidaQuizResponse buscar(@PathVariable String gameId) {
        return new PartidaQuizResponse(gameId, quizService.buscarPartida(gameId));
    }

    @PostMapping("/{gameId}/posicionar")
    public PartidaQuizResponse posicionar(@PathVariable String gameId, @RequestBody PosicionarNavioRequest request, Principal principal) {
        quizService.posicionarNavio(
                gameId,
                principal.getName(),
                request.getTipo(),
                request.getLinha(),
                request.getColuna(),
                request.getDirecao()
        );
        return new PartidaQuizResponse(gameId, quizService.buscarPartida(gameId));
    }

    @PostMapping("/{gameId}/pronto")
    public PartidaQuizResponse pronto(@PathVariable String gameId, Principal principal) {
        PartidaQuiz partida = quizService.marcarPronto(gameId, principal.getName());
        return new PartidaQuizResponse(gameId, partida);
    }

    @PostMapping("/{gameId}/sair")
    public void sair(@PathVariable String gameId, Principal principal) {
        PartidaQuiz partida = quizService.sairDaPartida(gameId, principal.getName());
        if (partida != null) {
            TiroQuizResponse aviso = new TiroQuizResponse(
                    null, -1, -1, null, null, null,
                    partida.getStatus(), partida.getVencedor());
            messagingTemplate.convertAndSend("/topic/quiz/" + gameId, aviso);
        }
    }
}

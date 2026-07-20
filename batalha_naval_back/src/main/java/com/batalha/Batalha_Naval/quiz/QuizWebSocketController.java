package com.batalha.Batalha_Naval.quiz;

import com.batalha.Batalha_Naval.dto.ErroResponse;
import com.batalha.Batalha_Naval.dto.TiroRequest;
import lombok.AllArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@AllArgsConstructor
public class QuizWebSocketController {

    private final QuizService quizService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/quiz/{gameId}/cheguei")
    public void cheguei(@DestinationVariable String gameId, Principal principal) {
        try {
            quizService.jogadorChegou(gameId, principal.getName());
        } catch (Exception e) {
            enviarErro(gameId, principal.getName(), e);
        }
    }

    @MessageMapping("/quiz/{gameId}/responder")
    public void responder(@DestinationVariable String gameId, RespostaRequest request, Principal principal) {
        try {
            quizService.responder(gameId, principal.getName(), request.getResposta());
        } catch (Exception e) {
            enviarErro(gameId, principal.getName(), e);
        }
    }

    @MessageMapping("/quiz/{gameId}/tiro")
    public void atirar(@DestinationVariable String gameId, TiroRequest request, Principal principal) {
        try {
            quizService.atirar(gameId, principal.getName(), request.getLinha(), request.getColuna());
        } catch (Exception e) {
            enviarErro(gameId, principal.getName(), e);
        }
    }

    private void enviarErro(String gameId, String jogador, Exception e) {
        messagingTemplate.convertAndSend(
                "/topic/quiz/" + gameId + "/erro/" + jogador,
                new ErroResponse(e.getMessage()));
    }
}

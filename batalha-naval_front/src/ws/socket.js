import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

export function conectar(gameId, jogador, aoReceberTiro, aoReceberErro) {
    const token = localStorage.getItem('token');
    const client = new Client({
        webSocketFactory: () => new SockJS(`${import.meta.env.VITE_API_URL || 'http://localhost:8080'}/ws`),
        connectHeaders: { Authorization: `Bearer ${token}` },
        onConnect: () => {
            client.subscribe(`/topic/game/${gameId}`, (msg) => {
                aoReceberTiro(JSON.parse(msg.body));
            });

            client.subscribe(`/topic/game/${gameId}/erro/${jogador}`, (msg) => {
                aoReceberErro(JSON.parse(msg.body));
            });
        }
    });

    client.activate();
    return client;
}

export function atirar(client, gameId, jogador, linha, coluna) {
    client.publish({
        destination: `/app/game/${gameId}/tiro`,
        body: JSON.stringify({ jogador, linha, coluna })
    });
}

export function conectarMinado(gameId, jogador, aoReceberTiro, aoReceberErro, aoContagem) {
    const token = localStorage.getItem('token');
    const client = new Client({
        webSocketFactory: () => new SockJS(`${import.meta.env.VITE_API_URL || 'http://localhost:8080'}/ws`),
        connectHeaders: { Authorization: `Bearer ${token}` },
        onConnect: () => {
            client.subscribe(`/topic/minado/${gameId}`, (msg) => {
                aoReceberTiro(JSON.parse(msg.body));
            });
            client.subscribe(`/topic/minado/${gameId}/erro/${jogador}`, (msg) => {
                aoReceberErro(JSON.parse(msg.body));
            });
            client.subscribe(`/topic/minado/${gameId}/contagem`, (msg) => {
                aoContagem(JSON.parse(msg.body));
            });
            client.publish({
                destination: `/app/minado/${gameId}/cheguei`,
                body: JSON.stringify({ jogador })
            });
        }
    });
    client.activate();
    return client;
}

export function atirarMinado(client, gameId, jogador, linha, coluna) {
    client.publish({
        destination: `/app/minado/${gameId}/tiro`,
        body: JSON.stringify({ jogador, linha, coluna })
    });
}
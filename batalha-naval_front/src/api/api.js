const API_HOST = import.meta.env.VITE_API_URL || 'http://localhost:8080';
const BASE = `${API_HOST}/api/game`;
const AUTH_BASE = `${API_HOST}/api/auth`;
const MINADO_BASE = `${API_HOST}/api/minado`;
const QUIZ_BASE = `${API_HOST}/api/quiz`;

async function pedir(url, metodo = 'GET', corpo, comAuth = true) {
    const headers = { 'Content-Type': 'application/json' };
    if (comAuth) headers['Authorization'] = `Bearer ${localStorage.getItem('token')}`;

    const opcoes = { method: metodo, headers };
    if (corpo !== undefined) opcoes.body = JSON.stringify(corpo);

    const res = await fetch(url, opcoes);
    if (!res.ok) {
        const erro = await res.json().catch(() => ({}));
        throw new Error(erro.mensagem || 'Algo deu errado');
    }
    const texto = await res.text();
    return texto ? JSON.parse(texto) : null;
}

export function registrar(username, senha) {
    return pedir(`${AUTH_BASE}/register`, 'POST', { username, senha }, false);
}

export function logar(username, senha) {
    return pedir(`${AUTH_BASE}/login`, 'POST', { username, senha }, false);
}

export function criarPartida(jogador, nome, senha) {
    return pedir(`${BASE}/create`, 'POST', { jogador, nome, senha });
}

export function entrarPartida(gameId, jogador, senha) {
    return pedir(`${BASE}/${gameId}/join`, 'POST', { jogador, senha });
}

export function listarAbertas() {
    return pedir(`${BASE}/open`);
}

export function posicionarNavio(gameId, dados) {
    return pedir(`${BASE}/${gameId}/posicionar`, 'POST', dados);
}

export function marcarPronto(gameId, jogador) {
    return pedir(`${BASE}/${gameId}/pronto`, 'POST', { jogador });
}

export function buscarPartida(gameId) {
    return pedir(`${BASE}/${gameId}`);
}

export function sairDaPartida(gameId, jogador) {
    return pedir(`${BASE}/${gameId}/sair`, 'POST', { jogador });
}

export function sairDaPartidaMinada(gameId, jogador) {
    return pedir(`${MINADO_BASE}/${gameId}/sair`, 'POST', { jogador });
}

export function criarPartidaMinada(jogador, nome, senha) {
    return pedir(`${MINADO_BASE}/create`, 'POST', { jogador, nome, senha });
}

export function entrarPartidaMinada(gameId, jogador, senha) {
    return pedir(`${MINADO_BASE}/${gameId}/join`, 'POST', { jogador, senha });
}

export function listarAbertasMinada() {
    return pedir(`${MINADO_BASE}/open`);
}

export function posicionarNavioMinado(gameId, dados) {
    return pedir(`${MINADO_BASE}/${gameId}/navio`, 'POST', dados);
}

export function posicionarMinaMinado(gameId, dados) {
    return pedir(`${MINADO_BASE}/${gameId}/mina`, 'POST', dados);
}

export function marcarProntoMinado(gameId, jogador) {
    return pedir(`${MINADO_BASE}/${gameId}/pronto`, 'POST', { jogador });
}

export function buscarPartidaMinada(gameId) {
    return pedir(`${MINADO_BASE}/${gameId}`);
}

export function criarPartidaQuiz(jogador, nome, senha, categorias, dificuldade) {
    return pedir(`${QUIZ_BASE}/create`, 'POST', { jogador, nome, senha, categorias, dificuldade });
}

export function entrarPartidaQuiz(gameId, jogador, senha) {
    return pedir(`${QUIZ_BASE}/${gameId}/join`, 'POST', { jogador, senha });
}

export function listarAbertasQuiz() {
    return pedir(`${QUIZ_BASE}/open`);
}

export function posicionarNavioQuiz(gameId, dados) {
    return pedir(`${QUIZ_BASE}/${gameId}/posicionar`, 'POST', dados);
}

export function marcarProntoQuiz(gameId, jogador) {
    return pedir(`${QUIZ_BASE}/${gameId}/pronto`, 'POST', { jogador });
}

export function buscarPartidaQuiz(gameId) {
    return pedir(`${QUIZ_BASE}/${gameId}`);
}

export function sairDaPartidaQuiz(gameId, jogador) {
    return pedir(`${QUIZ_BASE}/${gameId}/sair`, 'POST', { jogador });
}
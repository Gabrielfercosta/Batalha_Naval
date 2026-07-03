const BASE = 'http://localhost:8080/api/game';
const AUTH_BASE = 'http://localhost:8080/api/auth';
const MINADO_BASE = 'http://localhost:8080/api/minado';


export async function registrar(username, senha) {
    const res = await fetch(`${AUTH_BASE}/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, senha })
    });
    if (!res.ok) {
        const erro = await res.json();
        throw new Error(erro.mensagem);
    }
    return res.json();
}

function authHeaders() {
    const token = localStorage.getItem('token');
    return {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
    };
}

export async function logar(username, senha) {
    const res = await fetch(`${AUTH_BASE}/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, senha })
    });
    if (!res.ok) {
        const erro = await res.json();
        throw new Error(erro.mensagem);
    }
    return res.json();
}


export async function criarPartida(jogador, nome, senha) {
    const res = await fetch(`${BASE}/create`, {
        method: 'POST',
        headers: authHeaders(),
        body: JSON.stringify({ jogador, nome, senha })
    });
    return res.json();
}

export async function entrarPartida(gameId, jogador, senha) {
    const res = await fetch(`${BASE}/${gameId}/join`, {
        method: 'POST',
        headers: authHeaders(),
        body: JSON.stringify({ jogador, senha })
    });
    if (!res.ok) {
        const erro = await res.json();
        throw new Error(erro.mensagem);
    }
    return res.json();
}

export async function listarAbertas() {
    const res = await fetch(`${BASE}/open`, {
        headers: authHeaders()
    });
    return res.json();
}

export async function posicionarNavio(gameId, dados) {
    const res = await fetch(`${BASE}/${gameId}/posicionar`, {
        method: 'POST',
        headers: authHeaders(),
        body: JSON.stringify(dados)
    });
    if (!res.ok) {
        const erro = await res.json();
        throw new Error(erro.mensagem);
    }
    return res.json();
}

export async function marcarPronto(gameId, jogador) {
    const res = await fetch(`${BASE}/${gameId}/pronto`, {
        method: 'POST',
        headers: authHeaders(),
        body: JSON.stringify({ jogador })
    });
    if (!res.ok) {
        const erro = await res.json();
        throw new Error(erro.mensagem);
    }
    return res.json();
}

export async function buscarPartida(gameId) {
    const res = await fetch(`${BASE}/${gameId}`, {
        headers: authHeaders()
    });
    return res.json();
}

export async function sairDaPartida(gameId, jogador) {
    await fetch(`${BASE}/${gameId}/sair`, {
        method: 'POST',
        headers: authHeaders(),
        body: JSON.stringify({ jogador })
    });
}

export async function sairDaPartidaMinada(gameId, jogador) {
    await fetch(`${MINADO_BASE}/${gameId}/sair`, {
        method: 'POST',
        headers: authHeaders(),
        body: JSON.stringify({ jogador })
    });
}

export async function criarPartidaMinada(jogador, nome, senha) {
    const res = await fetch(`${MINADO_BASE}/create`, {
        method: 'POST',
        headers: authHeaders(),
        body: JSON.stringify({ jogador, nome, senha })
    });
    return res.json();
}

export async function entrarPartidaMinada(gameId, jogador, senha) {
    const res = await fetch(`${MINADO_BASE}/${gameId}/join`, {
        method: 'POST',
        headers: authHeaders(),
        body: JSON.stringify({ jogador, senha })
    });
    if (!res.ok) {
        const erro = await res.json();
        throw new Error(erro.mensagem);
    }
    return res.json();
}

export async function listarAbertasMinada() {
    const res = await fetch(`${MINADO_BASE}/open`, { headers: authHeaders() });
    return res.json();
}

export async function posicionarNavioMinado(gameId, dados) {
    const res = await fetch(`${MINADO_BASE}/${gameId}/navio`, {
        method: 'POST',
        headers: authHeaders(),
        body: JSON.stringify(dados)
    });
    if (!res.ok) {
        const erro = await res.json();
        throw new Error(erro.mensagem);
    }
    return res.json();
}

export async function posicionarMinaMinado(gameId, dados) {
    const res = await fetch(`${MINADO_BASE}/${gameId}/mina`, {
        method: 'POST',
        headers: authHeaders(),
        body: JSON.stringify(dados)
    });
    if (!res.ok) {
        const erro = await res.json();
        throw new Error(erro.mensagem);
    }
    return res.json();
}

export async function marcarProntoMinado(gameId, jogador) {
    const res = await fetch(`${MINADO_BASE}/${gameId}/pronto`, {
        method: 'POST',
        headers: authHeaders(),
        body: JSON.stringify({ jogador })
    });
    if (!res.ok) {
        const erro = await res.json();
        throw new Error(erro.mensagem);
    }
    return res.json();
}

export async function buscarPartidaMinada(gameId) {
    const res = await fetch(`${MINADO_BASE}/${gameId}`, { headers: authHeaders() });
    return res.json();
}


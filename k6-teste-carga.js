import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Métricas customizadas
const erros = new Rate('erros');
const tempoLogin = new Trend('tempo_login', true);
const tempoListarSalas = new Trend('tempo_listar_salas', true);
const tempoCriarPartida = new Trend('tempo_criar_partida', true);

// Configuração do teste
export const options = {
    stages: [
        { duration: '10s', target: 10 },  // Ramp-up: 0 → 10 usuários em 10s
        { duration: '30s', target: 10 },  // Sustenta 10 usuários por 30s
        { duration: '10s', target: 30 },  // Sobe para 30 usuários
        { duration: '30s', target: 30 },  // Sustenta 30 usuários por 30s
        { duration: '10s', target: 0 },   // Ramp-down
    ],
    thresholds: {
        http_req_duration: ['p(95)<500'],      // 95% das requisições < 500ms
        erros: ['rate<0.15'],                   // Menos de 15% de erros reais
        tempo_listar_salas: ['p(95)<100'],      // Listar salas < 100ms no p95
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// Cada usuário virtual executa este fluxo
export default function () {
    const usuario = `k6user_${__VU}_${__ITER}`;
    const senha = 'senha123';

    group('1. Registro/Login', () => {
        // Tenta registrar; se já existe, faz login
        let res = http.post(`${BASE_URL}/api/auth/register`, JSON.stringify({
            username: usuario,
            senha: senha,
        }), { headers: { 'Content-Type': 'application/json' } });

        if (res.status === 400) {
            res = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({
                username: usuario,
                senha: senha,
            }), { headers: { 'Content-Type': 'application/json' } });
        }

        tempoLogin.add(res.timings.duration);
        const sucesso = check(res, {
            'login/registro com sucesso': (r) => r.status === 200 || r.status === 429,
            'retornou token': (r) => {
                if (r.status === 429) return true; // Rate limited é esperado
                try { return JSON.parse(r.body).token !== undefined; } catch(e) { return false; }
            },
        });
        erros.add(res.status !== 200 && res.status !== 429);

        if (res.status === 429 || res.status !== 200) return;

        const token = JSON.parse(res.body).token;
        const authHeaders = {
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`,
            },
        };

        group('2. Listar salas abertas', () => {
            const r1 = http.get(`${BASE_URL}/api/game/open`, authHeaders);
            tempoListarSalas.add(r1.timings.duration);
            const ok = check(r1, { 'listou salas': (r) => r.status === 200 });
            erros.add(!ok);

            const r2 = http.get(`${BASE_URL}/api/quiz/open`, authHeaders);
            check(r2, { 'listou salas quiz': (r) => r.status === 200 });

            const r3 = http.get(`${BASE_URL}/api/minado/open`, authHeaders);
            check(r3, { 'listou salas minado': (r) => r.status === 200 });
        });

        group('3. Criar partidas (3 modos)', () => {
            const criarClassico = http.post(`${BASE_URL}/api/game/create`, JSON.stringify({
                nome: `Sala K6 ${__VU}`,
                senha: '',
            }), authHeaders);
            tempoCriarPartida.add(criarClassico.timings.duration);
            check(criarClassico, { 'partida classica criada': (r) => r.status === 200 || r.status === 429 });

            const criarMinado = http.post(`${BASE_URL}/api/minado/create`, JSON.stringify({
                nome: `Minada K6 ${__VU}`,
                senha: '',
            }), authHeaders);
            check(criarMinado, { 'partida minada criada': (r) => r.status === 200 || r.status === 429 });

            const criarQuiz = http.post(`${BASE_URL}/api/quiz/create`, JSON.stringify({
                nome: `Quiz K6 ${__VU}`,
                senha: '',
                categorias: ['geral'],
                dificuldade: '',
                modoRapido: false,
            }), authHeaders);
            check(criarQuiz, { 'partida quiz criada': (r) => r.status === 200 || r.status === 429 });

            // Mantém partidas ativas por 5s para o Prometheus capturar
            sleep(5);

            // Sai das partidas
            if (criarClassico.status === 200) {
                try { http.post(`${BASE_URL}/api/game/${JSON.parse(criarClassico.body).gameId}/sair`, null, authHeaders); } catch(e) {}
            }
            if (criarMinado.status === 200) {
                try { http.post(`${BASE_URL}/api/minado/${JSON.parse(criarMinado.body).gameId}/sair`, null, authHeaders); } catch(e) {}
            }
            if (criarQuiz.status === 200) {
                try { http.post(`${BASE_URL}/api/quiz/${JSON.parse(criarQuiz.body).gameId}/sair`, null, authHeaders); } catch(e) {}
            }
        });

        group('4. Health check', () => {
            const r = http.get(`${BASE_URL}/actuator/health`);
            check(r, { 'backend saudavel': (r) => r.status === 200 });
        });
    });

    sleep(1); // Pausa de 1s entre iterações (simula comportamento real)
}

// Resumo no final do teste
export function handleSummary(data) {
    const m = data.metrics;
    const dur = m.http_req_duration ? m.http_req_duration.values : {};
    const login = m.tempo_login ? m.tempo_login.values : {};
    const salas = m.tempo_listar_salas ? m.tempo_listar_salas.values : {};
    const criar = m.tempo_criar_partida ? m.tempo_criar_partida.values : {};
    const errRate = m.erros ? m.erros.values.rate : 0;

    const fmt = (v) => v !== undefined && !isNaN(v) ? Math.round(v) : '-';

    const linhas = [
        '╔══════════════════════════════════════════════════════╗',
        '║     RESULTADO DO TESTE DE CARGA - BATALHA NAVAL     ║',
        '╚══════════════════════════════════════════════════════╝',
        '',
        `Total de requisições: ${m.http_reqs ? m.http_reqs.values.count : 0}`,
        `Duração total: ${Math.round(data.state.testRunDurationMs / 1000)}s`,
        `Usuários virtuais (máx): ${m.vus_max ? m.vus_max.values.max : 0}`,
        '',
        '── Tempo de resposta (todas as requisições) ──',
        `  Média:  ${fmt(dur.avg)}ms`,
        `  p50:    ${fmt(dur['p(50)'])}ms`,
        `  p90:    ${fmt(dur['p(90)'])}ms`,
        `  p95:    ${fmt(dur['p(95)'])}ms`,
        `  p99:    ${fmt(dur['p(99)'])}ms`,
        `  Máx:    ${fmt(dur.max)}ms`,
        '',
        '── Tempo por operação ──',
        `  Login/Registro:  média ${fmt(login.avg)}ms | p95 ${fmt(login['p(95)'])}ms`,
        `  Listar salas:    média ${fmt(salas.avg)}ms | p95 ${fmt(salas['p(95)'])}ms`,
        `  Criar partida:   média ${fmt(criar.avg)}ms | p95 ${fmt(criar['p(95)'])}ms`,
        '',
        '── Taxa de erros (excluindo rate limiting) ──',
        `  ${(errRate * 100).toFixed(2)}%`,
        '',
        '── Thresholds ──',
    ];

    for (const [nome, th] of Object.entries(m)) {
        if (th.thresholds) {
            for (const [cond, resultado] of Object.entries(th.thresholds)) {
                const status = resultado.ok ? '✅ PASS' : '❌ FAIL';
                linhas.push(`  ${status}  ${nome}: ${cond}`);
            }
        }
    }

    linhas.push('');
    const texto = linhas.join('\n');
    console.log(texto);

    return {
        'stdout': texto,
        'resultados-carga.txt': texto,
    };
}

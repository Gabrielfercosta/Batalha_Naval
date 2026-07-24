import { useState, useEffect } from 'react';
import {
    criarPartida, listarAbertas, entrarPartida,
    criarPartidaMinada, listarAbertasMinada, entrarPartidaMinada,
    criarPartidaQuiz, listarAbertasQuiz, entrarPartidaQuiz
} from '../api/api';

const TUTORIAIS = {
    classico: {
        titulo: 'Batalha Naval Classico',
        passos: [
            'Objetivo: afundar todos os 5 navios inimigos antes que o adversario afunde os seus.',
            'Voce possui 5 navios: Porta-Avioes (5 casas), Encouracado (4), Cruzador (3), Submarino (3) e Destroyer (2). Posicione cada um no tabuleiro 10x10 escolhendo a direcao horizontal ou vertical. Pressione a tecla R para alternar a direcao antes de posicionar.',
            'O jogo funciona por turnos alternados. Na sua vez, clique em uma casa do Mar Inimigo para disparar.',
            'Se o tiro acertar um navio, voce ganha outro tiro imediatamente. Se cair na agua, a vez passa para o adversario.',
            'Quando todas as casas de um navio sao atingidas, ele e marcado como AFUNDADO e seus destrocos aparecem no tabuleiro.',
            'Vence o jogador que afundar toda a frota inimiga primeiro.'
        ]
    },
    minada: {
        titulo: 'Batalha Minada',
        passos: [
            'Objetivo: encontrar e acertar todos os navios inimigos antes do adversario, sem atingir nenhuma mina.',
            'O tabuleiro e maior: 16x16. Voce posiciona os mesmos 5 navios e tambem 20 minas em posicoes estrategicas para dificultar a busca do adversario.',
            'Diferente do modo classico, ambos os jogadores jogam ao mesmo tempo. E uma corrida: nao ha turnos alternados.',
            'Ao clicar em uma casa do Mar Inimigo, se for agua, a casa revela pistas numericas indicando quantas minas e quantos navios existem nas casas vizinhas. Use essas informacoes para deduzir onde estao os navios.',
            'Voce pode marcar casas suspeitas com bandeiras clicando com o botao direito do mouse, para se organizar melhor.',
            'Se voce atingir uma mina, perde a partida imediatamente. Se encontrar todos os navios inimigos sem pisar em minas, voce vence.',
            'Antes da partida comecar, ha uma contagem regressiva para garantir que ambos os jogadores estejam prontos.'
        ]
    },
    quiz: {
        titulo: 'Batalha Naval Quiz',
        passos: [
            'Objetivo: afundar a frota inimiga conquistando tiros atraves de perguntas de conhecimento.',
            'Posicione seus 5 navios no tabuleiro 10x10, da mesma forma que no modo classico (horizontal ou vertical, tecla R para girar).',
            'A partida funciona em rodadas. Cada rodada apresenta perguntas que ambos os jogadores respondem ao mesmo tempo, com um tempo limite para cada resposta.',
            'Apos cada pergunta, o resultado e exibido mostrando quem acertou e quem errou, junto com a resposta correta.',
            'Cada acerto vale tiros: perguntas faceis valem 1 tiro, medias valem 2 e dificeis valem 3. No modo rapido, esses valores sao dobrados.',
            'Ao final da rodada de perguntas, quem acertou mais dispara primeiro. Se ambos acertaram igual, os tiros sao distribuidos alternadamente.',
            'Ao criar a sala, voce pode escolher as categorias das perguntas (Geral, Livros, Filmes, Musicas, Teatro, TV, Videogame) e o nivel de dificuldade.'
        ]
    }
};

const CATEGORIAS = [
    { id: 'geral', nome: 'Geral' },
    { id: 'livros', nome: 'Livros' },
    { id: 'filmes', nome: 'Filmes' },
    { id: 'musicas', nome: 'Músicas' },
    { id: 'teatro', nome: 'Teatro' },
    { id: 'tv', nome: 'TV' },
    { id: 'videogame', nome: 'Videogame' },
    { id: 'tabuleiro', nome: 'Tabuleiro' },
    { id: 'ciencia', nome: 'Ciência' },
    { id: 'computadores', nome: 'Computadores' },
    { id: 'matematica', nome: 'Matemática' },
    { id: 'mitologia', nome: 'Mitologia' },
    { id: 'esportes', nome: 'Esportes' },
    { id: 'geografia', nome: 'Geografia' },
    { id: 'historia', nome: 'História' },
    { id: 'politica', nome: 'Política' },
    { id: 'arte', nome: 'Arte' },
    { id: 'celebridades', nome: 'Celebridades' },
    { id: 'animais', nome: 'Animais' },
    { id: 'veiculos', nome: 'Veículos' },
    { id: 'quadrinhos', nome: 'Quadrinhos' },
    { id: 'dispositivos', nome: 'Dispositivos' },
    { id: 'anime', nome: 'Anime' },
    { id: 'cartoons', nome: 'Cartoons' }
];

const DIFICULDADES = [
    { v: 'todas', nome: 'Todas' },
    { v: 'easy', nome: 'Fácil' },
    { v: 'medium', nome: 'Médio' },
    { v: 'hard', nome: 'Difícil' }
];

function Lobby({ jogador, aoIniciarPartida, aoIniciarMinada, aoIniciarQuiz }) {
    const [salas, setSalas] = useState([]);
    const [nome, setNome] = useState('');
    const [senha, setSenha] = useState('');
    const [modo, setModo] = useState('classico');
    const [mensagem, setMensagem] = useState('');
    const [ajuda, setAjuda] = useState(null);
    const [salaComSenha, setSalaComSenha] = useState(null);
    const [senhaDigitada, setSenhaDigitada] = useState('');
    const [categoriasAtivas, setCategoriasAtivas] = useState(() => new Set(CATEGORIAS.map((c) => c.id)));
    const [dificuldade, setDificuldade] = useState('todas');
    const [modoRapido, setModoRapido] = useState(false);
    const [criando, setCriando] = useState(false);

    function toggleCategoria(id) {
        setCategoriasAtivas((prev) => {
            const nova = new Set(prev);
            if (nova.has(id)) nova.delete(id);
            else nova.add(id);
            return nova;
        });
    }

    const todasMarcadas = categoriasAtivas.size === CATEGORIAS.length;

    function alternarTodasCategorias() {
        setCategoriasAtivas(todasMarcadas ? new Set() : new Set(CATEGORIAS.map((c) => c.id)));
    }

    const apiPorModo = {
        classico: { criar: criarPartida, entrar: entrarPartida, iniciar: aoIniciarPartida },
        minada: { criar: criarPartidaMinada, entrar: entrarPartidaMinada, iniciar: aoIniciarMinada },
        quiz: { criar: criarPartidaQuiz, entrar: entrarPartidaQuiz, iniciar: aoIniciarQuiz }
    };

    const iconePorModo = { classico: '🚢', minada: '💣', quiz: '🧠' };

    async function carregarSalas() {
        const [classicas, minadas, quizes] = await Promise.all([
            listarAbertas(),
            listarAbertasMinada(),
            listarAbertasQuiz()
        ]);
        setSalas([
            ...classicas.map((s) => ({ ...s, modo: 'classico' })),
            ...minadas.map((s) => ({ ...s, modo: 'minada' })),
            ...quizes.map((s) => ({ ...s, modo: 'quiz' }))
        ]);
    }

    useEffect(() => {
        carregarSalas();
        const intervalo = setInterval(carregarSalas, 5000);
        return () => clearInterval(intervalo);
    }, []);

    async function criar() {
        if (criando) return;
        if (nome.trim() === '') { setMensagem('Dê um nome pra sala.'); return; }
        if (modo === 'quiz' && categoriasAtivas.size === 0) { setMensagem('Escolha ao menos 1 categoria.'); return; }
        setCriando(true);
        try {
            if (modo === 'quiz') {
                const cats = Array.from(categoriasAtivas);
                const p = await criarPartidaQuiz(jogador, nome, senha, cats, dificuldade === 'todas' ? '' : dificuldade, modoRapido);
                aoIniciarQuiz(p.gameId);
            } else {
                const api = apiPorModo[modo];
                const p = await api.criar(jogador, nome, senha);
                api.iniciar(p.gameId);
            }
        } catch (e) {
            setMensagem(e.message);
            setCriando(false);
        }
    }

    async function entrar(sala) {
        if (sala.temSenha) {
            setSalaComSenha(sala);
            setSenhaDigitada('');
            return;
        }
        try {
            const api = apiPorModo[sala.modo];
            await api.entrar(sala.gameId, jogador, '');
            api.iniciar(sala.gameId);
        } catch (e) { setMensagem(e.message); }
    }

    async function confirmarSenha() {
        if (!salaComSenha) return;
        try {
            const api = apiPorModo[salaComSenha.modo];
            await api.entrar(salaComSenha.gameId, jogador, senhaDigitada);
            api.iniciar(salaComSenha.gameId);
            setSalaComSenha(null);
        } catch (e) {
            setMensagem(e.message);
            setSalaComSenha(null);
        }
    }

    return (
        <div className="painel" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 12, maxWidth: 520 }}>
            <h2>Lobby</h2>

            <div style={{ display: 'flex', gap: 8, alignItems: 'center', justifyContent: 'center', flexWrap: 'wrap' }}>
                <button onClick={() => setModo('classico')} style={{ position: 'relative', opacity: modo === 'classico' ? 1 : 0.5 }}>
                    🚢 Clássico
                    <span className="badge-ajuda" onClick={(e) => { e.stopPropagation(); setAjuda('classico'); }}>?</span>
                </button>
                <button onClick={() => setModo('minada')} style={{ position: 'relative', opacity: modo === 'minada' ? 1 : 0.5 }}>
                    💣 Minada
                    <span className="badge-ajuda" onClick={(e) => { e.stopPropagation(); setAjuda('minada'); }}>?</span>
                </button>
                <button onClick={() => setModo('quiz')} style={{ position: 'relative', opacity: modo === 'quiz' ? 1 : 0.5 }}>
                    🧠 Quiz
                    <span className="badge-ajuda" onClick={(e) => { e.stopPropagation(); setAjuda('quiz'); }}>?</span>
                </button>
            </div>

            <input value={nome} onChange={(e) => setNome(e.target.value)} placeholder="Nome da sala" />
            <input value={senha} onChange={(e) => setSenha(e.target.value)} placeholder="Senha (opcional)" />

            {modo === 'quiz' && (
                <div style={{ width: '100%' }}>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8, flexWrap: 'wrap', margin: '4px 0' }}>
                        <p style={{ margin: 0, fontSize: 14 }}>Categorias <span style={{ opacity: 0.6, fontSize: 12 }}>(clique pra desativar)</span></p>
                        <button type="button" onClick={alternarTodasCategorias} style={{ margin: 0, padding: '4px 10px', fontSize: 12 }}>
                            {todasMarcadas ? 'Desmarcar todas' : 'Marcar todas'}
                        </button>
                    </div>
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, justifyContent: 'center' }}>
                        {CATEGORIAS.map((c) => (
                            <button key={c.id} onClick={() => toggleCategoria(c.id)} style={{ fontSize: 12, padding: '4px 8px', margin: 0, opacity: categoriasAtivas.has(c.id) ? 1 : 0.35 }}>
                                {c.nome}
                            </button>
                        ))}
                    </div>
                    <p style={{ margin: '10px 0 4px', fontSize: 14 }}>Dificuldade</p>
                    <div style={{ display: 'flex', gap: 6, justifyContent: 'center', flexWrap: 'wrap' }}>
                        {DIFICULDADES.map((d) => (
                            <button key={d.v} onClick={() => setDificuldade(d.v)} style={{ opacity: dificuldade === d.v ? 1 : 0.4 }}>
                                {d.nome}
                            </button>
                        ))}
                    </div>
                    <label style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 8, cursor: 'pointer', fontSize: 14 }}>
                        <input type="checkbox" checked={modoRapido} onChange={() => setModoRapido(!modoRapido)} />
                        ⚡ Modo Rápido <span style={{ opacity: 0.6, fontSize: 12 }}>(dobro de tiros)</span>
                    </label>
                </div>
            )}

            <div>
                <button onClick={criar} disabled={criando}>{criando ? 'Criando...' : 'Criar sala'}</button>
                <button onClick={carregarSalas}>Atualizar</button>
            </div>

            {mensagem && <p style={{ color: 'var(--perigo)' }}>{mensagem}</p>}

            <h3 style={{ fontSize: 20 }}>Salas abertas</h3>
            {salas.length === 0 && <p>Nenhuma sala aberta.</p>}
            <ul className="lista-salas">
                {salas.map((sala) => (
                    <li key={sala.gameId} className="sala-item">
                        <span>{iconePorModo[sala.modo]} {sala.temSenha ? '🔒 ' : ''}{sala.nome} <span style={{ fontSize: 12, opacity: 0.7 }}>({sala.criador} · {sala.jogadores}/2)</span></span>
                        <button onClick={() => entrar(sala)}>Entrar</button>
                    </li>
                ))}
            </ul>
            {ajuda && (
                <div className="modal-fundo" onClick={() => setAjuda(null)}>
                    <div className="modal" onClick={(e) => e.stopPropagation()}>
                        <button className="modal-fechar" onClick={() => setAjuda(null)}>✕</button>
                        <h2>{TUTORIAIS[ajuda].titulo}</h2>
                        <ul className="modal-lista">
                            {TUTORIAIS[ajuda].passos.map((passo, i) => <li key={i}>{passo}</li>)}
                        </ul>
                    </div>
                </div>
            )}
            {salaComSenha && (
                <div className="modal-fundo" onClick={() => setSalaComSenha(null)}>
                    <div className="modal" onClick={(e) => e.stopPropagation()} style={{ textAlign: 'center' }}>
                        <button className="modal-fechar" onClick={() => setSalaComSenha(null)}>✕</button>
                        <h2 style={{ margin: '0 0 4px' }}>🔒 Sala Protegida</h2>
                        <p style={{ margin: '0 0 14px', fontSize: 15 }}>Digite a senha para entrar em <strong>{salaComSenha.nome}</strong></p>
                        <input
                            type="password"
                            value={senhaDigitada}
                            onChange={(e) => setSenhaDigitada(e.target.value)}
                            onKeyDown={(e) => e.key === 'Enter' && confirmarSenha()}
                            placeholder="Senha da sala"
                            autoFocus
                            style={{ width: '100%', maxWidth: 260 }}
                        />
                        <div style={{ marginTop: 14 }}>
                            <button onClick={confirmarSenha}>Entrar</button>
                            <button onClick={() => setSalaComSenha(null)} style={{ opacity: 0.7 }}>Cancelar</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

export default Lobby;

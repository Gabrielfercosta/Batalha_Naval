import { useState, useEffect, useRef } from 'react';
import { conectarQuiz, responderQuiz, atirarQuiz } from '../ws/socket';
import { buscarPartidaQuiz } from '../api/api';
import { tocarSom, tocarMusica } from '../audio/musica';
import { estiloNavio as estiloNavioBase, SPRITES_POR_TIPO as SPRITES } from '../utils/navios';

function BatalhaQuiz({ jogador, gameId, meusNavios, voltarLobby }) {
    const [client, setClient] = useState(null);
    const [meusTiros, setMeusTiros] = useState({});
    const [tirosInimigos, setTirosInimigos] = useState({});
    const [status, setStatus] = useState('AGUARDANDO');
    const [vencedor, setVencedor] = useState(null);
    const [mensagem, setMensagem] = useState('');
    const [contagem, setContagem] = useState(null);
    const [pergunta, setPergunta] = useState(null);
    const [respondi, setRespondi] = useState(false);
    const [escolhida, setEscolhida] = useState(null);
    const [resultado, setResultado] = useState(null);
    const [placar, setPlacar] = useState(null);
    const [proximoAtirador, setProximoAtirador] = useState(null);
    const [segundos, setSegundos] = useState(null);
    const [naviosInimigos, setNaviosInimigos] = useState([]);
    const finalizadoRef = useRef(false);

    useEffect(() => {
        const c = conectarQuiz(
            gameId,
            jogador,
            (ev) => {
                if (finalizadoRef.current) return;
                if (ev.tipo === 'CONTAGEM') {
                    setContagem(ev.segundos);
                    setPergunta(null);
                    setResultado(null);
                    setPlacar(null);
                    setProximoAtirador(null);
                    setSegundos(null);
                    setMensagem('');
                    setStatus('EM_ANDAMENTO');
                } else if (ev.tipo === 'PERGUNTA') {
                    setPergunta({ texto: ev.pergunta, opcoes: ev.opcoes, indice: ev.indice, total: ev.total, dificuldade: ev.dificuldade, modoRapido: ev.modoRapido });
                    setResultado(null);
                    setPlacar(null);
                    setRespondi(false);
                    setEscolhida(null);
                    setSegundos(ev.segundos);
                    setContagem(null);
                    setMensagem('');
                    setStatus('EM_ANDAMENTO');
                } else if (ev.tipo === 'RESULTADO') {
                    setResultado({ respostaCorreta: ev.respostaCorreta, acertos: ev.acertos });
                    setSegundos(null);
                } else if (ev.tipo === 'PLACAR') {
                    setPlacar({ acertos: ev.acertos });
                    setProximoAtirador(ev.proximoAtirador);
                    setPergunta(null);
                    setResultado(null);
                } else if (ev.tipo === 'TIRO') {
                    if (ev.autor) {
                        const chave = `${ev.linha}-${ev.coluna}`;
                        if (ev.autor === jogador) {
                            setMeusTiros((a) => ({ ...a, [chave]: ev.resultado }));
                        } else {
                            setTirosInimigos((a) => ({ ...a, [chave]: ev.resultado }));
                        }
                        if (ev.resultado === 'ACERTO' || ev.resultado === 'AFUNDADO') {
                            tocarSom('explosao');
                        }
                    }
                    setProximoAtirador(ev.proximoAtirador);
                    if (ev.status === 'FINALIZADA') finalizadoRef.current = true;
                    setStatus(ev.status);
                    setVencedor(ev.vencedor);
                } else if (ev.tipo === 'ERRO') {
                    setMensagem(ev.mensagem);
                }
            },
            (erro) => setMensagem(erro.mensagem)
        );
        setClient(c);
        return () => { c.deactivate(); };
    }, [gameId, jogador]);

    useEffect(() => {
        if (status === 'FINALIZADA') {
            tocarMusica(vencedor === jogador ? 'vitoria' : 'derrota', false);
        }
    }, [status, vencedor]);

    useEffect(() => {
        buscarPartidaQuiz(gameId).then((p) => setStatus(p.status));
    }, [gameId]);

    useEffect(() => {
        if (status !== 'FINALIZADA') return;
        buscarPartidaQuiz(gameId).then((p) => {
            const souJogador1 = jogador === p.jogador1;
            setNaviosInimigos((souJogador1 ? p.navios2 : p.navios1) || []);
        });
    }, [status]);

    useEffect(() => {
        if (contagem === null) return;
        if (contagem <= 0) { setContagem(null); return; }
        const t = setTimeout(() => setContagem((c) => c - 1), 1000);
        return () => clearTimeout(t);
    }, [contagem]);

    useEffect(() => {
        if (segundos === null || segundos <= 0 || resultado) return;
        const t = setTimeout(() => setSegundos((s) => s - 1), 1000);
        return () => clearTimeout(t);
    }, [segundos, resultado]);

    const podeAtirar = status === 'EM_ANDAMENTO' && proximoAtirador === jogador;

    function responder(opcao) {
        if (respondi) return;
        setRespondi(true);
        setEscolhida(opcao);
        responderQuiz(client, gameId, opcao);
    }

    function clicarInimigo(linha, coluna) {
        if (!podeAtirar) return;
        const chave = `${linha}-${coluna}`;
        if (meusTiros[chave]) return;
        setProximoAtirador(null);
        atirarQuiz(client, gameId, jogador, linha, coluna);
    }

    function classeCelula(res) {
        if (res === 'AFUNDADO') return 'celula fogo afundado';
        if (res === 'ACERTO') return 'celula fogo';
        if (res === 'AGUA') return 'celula agua-tiro';
        return 'celula';
    }

    function montarTabuleiro(tiros, clicavel) {
        const linhas = [];
        for (let l = 0; l < 10; l++) {
            const celulas = [];
            for (let c = 0; c < 10; c++) {
                const chave = `${l}-${c}`;
                const classe = classeCelula(tiros[chave]) + (clicavel && podeAtirar ? ' clicavel' : '');
                celulas.push(
                    <td key={chave} className={classe} onClick={clicavel ? () => clicarInimigo(l, c) : undefined} />
                );
            }
            linhas.push(<tr key={l}>{celulas}</tr>);
        }
        return <table className="tabuleiro"><tbody>{linhas}</tbody></table>;
    }

    function estiloNavio(tamanho, linha, coluna, dir) {
        return estiloNavioBase(tamanho, linha, coluna, dir, (n) => `calc(var(--celula) * ${n})`);
    }

    function nomeOponente(mapa) {
        return Object.keys(mapa).find((n) => n !== jogador);
    }

    function painel() {
        if (status === 'FINALIZADA') {
            return (
                <>
                    <h2 style={{ margin: 0 }}>{vencedor === jogador ? 'Você venceu! 🏆' : 'Você perdeu! 💀'}</h2>
                    <button onClick={voltarLobby} style={{ marginTop: 10 }}>Voltar ao Lobby</button>
                </>
            );
        }
        if (resultado) {
            const oponente = nomeOponente(resultado.acertos);
            return (
                <>
                    <h3 style={{ margin: 0 }}>Resposta certa: <b>{resultado.respostaCorreta}</b></h3>
                    <p style={{ margin: '6px 0 0' }}>
                        Você: {resultado.acertos[jogador] ? '✓' : '✗'} &nbsp;|&nbsp; {oponente || 'Oponente'}: {resultado.acertos[oponente] ? '✓' : '✗'}
                    </p>
                </>
            );
        }
        if (pergunta) {
            const dificil = pergunta.dificuldade === 'hard';
            const base = dificil ? 3 : pergunta.dificuldade === 'medium' ? 2 : 1;
            const valorTiro = pergunta.modoRapido ? base * 2 : base;
            const nomeDif = dificil ? '·· Difícil' : pergunta.dificuldade === 'medium' ? '· Médio' : '🌟 Fácil';
            const plural = valorTiro > 1 ? 'tiros' : 'tiro';
            return (
                <>
                    <div style={{ display: 'flex', gap: 10, alignItems: 'center', justifyContent: 'center', flexWrap: 'wrap' }}>
                        <span style={{ fontSize: 15, fontWeight: 700 }}>Pergunta {pergunta.indice}/{pergunta.total}</span>
                        <span style={{
                            fontSize: 'clamp(14px, 2.2vw, 18px)', fontWeight: 800, color: '#ffffff',
                            background: dificil ? '#c0392b' : '#1c7cbd', padding: '5px 14px', borderRadius: 999,
                            boxShadow: '0 2px 6px rgba(0,0,0,0.35)'
                        }}>
                            {nomeDif} · vale {valorTiro} {plural}
                        </span>
                    </div>
                    <div style={{ fontSize: 22, fontWeight: 800 }}>⏱️ {segundos}s</div>
                    <h3 style={{ margin: '6px 0', fontSize: 'clamp(14px, 2.5vw, 20px)' }}>{pergunta.texto}</h3>
                    <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', justifyContent: 'center' }}>
                        {pergunta.opcoes.map((op) => (
                            <button key={op} onClick={() => responder(op)} disabled={respondi} style={{ opacity: respondi && escolhida !== op ? 0.5 : 1, fontWeight: escolhida === op ? 800 : 400, fontSize: 'clamp(12px, 2vw, 16px)', padding: '8px 14px' }}>
                            {op}
                            </button>
                        ))}
                    </div>
                    {respondi && <p style={{ margin: '8px 0 0', color: '#ffffff', fontWeight: 600, textShadow: '0 1px 4px rgba(0,0,0,0.7)' }}>Respondeu! Aguardando o adversário...</p>}
                </>
            );
        }
        if (placar) {
            const oponente = nomeOponente(placar.acertos);
            return (
                <>
                    <h3 style={{ margin: 0 }}>Fim das perguntas! 🎯</h3>
                    <p style={{ margin: '6px 0 0' }}>
                        Você: <b>{placar.acertos[jogador] || 0}</b> acertos &nbsp;|&nbsp; {oponente || 'Oponente'}: <b>{placar.acertos[oponente] || 0}</b>
                    </p>
                    <p style={{ margin: '4px 0 0', color: '#ffffff', fontWeight: 700, textShadow: '0 1px 4px rgba(0,0,0,0.7)' }}>
                    {podeAtirar
                            ? '🎯 Sua vez de atirar! Clique no Mar Inimigo.'
                            : (proximoAtirador ? `${proximoAtirador} está atirando...` : 'Ninguém acertou. Próxima rodada...')}
                    </p>
                </>
            );
        }
        return <h3 style={{ margin: 0 }}>Esperando o outro jogador...</h3>;
    }

    return (
        <>
            {contagem !== null && (
                <div className="contagem-regressiva">
                    <span className="contagem-numero" key={contagem}>{contagem}</span>
                </div>
            )}
            <div className="painel painel-batalha" style={{ maxWidth: '90vw', width: 560, fontSize: 'clamp(13px, 2vw, 16px)' }}>
                {painel()}
                {mensagem && <p style={{ color: '#ffb4b4', margin: '8px 0 0' }}>{mensagem}</p>}
            </div>

            <span className="rotulo-lago rotulo-esquerdo">Mar Inimigo</span>
            <span className="rotulo-lago rotulo-direito">Seu Mar</span>

            <div className="arena">
                <div className="lago-grid lago-esquerdo">
                    {montarTabuleiro(meusTiros, true)}
                    {status === 'FINALIZADA' && naviosInimigos.map((n, i) => (
                        <img key={i} src={SPRITES[n.tipo]} style={{ ...estiloNavio(n.tamanho, n.linha, n.coluna, n.direcao), opacity: 0.8 }} />
                    ))}
                </div>
                <div className="lago-grid lago-direito">
                    {montarTabuleiro(tirosInimigos, false)}
                    {meusNavios.map((n, i) => (
                        <img key={i} src={SPRITES[n.tipo]} style={estiloNavio(n.tamanho, n.linha, n.coluna, n.direcao)} />
                    ))}
                </div>
            </div>
        </>
    );
}

export default BatalhaQuiz;

import { useState, useEffect, useRef } from 'react';
import { conectarMinado, atirarMinado } from '../ws/socket';
import { buscarPartidaMinada } from '../api/api';
import { tocarMusica, tocarSom } from '../audio/musica';


const GRID = 16;
const ESPESSURA = 1.3;

const SPRITES = {
    5: '/navios/carrier.png',
    4: '/navios/battleship.png',
    3: '/navios/cruiser.png',
    2: '/navios/destroyer.png'
};

function BatalhaMinada({ jogador, gameId, meusNavios, minhasMinas, voltarLobby }) {
    const [client, setClient] = useState(null);
    const [meusTiros, setMeusTiros] = useState({});
    const [tirosInimigos, setTirosInimigos] = useState({});
    const [pistas, setPistas] = useState({});
    const [pistasMeuMar, setPistasMeuMar] = useState({});
    const [bandeiras, setBandeiras] = useState({});
    const [turno, setTurno] = useState('');
    const [status, setStatus] = useState('');
    const [vencedor, setVencedor] = useState(null);
    const [mensagem, setMensagem] = useState('');
    const [contagem, setContagem] = useState(null);
    const jaContou = useRef(false);
    const [liberado, setLiberado] = useState(false);

    useEffect(() => {
        const c = conectarMinado(gameId, jogador,
            (tiro) => {
                if (tiro.resultado) {
                    const chave = `${tiro.linha}-${tiro.coluna}`;
                    if (tiro.autor === jogador) {
                        setMeusTiros((a) => ({ ...a, [chave]: tiro.resultado }));
                    } else {
                        setTirosInimigos((a) => ({ ...a, [chave]: tiro.resultado }));
                    }
                    if (tiro.resultado === 'MINA') {
                        tocarSom('explosao');
                    }
                    if (tiro.casasReveladas) {
                        if (tiro.autor === jogador) {
                            setPistas((a) => {
                                const novo = { ...a };
                                for (const casa of tiro.casasReveladas) {
                                    novo[`${casa.linha}-${casa.coluna}`] = { minas: casa.minasVizinhas, navios: casa.naviosVizinhos };
                                }
                                return novo;
                            });
                        } else {
                            setPistasMeuMar((a) => {
                                const novo = { ...a };
                                for (const casa of tiro.casasReveladas) {
                                    novo[`${casa.linha}-${casa.coluna}`] = true;
                                }
                                return novo;
                            });
                        }
                    }
                }
                setTurno(tiro.turnoAtual);
                setStatus(tiro.status);
                setVencedor(tiro.vencedor);
                setMensagem('');
            },
            (erro) => setMensagem(erro.mensagem),
            (info) => {
                if (!jaContou.current) {
                    jaContou.current = true;
                    setContagem(info.segundos);
                }
            }
        );
        setClient(c);
        return () => { c.deactivate(); };
    }, [gameId, jogador]);

    useEffect(() => {
        if (contagem === null) return;
        if (contagem === 0) {
            setContagem(null);
            setLiberado(true);
            return;
        }
        const t = setTimeout(() => setContagem(contagem - 1), 1000);
        return () => clearTimeout(t);
    }, [contagem]);

    useEffect(() => {
        if (status === 'FINALIZADA') {
            tocarMusica(vencedor === jogador ? 'vitoria' : 'derrota', false);
        }
    }, [status, vencedor]);

    useEffect(() => {
        buscarPartidaMinada(gameId).then((p) => {
            setTurno(p.turnoAtual);
            setStatus(p.status);
        });
    }, [gameId]);

    function clicarInimigo(linha, coluna) {
        if (contagem !== null) return;
        if (!liberado) return;
        if (status !== 'EM_ANDAMENTO') return;
        const chave = `${linha}-${coluna}`;
        if (meusTiros[chave] || pistas[chave]) return;
        if (bandeiras[chave]) return;
        atirarMinado(client, gameId, jogador, linha, coluna);
    }

    function alternarBandeira(e, linha, coluna) {
        e.preventDefault();
        if (status !== 'EM_ANDAMENTO') return;
        const chave = `${linha}-${coluna}`;
        if (meusTiros[chave] || pistas[chave]) return;
        setBandeiras((a) => {
            const novo = { ...a };
            if (novo[chave]) delete novo[chave];
            else novo[chave] = true;
            return novo;
        });
    }

    function corCelula(resultado, chave) {
        if (resultado === 'NAVIO') return '#2e8b57';
        if (resultado === 'MINA') return '#c0392b';
        if (resultado === 'AGUA') return '#15394f';
        if (pistas[chave]) return '#15394f';
        return 'transparent';
    }

    function estiloNavio(tamanho, linha, coluna, dir) {
        const passo = 100 / GRID;
        const horizontal = dir === 'HORIZONTAL';
        const base = {
            position: 'absolute',
            width: `${passo * ESPESSURA}%`,
            height: `${passo * tamanho}%`,
            objectFit: 'fill',
            pointerEvents: 'none'
        };
        if (!horizontal) {
            return {
                ...base,
                left: `${passo * (coluna + 0.5 - ESPESSURA / 2)}%`,
                top: `${passo * linha}%`
            };
        }
        const left = coluna + tamanho / 2 - ESPESSURA / 2;
        const top = linha + 0.5 - tamanho / 2;
        return {
            ...base,
            left: `${passo * left}%`,
            top: `${passo * top}%`,
            transform: 'rotate(90deg)'
        };
    }

    function montarTabuleiroInimigo() {
        const linhas = [];
        for (let l = 0; l < GRID; l++) {
            const celulas = [];
            for (let c = 0; c < GRID; c++) {
                const chave = `${l}-${c}`;
                const resultado = meusTiros[chave];
                const pista = pistas[chave];
                const bg = corCelula(resultado, chave);
                celulas.push(
                    <td
                        key={chave}
                        className={`celula ${status === 'EM_ANDAMENTO' ? 'clicavel' : ''}`}
                        onClick={() => clicarInimigo(l, c)}
                        onContextMenu={(e) => alternarBandeira(e, l, c)}
                        style={{ background: bg }}
                    >
                        {resultado === 'NAVIO' && '🚢'}
                        {resultado === 'MINA' && '💥'}
                        {!resultado && bandeiras[chave] && '🚩'}
                        {resultado !== 'NAVIO' && resultado !== 'MINA' && !bandeiras[chave] && pista && (
                            <span style={{ position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 'calc(var(--celula) * 0.42)', lineHeight: 1, color: '#ffffff', fontWeight: 800, textShadow: '0 1px 3px rgba(0,0,0,0.7)' }}>
                                💣{pista.minas}
                            </span>
                        )}
                    </td>
                );
            }
            linhas.push(<tr key={l}>{celulas}</tr>);
        }
        return <table className="tabuleiro"><tbody>{linhas}</tbody></table>;
    }

    function montarMeuTabuleiro() {
        const minasSet = new Set(minhasMinas.map((m) => `${m.linha}-${m.coluna}`));
        const linhas = [];
        for (let l = 0; l < GRID; l++) {
            const celulas = [];
            for (let c = 0; c < GRID; c++) {
                const chave = `${l}-${c}`;
                const resultado = tirosInimigos[chave];
                const revelada = pistasMeuMar[chave];
                const temMina = minasSet.has(chave);
                let bg = 'transparent';
                if (resultado === 'NAVIO') bg = '#2e8b57';
                else if (resultado === 'MINA') bg = '#c0392b';
                else if (resultado === 'AGUA') bg = '#15394f';
                else if (revelada) bg = '#15394f';
                celulas.push(
                    <td key={chave} className="celula" style={{ background: bg }}>
                        {resultado === 'NAVIO' && '💥'}
                        {resultado === 'AGUA' && '•'}
                        {temMina && (
                            <img src="/bomba.png" style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', objectFit: 'contain', pointerEvents: 'none' }} />
                        )}
                    </td>
                );
            }
            linhas.push(<tr key={l}>{celulas}</tr>);
        }

        return (
            <div style={{ position: 'relative', display: 'inline-block' }}>
                <table className="tabuleiro"><tbody>{linhas}</tbody></table>
                {meusNavios.map((n, i) => (
                    <img key={i} src={SPRITES[n.tamanho]} style={estiloNavio(n.tamanho, n.linha, n.coluna, n.direcao)} />
                ))}
            </div>
        );
    }

    const minhaVez = turno === jogador;

    return (
        <>
            {contagem !== null && (
                <div className="contagem-regressiva">
                    <span className="contagem-numero" key={contagem}>{contagem}</span>
                </div>
            )}
            <div className="painel painel-batalha">
                {status === 'FINALIZADA' && (
                    <>
                        <h2 style={{ margin: 0 }}>{vencedor === jogador ? 'Você venceu! 🏆' : 'Você perdeu! 💀'}</h2>
                        <button onClick={voltarLobby} style={{ marginTop: 10 }}>Voltar ao Lobby</button>
                    </>
                )}
                {status === 'EM_ANDAMENTO' && (
                    <h3 style={{ margin: 0 }}>🏁 Corrida! Ache toda a frota inimiga primeiro!</h3>
                )}
                {(status === 'POSICIONANDO' || status === 'AGUARDANDO') && (
                    <h3 style={{ margin: 0 }}>Esperando o outro jogador...</h3>
                )}
                {mensagem && <p style={{ color: '#ffb4b4', margin: '8px 0 0' }}>{mensagem}</p>}
            </div>

            <span className="rotulo-lago rotulo-esquerdo">Mar Inimigo</span>
            <span className="rotulo-lago rotulo-direito">Seu Mar</span>

            <div className="arena arena-min">
                <div className="lago-grid lago-esquerdo">
                    {montarTabuleiroInimigo()}
                </div>
                <div className="lago-grid lago-direito">
                    {montarMeuTabuleiro()}
                </div>
            </div>
        </>
    );
}

export default BatalhaMinada;

import { useState, useEffect } from 'react';
import { conectar, atirar } from '../ws/socket';
import { buscarPartida } from '../api/api';
import { tocarMusica, tocarSom } from '../audio/musica';
import { estiloNavio as estiloNavioBase, ESPESSURA } from '../utils/navios';

const SPRITES = {
    PORTA_AVIOES: '/navios/carrier.png',
    ENCOURACADO: '/navios/battleship.png',
    CRUZADOR: '/navios/cruiser.png',
    SUBMARINO: '/navios/submarine.png',
    DESTROYER: '/navios/destroyer.png'
};

const DESTROCOS = {
    PORTA_AVIOES: '/destrocos/carrier.png',
    ENCOURACADO: '/destrocos/battleship.png',
    CRUZADOR: '/destrocos/cruiser.png',
    SUBMARINO: '/destrocos/submarine.png',
    DESTROYER: '/destrocos/destroyer.png'
};

function Batalha({ jogador, gameId, meusNavios, voltarLobby }) {
    const [client, setClient] = useState(null);
    const [meusTiros, setMeusTiros] = useState({});
    const [tirosInimigos, setTirosInimigos] = useState({});
    const [turno, setTurno] = useState('');
    const [status, setStatus] = useState('EM_ANDAMENTO');
    const [vencedor, setVencedor] = useState(null);
    const [mensagem, setMensagem] = useState('');
    const [naviosInimigos, setNaviosInimigos] = useState([]);
    const [afundadosInimigos, setAfundadosInimigos] = useState([]);

    useEffect(() => {
        const c = conectar(
            gameId,
            jogador,
            (tiro) => {
                if (tiro.resultado) {
                    const chave = `${tiro.linha}-${tiro.coluna}`;
                    if (tiro.autor === jogador) {
                        setMeusTiros((antigos) => ({ ...antigos, [chave]: tiro.resultado }));
                    } else {
                        setTirosInimigos((antigos) => ({ ...antigos, [chave]: tiro.resultado }));
                    }
                    if (tiro.resultado === 'ACERTO' || tiro.resultado === 'AFUNDADO') {
                        tocarSom('explosao');
                    }
                    if (tiro.navioAfundado && tiro.autor === jogador) {
                        setAfundadosInimigos((antigos) => [...antigos, tiro.navioAfundado]);
                    }
                }
                setTurno(tiro.turnoAtual);
                setStatus(tiro.status);
                setVencedor(tiro.vencedor);
                setMensagem('');
            },
            (erro) => {
                setMensagem(erro.mensagem);
            }
        );
        setClient(c);

        return () => {
            c.deactivate();
        };
    }, [gameId, jogador]);

    useEffect(() => {
        if (status === 'FINALIZADA') {
            tocarMusica(vencedor === jogador ? 'vitoria' : 'derrota', false);
        }
    }, [status, vencedor]);

    useEffect(() => {
        buscarPartida(gameId).then((p) => {
            setTurno(p.turnoAtual);
            setStatus(p.status);
            setVencedor(p.vencedor);
        });
    }, [gameId]);

    useEffect(() => {
        if (status !== 'FINALIZADA') return;
        buscarPartida(gameId).then((p) => {
            const souJogador1 = jogador === p.jogador1;
            setNaviosInimigos((souJogador1 ? p.navios2 : p.navios1) || []);
        });
    }, [status]);

    function clicarInimigo(linha, coluna) {
        if (status !== 'EM_ANDAMENTO') return;
        if (turno !== jogador) return;
        const chave = `${linha}-${coluna}`;
        if (meusTiros[chave]) return;
        atirar(client, gameId, jogador, linha, coluna);
    }

    function classeCelula(resultado) {
        if (resultado === 'AFUNDADO') return 'celula fogo afundado';
        if (resultado === 'ACERTO') return 'celula fogo';
        if (resultado === 'AGUA') return 'celula agua-tiro';
        return 'celula';
    }

    function montarTabuleiro(tiros, clicavel) {
        const linhas = [];
        for (let l = 0; l < 10; l++) {
            const celulas = [];
            for (let c = 0; c < 10; c++) {
                const chave = `${l}-${c}`;
                const resultado = tiros[chave];
                const classe = classeCelula(resultado) + (clicavel && status === 'EM_ANDAMENTO' && turno === jogador ? ' clicavel' : '');
                celulas.push(
                    <td key={chave} className={classe} onClick={clicavel ? () => clicarInimigo(l, c) : undefined} />
                );
            }
            linhas.push(<tr key={l}>{celulas}</tr>);
        }
        return (
            <table className="tabuleiro">
                <tbody>{linhas}</tbody>
            </table>
        );
    }

    function estiloNavio(tamanho, linha, coluna, dir) {
        return estiloNavioBase(tamanho, linha, coluna, dir, (n) => `calc(var(--celula) * ${n})`);
    }

    function navioAfundado(navio) {
        for (let i = 0; i < navio.tamanho; i++) {
            const chave = navio.direcao === 'HORIZONTAL'
                ? `${navio.linha}-${navio.coluna + i}`
                : `${navio.linha + i}-${navio.coluna}`;
            const r = tirosInimigos[chave];
            if (r !== 'ACERTO' && r !== 'AFUNDADO') return false;
        }
        return true;
    }

    function estiloDestroco(navio) {
        const horizontal = navio.direcao === 'HORIZONTAL';
        const lado = navio.tamanho;
        const leftCel = horizontal
            ? navio.coluna
            : navio.coluna + 0.5 - lado / 2;
        const topCel = horizontal
            ? navio.linha + 0.5 - lado / 2
            : navio.linha;
        return {
            position: 'absolute',
            width: `calc(var(--celula) * ${lado})`,
            height: `calc(var(--celula) * ${lado})`,
            left: `calc(var(--celula) * ${leftCel})`,
            top: `calc(var(--celula) * ${topCel})`,
            pointerEvents: 'none',
            transform: horizontal ? 'rotate(90deg)' : 'none'
        };
    }

    function camadaNavios() {
        return meusNavios.map((navio, i) => {
            const afundado = navioAfundado(navio);
            if (!afundado) {
                return (
                    <img
                        key={i}
                        src={SPRITES[navio.tipo]}
                        style={estiloNavio(navio.tamanho, navio.linha, navio.coluna, navio.direcao)}
                    />
                );
            }
            return (
                <div key={i} style={estiloDestroco(navio)}>
                    <img
                        src={DESTROCOS[navio.tipo]}
                        className="destrocos"
                        style={{ width: '100%', height: '100%', objectFit: 'contain' }}
                    />
                </div>
            );
        });
    }

    const minhaVez = turno === jogador;

    return (
        <>
            <div className="painel painel-batalha">
                {status === 'FINALIZADA' && (
                    <>
                        <h2 style={{ margin: 0 }}>{vencedor === jogador ? 'Você venceu! 🏆' : 'Você perdeu! 💀'}</h2>
                        <button onClick={voltarLobby} style={{ marginTop: 10 }}>Voltar ao Lobby</button>
                    </>
                )}
                {status === 'EM_ANDAMENTO' && (
                    <h3 style={{ margin: 0 }}>{minhaVez ? '🎯 Sua vez! Ataque o inimigo' : '⏳ Aguardando o adversário...'}</h3>
                )}
                {(status === 'POSICIONANDO' || status === 'AGUARDANDO') && (
                    <h3 style={{ margin: 0 }}>Esperando o outro jogador ficar pronto...</h3>
                )}
                {mensagem && <p style={{ color: '#ffb4b4', margin: '8px 0 0' }}>{mensagem}</p>}
            </div>

            <span className="rotulo-lago rotulo-esquerdo">Mar Inimigo</span>
            <span className="rotulo-lago rotulo-direito">Seu Mar</span>

            <div className="arena">
                <div className="lago-grid lago-esquerdo">
                    {montarTabuleiro(meusTiros, true)}
                    {afundadosInimigos.map((navio, i) => (
                        <div key={i} style={estiloDestroco(navio)}>
                            <img
                                src={DESTROCOS[navio.tipo]}
                                className="destrocos"
                                style={{ width: '100%', height: '100%', objectFit: 'contain' }}
                            />
                        </div>
                    ))}
                    {status === 'FINALIZADA' && naviosInimigos.map((n, i) => (
                        <img key={i} src={SPRITES[n.tipo]} style={{ ...estiloNavio(n.tamanho, n.linha, n.coluna, n.direcao), opacity: 0.8 }} />
                    ))}
                </div>
                <div className="lago-grid lago-direito">
                    {montarTabuleiro(tirosInimigos, false)}
                    {camadaNavios()}
                </div>
            </div>
        </>
    );
}

export default Batalha;

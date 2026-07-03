import { useState, useEffect } from 'react';
import { conectarMinado, atirarMinado } from '../ws/socket';
import { buscarPartidaMinada } from '../api/api';

const CELULA = 28;
const GRID = 16;

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
    const [status, setStatus] = useState('EM_ANDAMENTO');
    const [vencedor, setVencedor] = useState(null);
    const [tirosRestantes, setTirosRestantes] = useState(10);
    const [mensagem, setMensagem] = useState('');

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
                setTirosRestantes(tiro.tirosRestantes);
                setMensagem('');
            },
            (erro) => setMensagem(erro.mensagem)
        );
        setClient(c);
        return () => { c.deactivate(); };
    }, [gameId, jogador]);

    useEffect(() => {
        buscarPartidaMinada(gameId).then((p) => {
            setTurno(p.turnoAtual);
            setStatus(p.status);
            setTirosRestantes(p.tirosRestantes);
        });
    }, [gameId]);

    function clicarInimigo(linha, coluna) {
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
        const horizontal = dir === 'HORIZONTAL';
        const comprimento = tamanho * CELULA;
        const base = { position: 'absolute', width: CELULA, height: comprimento, objectFit: 'fill', pointerEvents: 'none' };
        if (!horizontal) return { ...base, left: coluna * CELULA, top: linha * CELULA };
        const centroX = coluna * CELULA + comprimento / 2;
        const centroY = linha * CELULA + CELULA / 2;
        return { ...base, left: centroX - CELULA / 2, top: centroY - comprimento / 2, transform: 'rotate(90deg)' };
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
                        className={`celula celula-min ${status === 'EM_ANDAMENTO' ? 'clicavel' : ''}`}
                        onClick={() => clicarInimigo(l, c)}
                        onContextMenu={(e) => alternarBandeira(e, l, c)}
                        style={{ background: bg }}
                    >
                        {resultado === 'NAVIO' && '🚢'}
                        {resultado === 'MINA' && '💥'}
                        {!resultado && bandeiras[chave] && '🚩'}
                        {resultado !== 'NAVIO' && resultado !== 'MINA' && !bandeiras[chave] && pista && (
                            <span style={{position: 'absolute', inset: 0, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', fontSize: 8, lineHeight: 1}}>
                                <span style={{ color: '#ff6b6b' }}>💣{pista.minas}</span>
                                <span style={{ color: '#7CFC00' }}>🚢{pista.navios}</span>
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
        const linhas = [];
        for (let l = 0; l < GRID; l++) {
            const celulas = [];
            for (let c = 0; c < GRID; c++) {
                const chave = `${l}-${c}`;
                const resultado = tirosInimigos[chave];
                const revelada = pistasMeuMar[chave];
                let bg = 'transparent';
                if (resultado === 'NAVIO') bg = '#2e8b57';
                else if (resultado === 'MINA') bg = '#c0392b';
                else if (resultado === 'AGUA') bg = '#15394f';
                else if (revelada) bg = '#15394f';
                celulas.push(
                    <td key={chave} className="celula celula-min" style={{ background: bg }}>
                        {resultado === 'NAVIO' && '💥'}
                        {resultado === 'AGUA' && '•'}
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
                {minhasMinas.map((m, i) => (
                    <img key={`mina-${i}`} src="/bomba.png" style={{
                        position: 'absolute',
                        left: m.coluna * CELULA,
                        top: m.linha * CELULA,
                        width: CELULA,
                        height: CELULA,
                        objectFit: 'contain',
                        pointerEvents: 'none'
                    }} />
                ))}
            </div>
        );
    }

    const minhaVez = turno === jogador;

    return (
        <div style={{ width: '100%', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 16 }}>
            <div className="painel" style={{ textAlign: 'center', padding: '10px 24px' }}>
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

            <div className="tabuleiros">
                <div style={{ textAlign: 'center' }}>
                    <h4>Mar Inimigo 💣</h4>
                    {montarTabuleiroInimigo()}
                </div>
                <div style={{ textAlign: 'center' }}>
                    <h4>Seu Mar</h4>
                    {montarMeuTabuleiro()}
                </div>
            </div>
        </div>
    );
}

export default BatalhaMinada;

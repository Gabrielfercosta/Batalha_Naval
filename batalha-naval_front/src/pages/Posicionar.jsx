import { useState, useEffect } from 'react';
import { posicionarNavio as posicionarClassico, marcarPronto as prontoClassico } from '../api/api';
import { estiloNavio as estiloNavioBase, SPRITES_POR_TIPO as SPRITES } from '../utils/navios';

const FROTA = [
    { tipo: 'PORTA_AVIOES', tamanho: 5 },
    { tipo: 'ENCOURACADO', tamanho: 4 },
    { tipo: 'CRUZADOR', tamanho: 3 },
    { tipo: 'SUBMARINO', tamanho: 3 },
    { tipo: 'DESTROYER', tamanho: 2 }
];

const CELULA = 40;

function Posicionar({ jogador, gameId, aoComecarBatalha, aoVoltar, apiPosicionar = posicionarClassico, apiPronto = prontoClassico }) {
    const [direcao, setDirecao] = useState('HORIZONTAL');
    const [naviosColocados, setNaviosColocados] = useState([]);
    const [hover, setHover] = useState(null);
    const [mensagem, setMensagem] = useState('');
    const [enviando, setEnviando] = useState(false);

    const tiposColocados = naviosColocados.map((n) => n.tipo);
    const navioAtual = FROTA.find((f) => !tiposColocados.includes(f.tipo));
    const acabou = !navioAtual;

    const ocupadas = [];
    for (const n of naviosColocados) {
        for (let i = 0; i < n.tamanho; i++) {
            if (n.direcao === 'HORIZONTAL') ocupadas.push(`${n.linha}-${n.coluna + i}`);
            else ocupadas.push(`${n.linha + i}-${n.coluna}`);
        }
    }

    useEffect(() => {
        function aoTeclar(e) {
            if (e.key === 'r' || e.key === 'R') {
                setDirecao((d) => (d === 'HORIZONTAL' ? 'VERTICAL' : 'HORIZONTAL'));
            }
        }
        window.addEventListener('keydown', aoTeclar);
        return () => window.removeEventListener('keydown', aoTeclar);
    }, []);

    function estiloNavio(tamanho, linha, coluna, dir) {
        return estiloNavioBase(tamanho, linha, coluna, dir, (n) => n * CELULA);
    }

    function previaValida(linha, coluna) {
        if (!navioAtual) return false;
        for (let i = 0; i < navioAtual.tamanho; i++) {
            const l = direcao === 'HORIZONTAL' ? linha : linha + i;
            const c = direcao === 'HORIZONTAL' ? coluna + i : coluna;
            if (l > 9 || c > 9) return false;
            if (ocupadas.includes(`${l}-${c}`)) return false;
        }
        return true;
    }

    function clicarCelula(linha, coluna) {
        if (acabou) return;
        if (!previaValida(linha, coluna)) {
            setMensagem('Não dá pra posicionar aí.');
            return;
        }
        setNaviosColocados((atuais) => [...atuais, { tipo: navioAtual.tipo, tamanho: navioAtual.tamanho, linha, coluna, direcao }]);
        setMensagem('');
    }

    function removerNavio(tipo) {
        setNaviosColocados((atuais) => atuais.filter((n) => n.tipo !== tipo));
        setMensagem('');
    }

    async function comecar() {
        if (enviando) return;
        setEnviando(true);
        setMensagem('');
        try {
            for (const n of naviosColocados) {
                try {
                    await apiPosicionar(gameId, { jogador, tipo: n.tipo, linha: n.linha, coluna: n.coluna, direcao: n.direcao });
                } catch (e) {
                    if (!String(e.message).includes('já posicionou')) throw e;
                }
            }
            await apiPronto(gameId, jogador);
            aoComecarBatalha(naviosColocados);
        } catch (e) {
            setMensagem(e.message);
            setEnviando(false);
        }
    }

    const linhas = [];
    for (let l = 0; l < 10; l++) {
        const celulas = [];
        for (let c = 0; c < 10; c++) {
            celulas.push(
                <td key={`${l}-${c}`} className="celula clicavel" onMouseEnter={() => setHover({ linha: l, coluna: c })} onClick={() => clicarCelula(l, c)} />
            );
        }
        linhas.push(<tr key={l}>{celulas}</tr>);
    }

    return (
        <div className="painel" style={{ position: 'relative', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 12 }}>
            <button onClick={aoVoltar} style={{ position: 'absolute', top: 16, left: 16, margin: 0, padding: '6px 16px', fontSize: 14 }}>← Voltar</button>
            <h2>Posicione sua frota</h2>

            {acabou && <p style={{ fontSize: 18 }}>Frota completa!</p>}

            <button onClick={() => setDirecao(direcao === 'HORIZONTAL' ? 'VERTICAL' : 'HORIZONTAL')}>
                Direção: {direcao} (ou tecle R)
            </button>

            <p style={{ fontSize: 13, margin: 0, color: '#9fb8d8' }}>Clique num navio já colocado para removê-lo e reposicionar.</p>
            {mensagem && <p style={{ color: 'var(--perigo)' }}>{mensagem}</p>}

            <div className="pos-layout">
                <div className="pos-board" style={{ position: 'relative', display: 'inline-block' }} onMouseLeave={() => setHover(null)}>
                    <table className="tabuleiro"><tbody>{linhas}</tbody></table>

                    {naviosColocados.map((navio, i) => (
                        <img
                            key={i}
                            src={SPRITES[navio.tipo]}
                            onClick={() => removerNavio(navio.tipo)}
                            title="Clique para remover"
                            style={{ ...estiloNavio(navio.tamanho, navio.linha, navio.coluna, navio.direcao), pointerEvents: 'auto', cursor: 'pointer' }}
                        />
                    ))}

                    {!acabou && hover && (
                        <img
                            src={SPRITES[navioAtual.tipo]}
                            style={{
                                ...estiloNavio(navioAtual.tamanho, hover.linha, hover.coluna, direcao),
                                opacity: 0.5,
                                filter: previaValida(hover.linha, hover.coluna) ? 'none' : 'sepia(1) saturate(6) hue-rotate(-40deg)'
                            }}
                        />
                    )}
                </div>

                {!acabou && (
                    <div className="vitrine">
                        <img src={SPRITES[navioAtual.tipo]} alt={navioAtual.tipo} className="vitrine-img" />
                        <p className="vitrine-nome">{navioAtual.tipo.replaceAll('_', ' ')}</p>
                        <p className="vitrine-tamanho">{navioAtual.tamanho} casas</p>
                    </div>
                )}
            </div>

            {acabou && (
                <div>
                    <button onClick={comecar} disabled={enviando}>{enviando ? 'Iniciando...' : 'Pronto'}</button>
                </div>
            )}
        </div>
    );
}

export default Posicionar;

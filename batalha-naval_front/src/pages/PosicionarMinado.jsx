import { useState, useEffect } from 'react';
import { posicionarNavioMinado, posicionarMinaMinado, marcarProntoMinado } from '../api/api';
import { estiloNavio as estiloNavioBase, SPRITES_POR_TAMANHO as SPRITES } from '../utils/navios';

const TAMANHOS = [5, 4, 3, 3, 2];
const QTD_MINAS = 20;
const CELULA = 28;
const GRID = 16;

function PosicionarMinado({ jogador, gameId, aoComecar, aoVoltar }) {
    const [direcao, setDirecao] = useState('HORIZONTAL');
    const [naviosColocados, setNaviosColocados] = useState([]);
    const [minasColocadas, setMinasColocadas] = useState([]);
    const [hover, setHover] = useState(null);
    const [mensagem, setMensagem] = useState('');
    const [enviando, setEnviando] = useState(false);

    const restantes = [...TAMANHOS];
    for (const n of naviosColocados) {
        const idx = restantes.indexOf(n.tamanho);
        if (idx >= 0) restantes.splice(idx, 1);
    }
    const tamanhoAtual = restantes[0];
    const naviosProntos = restantes.length === 0;
    const minasFaltando = QTD_MINAS - minasColocadas.length;
    const fase = !naviosProntos ? 'navio' : (minasFaltando > 0 ? 'mina' : 'pronto');
    const tudoPronto = naviosProntos && minasFaltando <= 0;

    const ocupadasSet = new Set();
    for (const n of naviosColocados) {
        for (let i = 0; i < n.tamanho; i++) {
            if (n.direcao === 'HORIZONTAL') ocupadasSet.add(`${n.linha}-${n.coluna + i}`);
            else ocupadasSet.add(`${n.linha + i}-${n.coluna}`);
        }
    }
    const minasSet = new Set(minasColocadas.map((m) => `${m.linha}-${m.coluna}`));

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

    function navioEm(linha, coluna) {
        return naviosColocados.find((n) => {
            for (let i = 0; i < n.tamanho; i++) {
                const l = n.direcao === 'HORIZONTAL' ? n.linha : n.linha + i;
                const c = n.direcao === 'HORIZONTAL' ? n.coluna + i : n.coluna;
                if (l === linha && c === coluna) return true;
            }
            return false;
        });
    }

    function previaValida(linha, coluna) {
        if (fase !== 'navio') return false;
        for (let i = 0; i < tamanhoAtual; i++) {
            const l = direcao === 'HORIZONTAL' ? linha : linha + i;
            const c = direcao === 'HORIZONTAL' ? coluna + i : coluna;
            if (l >= GRID || c >= GRID) return false;
            if (ocupadasSet.has(`${l}-${c}`)) return false;
            if (minasSet.has(`${l}-${c}`)) return false;
        }
        return true;
    }

    function clicarCelula(linha, coluna) {
        const chave = `${linha}-${coluna}`;
        const navio = navioEm(linha, coluna);
        if (navio) {
            setNaviosColocados((atuais) => atuais.filter((n) => n !== navio));
            setMensagem('');
            return;
        }
        if (minasSet.has(chave)) {
            setMinasColocadas((atuais) => atuais.filter((m) => !(m.linha === linha && m.coluna === coluna)));
            setMensagem('');
            return;
        }
        if (fase === 'navio') {
            if (!previaValida(linha, coluna)) { setMensagem('Não dá pra posicionar o navio aí.'); return; }
            setNaviosColocados((atuais) => [...atuais, { tamanho: tamanhoAtual, linha, coluna, direcao }]);
            setMensagem('');
        } else if (fase === 'mina') {
            setMinasColocadas((atuais) => [...atuais, { linha, coluna }]);
            setMensagem('');
        } else {
            setMensagem('Tudo posicionado! Clique num navio ou mina pra reposicionar.');
        }
    }

    async function pronto() {
        if (enviando || !tudoPronto) return;
        setEnviando(true);
        setMensagem('');
        try {
            for (const n of naviosColocados) {
                try {
                    await posicionarNavioMinado(gameId, { jogador, linha: n.linha, coluna: n.coluna, tamanho: n.tamanho, direcao: n.direcao });
                } catch (e) {
                    if (!String(e.message).includes('já posicionou')) throw e;
                }
            }
            for (const m of minasColocadas) {
                try {
                    await posicionarMinaMinado(gameId, { jogador, linha: m.linha, coluna: m.coluna });
                } catch (e) {
                    if (!String(e.message).includes('já posicionou')) throw e;
                }
            }
            await marcarProntoMinado(gameId, jogador);
            aoComecar(naviosColocados, minasColocadas);
        } catch (e) {
            setMensagem(e.message);
            setEnviando(false);
        }
    }

    const linhas = [];
    for (let l = 0; l < GRID; l++) {
        const celulas = [];
        for (let c = 0; c < GRID; c++) {
            celulas.push(<td key={`${l}-${c}`} className="celula celula-min clicavel" onMouseEnter={() => setHover({ linha: l, coluna: c })} onClick={() => clicarCelula(l, c)} />);
        }
        linhas.push(<tr key={l}>{celulas}</tr>);
    }

    let statusTexto;
    if (fase === 'navio') statusTexto = `Coloque os navios — faltam ${restantes.length}`;
    else if (fase === 'mina') statusTexto = `Coloque as minas — faltam ${minasFaltando} 💣`;
    else statusTexto = 'Tudo posicionado! Confira e clique em Pronto.';

    return (
        <div className="painel" style={{ position: 'relative', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 12 }}>
            <button onClick={aoVoltar} style={{ position: 'absolute', top: 16, left: 16, margin: 0, padding: '6px 16px', fontSize: 14 }}>← Voltar</button>
            <h2>Posicione navios e minas</h2>
            <p style={{ fontSize: 16, margin: 0 }}>{statusTexto}</p>
            {fase === 'navio' && (
                <button onClick={() => setDirecao(direcao === 'HORIZONTAL' ? 'VERTICAL' : 'HORIZONTAL')}>Direção: {direcao} (ou tecle R)</button>
            )}
            <p style={{ fontSize: 13, margin: 0, color: '#9fb8d8' }}>Clique num navio ou mina já colocado para removê-lo e reposicionar.</p>
            {mensagem && <p style={{ color: 'var(--perigo)' }}>{mensagem}</p>}

            <div className="pos-board" style={{ position: 'relative', display: 'inline-block' }} onMouseLeave={() => setHover(null)}>
                <table className="tabuleiro"><tbody>{linhas}</tbody></table>
                {naviosColocados.map((n, i) => (
                    <img key={i} src={SPRITES[n.tamanho]} style={{ ...estiloNavio(n.tamanho, n.linha, n.coluna, n.direcao), pointerEvents: 'none' }} />
                ))}
                {minasColocadas.map((m, i) => (
                    <img key={`mina-${i}`} src="/bomba.png" style={{ position: 'absolute', left: m.coluna * CELULA, top: m.linha * CELULA, width: CELULA, height: CELULA, objectFit: 'contain', pointerEvents: 'none' }} />
                ))}
                {fase === 'navio' && hover && (
                    <img src={SPRITES[tamanhoAtual]} style={{ ...estiloNavio(tamanhoAtual, hover.linha, hover.coluna, direcao), opacity: 0.5, filter: previaValida(hover.linha, hover.coluna) ? 'none' : 'sepia(1) saturate(6) hue-rotate(-40deg)' }} />
                )}
            </div>

            {tudoPronto && <button onClick={pronto} disabled={enviando}>{enviando ? 'Iniciando...' : 'Pronto'}</button>}
        </div>
    );
}

export default PosicionarMinado;

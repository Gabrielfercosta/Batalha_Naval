import { useState } from 'react';
import { posicionarMinaMinado, marcarProntoMinado } from '../api/api';
import { estiloNavio as estiloNavioBase, SPRITES_POR_TAMANHO as SPRITES } from '../utils/navios';

const CELULA = 28;
const GRID = 16;
const QTD_MINAS = 20;

function PosicionarMinasMinado({ jogador, gameId, naviosColocados, ocupadas, aoComecar, aoVoltar }) {
    const [minasColocadas, setMinasColocadas] = useState([]);
    const [mensagem, setMensagem] = useState('');
    const [enviando, setEnviando] = useState(false);

    const faltam = QTD_MINAS - minasColocadas.length;
    const acabou = faltam <= 0;

    function estiloNavio(tamanho, linha, coluna, dir) {
        return estiloNavioBase(tamanho, linha, coluna, dir, (n) => n * CELULA);
    }

    function clicarCelula(linha, coluna) {
        const chave = `${linha}-${coluna}`;
        if (ocupadas.includes(chave)) { setMensagem('Já tem navio aqui.'); return; }
        const jaTem = minasColocadas.some((m) => m.linha === linha && m.coluna === coluna);
        if (jaTem) {
            setMinasColocadas((atuais) => atuais.filter((m) => !(m.linha === linha && m.coluna === coluna)));
            setMensagem('');
            return;
        }
        if (acabou) { setMensagem('Você já colocou todas as minas. Clique numa mina pra removê-la.'); return; }
        setMinasColocadas((atuais) => [...atuais, { linha, coluna }]);
        setMensagem('');
    }

    async function pronto() {
        if (enviando) return;
        setEnviando(true);
        setMensagem('');
        try {
            for (const m of minasColocadas) {
                try {
                    await posicionarMinaMinado(gameId, { jogador, linha: m.linha, coluna: m.coluna });
                } catch (e) {
                    if (!String(e.message).includes('já posicionou')) throw e;
                }
            }
            await marcarProntoMinado(gameId, jogador);
            aoComecar(minasColocadas);
        } catch (e) {
            setMensagem(e.message);
            setEnviando(false);
        }
    }

    const minasSet = new Set(minasColocadas.map((m) => `${m.linha}-${m.coluna}`));
    const linhas = [];
    for (let l = 0; l < GRID; l++) {
        const celulas = [];
        for (let c = 0; c < GRID; c++) {
            const temMina = minasSet.has(`${l}-${c}`);
            celulas.push(<td key={`${l}-${c}`} className="celula celula-min clicavel" onClick={() => clicarCelula(l, c)} style={{ cursor: temMina ? 'pointer' : 'crosshair' }} />);
        }
        linhas.push(<tr key={l}>{celulas}</tr>);
    }

    return (
        <div className="painel" style={{ position: 'relative', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 12 }}>
            <button onClick={aoVoltar} style={{ position: 'absolute', top: 16, left: 16, margin: 0, padding: '6px 16px', fontSize: 14 }}>← Voltar</button>
            <h2>Posicione suas minas</h2>
            <p style={{ fontSize: 18 }}>Faltam: <b>{faltam}</b> 💣 (clique na água)</p>
            <p style={{ fontSize: 13, margin: 0, color: '#9fb8d8' }}>Clique numa mina já colocada para removê-la.</p>
            {mensagem && <p style={{ color: 'var(--perigo)' }}>{mensagem}</p>}

            <div className="pos-board" style={{ position: 'relative', display: 'inline-block' }}>
                <table className="tabuleiro"><tbody>{linhas}</tbody></table>
                {naviosColocados.map((n, i) => <img key={i} src={SPRITES[n.tamanho]} style={estiloNavio(n.tamanho, n.linha, n.coluna, n.direcao)} />)}
                {minasColocadas.map((m, i) => (
                    <img key={`mina-${i}`} src="/bomba.png" style={{ position: 'absolute', left: m.coluna * CELULA, top: m.linha * CELULA, width: CELULA, height: CELULA, objectFit: 'contain', pointerEvents: 'none' }} />
                ))}
            </div>

            {acabou && <button onClick={pronto} disabled={enviando}>{enviando ? 'Iniciando...' : 'Pronto'}</button>}
        </div>
    );
}

export default PosicionarMinasMinado;

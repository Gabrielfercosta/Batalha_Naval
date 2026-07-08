import { useState } from 'react';
import { posicionarMinaMinado, marcarProntoMinado } from '../api/api';

const CELULA = 28;
const ESPESSURA = 1.3;
const GRID = 16;
const QTD_MINAS = 20;

const SPRITES = {
    5: '/navios/carrier.png',
    4: '/navios/battleship.png',
    3: '/navios/cruiser.png',
    2: '/navios/destroyer.png'
};

function PosicionarMinasMinado({ jogador, gameId, naviosColocados, ocupadas, aoComecar, aoVoltar }) {
    const [minasColocadas, setMinasColocadas] = useState([]);
    const [mensagem, setMensagem] = useState('');

    const faltam = QTD_MINAS - minasColocadas.length;
    const acabou = faltam <= 0;

    function estiloNavio(tamanho, linha, coluna, dir) {
        const horizontal = dir === 'HORIZONTAL';
        const largura = CELULA * ESPESSURA;
        const comprimento = CELULA * tamanho;
        const base = { position: 'absolute', width: largura, height: comprimento, objectFit: 'fill', pointerEvents: 'none' };
        if (!horizontal) {
            return { ...base, left: (coluna + 0.5) * CELULA - largura / 2, top: linha * CELULA };
        }
        const centroX = (coluna + tamanho / 2) * CELULA;
        const centroY = (linha + 0.5) * CELULA;
        return { ...base, left: centroX - largura / 2, top: centroY - comprimento / 2, transform: 'rotate(90deg)' };
    }

    async function clicarCelula(linha, coluna) {
        if (acabou) return;
        if (ocupadas.includes(`${linha}-${coluna}`)) { setMensagem('Já tem navio aqui.'); return; }
        if (minasColocadas.some(m => m.linha === linha && m.coluna === coluna)) { setMensagem('Já tem mina aqui.'); return; }
        try {
            await posicionarMinaMinado(gameId, { jogador, linha, coluna });
            setMinasColocadas([...minasColocadas, { linha, coluna }]);
            setMensagem('');
        } catch (e) {
            setMensagem(e.message);
        }
    }

    async function pronto() {
        try {
            await marcarProntoMinado(gameId, jogador);
            aoComecar(minasColocadas);
        } catch (e) {
            setMensagem(e.message);
        }
    }

    const linhas = [];
    for (let l = 0; l < GRID; l++) {
        const celulas = [];
        for (let c = 0; c < GRID; c++) {
            celulas.push(<td key={`${l}-${c}`} className="celula celula-min clicavel" onClick={() => clicarCelula(l, c)} />);
        }
        linhas.push(<tr key={l}>{celulas}</tr>);
    }

    return (
        <div className="painel" style={{ position: 'relative', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 12 }}>
            <button onClick={aoVoltar} style={{ position: 'absolute', top: 16, left: 16, margin: 0, padding: '6px 16px', fontSize: 14 }}>← Voltar</button>
            <h2>Posicione suas minas</h2>
            <p style={{ fontSize: 18 }}>Faltam: <b>{faltam}</b> 💣 (clique na água)</p>
            {mensagem && <p style={{ color: 'var(--perigo)' }}>{mensagem}</p>}

            <div className="pos-board" style={{ position: 'relative', display: 'inline-block' }}>
                <table className="tabuleiro"><tbody>{linhas}</tbody></table>
                {naviosColocados.map((n, i) => <img key={i} src={SPRITES[n.tamanho]} style={estiloNavio(n.tamanho, n.linha, n.coluna, n.direcao)} />)}
                {minasColocadas.map((m, i) => (
                    <img key={`mina-${i}`} src="/bomba.png" style={{ position: 'absolute', left: m.coluna * CELULA, top: m.linha * CELULA, width: CELULA, height: CELULA, objectFit: 'contain', pointerEvents: 'none' }} />
                ))}
            </div>

            {acabou && <button onClick={pronto}>Pronto</button>}
        </div>
    );
}

export default PosicionarMinasMinado;

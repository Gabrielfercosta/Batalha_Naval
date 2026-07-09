import { useEffect, useState } from 'react';
import { posicionarNavioMinado } from '../api/api';
import { estiloNavio as estiloNavioBase } from '../utils/navios';

const FROTA = [
    { tamanho: 5 },
    { tamanho: 4 },
    { tamanho: 3 },
    { tamanho: 3 },
    { tamanho: 2 }
];

const SPRITES = {
    5: '/navios/carrier.png',
    4: '/navios/battleship.png',
    3: '/navios/cruiser.png',
    2: '/navios/destroyer.png'
};

const CELULA = 28;
const GRID = 16;

function PosicionarNaviosMinado({ jogador, gameId, aoTerminar, aoVoltar }) {
    const [indice, setIndice] = useState(0);
    const [direcao, setDirecao] = useState('HORIZONTAL');
    const [naviosColocados, setNaviosColocados] = useState([]);
    const [ocupadas, setOcupadas] = useState([]);
    const [hover, setHover] = useState(null);
    const [mensagem, setMensagem] = useState('');

    const navioAtual = FROTA[indice];
    const acabou = indice >= FROTA.length;

    useEffect(() => {
        if (acabou) {
            aoTerminar(naviosColocados, ocupadas);
        }
    }, [acabou]);

    if (acabou) return null;

    function estiloNavio(tamanho, linha, coluna, dir) {
        return estiloNavioBase(tamanho, linha, coluna, dir, (n) => n * CELULA);
    }

    function previaValida(linha, coluna) {
        for (let i = 0; i < navioAtual.tamanho; i++) {
            const l = direcao === 'HORIZONTAL' ? linha : linha + i;
            const c = direcao === 'HORIZONTAL' ? coluna + i : coluna;
            if (l >= GRID || c >= GRID) return false;
            if (ocupadas.includes(`${l}-${c}`)) return false;
        }
        return true;
    }

    async function clicarCelula(linha, coluna) {
        if (acabou) return;
        try {
            await posicionarNavioMinado(gameId, { jogador, linha, coluna, tamanho: navioAtual.tamanho, direcao });
            const novas = [];
            for (let i = 0; i < navioAtual.tamanho; i++) {
                if (direcao === 'HORIZONTAL') novas.push(`${linha}-${coluna + i}`);
                else novas.push(`${linha + i}-${coluna}`);
            }
            setOcupadas([...ocupadas, ...novas]);
            setNaviosColocados([...naviosColocados, { tamanho: navioAtual.tamanho, linha, coluna, direcao }]);
            setIndice(indice + 1);
            setMensagem('');
        } catch (e) {
            setMensagem(e.message);
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

    return (
        <div className="painel" style={{ position: 'relative', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 12 }}>
            <button onClick={aoVoltar} style={{ position: 'absolute', top: 16, left: 16, margin: 0, padding: '6px 16px', fontSize: 14 }}>← Voltar</button>
            <h2>Posicione seus navios</h2>
            <button onClick={() => setDirecao(direcao === 'HORIZONTAL' ? 'VERTICAL' : 'HORIZONTAL')}>Direção: {direcao}</button>
            {mensagem && <p style={{ color: 'var(--perigo)' }}>{mensagem}</p>}

            <div className="pos-layout">
                <div className="pos-board" style={{ position: 'relative', display: 'inline-block' }} onMouseLeave={() => setHover(null)}>
                    <table className="tabuleiro"><tbody>{linhas}</tbody></table>
                    {naviosColocados.map((n, i) => <img key={i} src={SPRITES[n.tamanho]} style={estiloNavio(n.tamanho, n.linha, n.coluna, n.direcao)} />)}
                    {hover && <img src={SPRITES[navioAtual.tamanho]} style={{ ...estiloNavio(navioAtual.tamanho, hover.linha, hover.coluna, direcao), opacity: 0.5, filter: previaValida(hover.linha, hover.coluna) ? 'none' : 'sepia(1) saturate(6) hue-rotate(-40deg)' }} />}
                </div>

                <div className="vitrine">
                    <img src={SPRITES[navioAtual.tamanho]} alt="navio" className="vitrine-img" />
                    <p className="vitrine-nome">Navio</p>
                    <p className="vitrine-tamanho">{navioAtual.tamanho} casas ({indice + 1} de {FROTA.length})</p>
                </div>
            </div>
        </div>
    );
}

export default PosicionarNaviosMinado;

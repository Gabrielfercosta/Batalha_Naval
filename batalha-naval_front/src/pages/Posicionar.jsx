import { useState } from 'react';
import { posicionarNavio, marcarPronto } from '../api/api';
import { estiloNavio as estiloNavioBase } from '../utils/navios';

const FROTA = [
    { tipo: 'PORTA_AVIOES', tamanho: 5 },
    { tipo: 'ENCOURACADO', tamanho: 4 },
    { tipo: 'CRUZADOR', tamanho: 3 },
    { tipo: 'SUBMARINO', tamanho: 3 },
    { tipo: 'DESTROYER', tamanho: 2 }
];

const SPRITES = {
    PORTA_AVIOES: '/navios/carrier.png',
    ENCOURACADO: '/navios/battleship.png',
    CRUZADOR: '/navios/cruiser.png',
    SUBMARINO: '/navios/submarine.png',
    DESTROYER: '/navios/destroyer.png'
};

const CELULA = 40;

function Posicionar({ jogador, gameId, aoComecarBatalha, aoVoltar }) {
    const [indice, setIndice] = useState(0);
    const [direcao, setDirecao] = useState('HORIZONTAL');
    const [ocupadas, setOcupadas] = useState([]);
    const [naviosColocados, setNaviosColocados] = useState([]);
    const [hover, setHover] = useState(null);
    const [mensagem, setMensagem] = useState('');

    const navioAtual = FROTA[indice];
    const acabou = indice >= FROTA.length;

    function estiloNavio(tamanho, linha, coluna, dir) {
        return estiloNavioBase(tamanho, linha, coluna, dir, (n) => n * CELULA);
    }

    function previaValida(linha, coluna) {
        for (let i = 0; i < navioAtual.tamanho; i++) {
            const l = direcao === 'HORIZONTAL' ? linha : linha + i;
            const c = direcao === 'HORIZONTAL' ? coluna + i : coluna;
            if (l > 9 || c > 9) return false;
            if (ocupadas.includes(`${l}-${c}`)) return false;
        }
        return true;
    }

    async function clicarCelula(linha, coluna) {
        if (acabou) return;
        try {
            await posicionarNavio(gameId, { jogador, tipo: navioAtual.tipo, linha, coluna, direcao });
            const novas = [];
            for (let i = 0; i < navioAtual.tamanho; i++) {
                if (direcao === 'HORIZONTAL') novas.push(`${linha}-${coluna + i}`);
                else novas.push(`${linha + i}-${coluna}`);
            }
            setOcupadas([...ocupadas, ...novas]);
            setNaviosColocados([...naviosColocados, { tipo: navioAtual.tipo, tamanho: navioAtual.tamanho, linha, coluna, direcao }]);
            setIndice(indice + 1);
            setMensagem('');
        } catch (e) {
            setMensagem(e.message);
        }
    }

    async function comecar() {
        try {
            await marcarPronto(gameId, jogador);
            aoComecarBatalha(naviosColocados);
        } catch (e) {
            setMensagem(e.message);
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
                Direção: {direcao}
            </button>

            {mensagem && <p style={{ color: 'var(--perigo)' }}>{mensagem}</p>}

            <div className="pos-layout">
                <div className="pos-board" style={{ position: 'relative', display: 'inline-block' }} onMouseLeave={() => setHover(null)}>
                    <table className="tabuleiro"><tbody>{linhas}</tbody></table>

                    {naviosColocados.map((navio, i) => (
                        <img key={i} src={SPRITES[navio.tipo]} style={estiloNavio(navio.tamanho, navio.linha, navio.coluna, navio.direcao)} />
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
                    <button onClick={comecar}>Pronto</button>
                </div>
            )}
        </div>
    );
}

export default Posicionar;

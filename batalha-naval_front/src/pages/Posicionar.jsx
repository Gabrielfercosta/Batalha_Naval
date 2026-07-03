import { useState } from 'react';
import { posicionarNavio, marcarPronto } from '../api/api';

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
        const horizontal = dir === 'HORIZONTAL';
        const comprimento = tamanho * CELULA;
        const base = {
            position: 'absolute',
            width: CELULA,
            height: comprimento,
            objectFit: 'fill',
            pointerEvents: 'none'
        };
        if (!horizontal) {
            return { ...base, left: coluna * CELULA, top: linha * CELULA };
        }
        const centroX = coluna * CELULA + comprimento / 2;
        const centroY = linha * CELULA + CELULA / 2;
        return { ...base, left: centroX - CELULA / 2, top: centroY - comprimento / 2, transform: 'rotate(90deg)' };
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
            await posicionarNavio(gameId, {
                jogador,
                tipo: navioAtual.tipo,
                linha,
                coluna,
                direcao
            });

            const novas = [];
            for (let i = 0; i < navioAtual.tamanho; i++) {
                if (direcao === 'HORIZONTAL') novas.push(`${linha}-${coluna + i}`);
                else novas.push(`${linha + i}-${coluna}`);
            }
            setOcupadas([...ocupadas, ...novas]);
            setNaviosColocados([...naviosColocados, {
                tipo: navioAtual.tipo,
                tamanho: navioAtual.tamanho,
                linha,
                coluna,
                direcao
            }]);
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
                <td
                    key={`${l}-${c}`}
                    className="celula clicavel"
                    onMouseEnter={() => setHover({ linha: l, coluna: c })}
                    onClick={() => clicarCelula(l, c)}
                />
            );
        }
        linhas.push(<tr key={l}>{celulas}</tr>);
    }

    return (
        <div className="painel" style={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            gap: 12,
            maxWidth: 420
        }}>
            <h2>Posicione sua frota</h2>
            <button onClick={aoVoltar} style={{ alignSelf: 'flex-start' }}>⬅ Voltar</button>
            {!acabou && (
                <p style={{ fontSize: 18 }}>
                    Colocando: <b>{navioAtual.tipo}</b> (tamanho {navioAtual.tamanho})
                </p>
            )}
            {acabou && <p style={{ fontSize: 18 }}>Frota completa!</p>}

            <button onClick={() => setDirecao(direcao === 'HORIZONTAL' ? 'VERTICAL' : 'HORIZONTAL')}>
                Direção: {direcao}
            </button>

            {mensagem && <p style={{ color: 'var(--perigo)' }}>{mensagem}</p>}

            <div
                style={{ position: 'relative', display: 'inline-block' }}
                onMouseLeave={() => setHover(null)}
            >
                <table className="tabuleiro">
                    <tbody>{linhas}</tbody>
                </table>

                {naviosColocados.map((navio, i) => (
                    <img
                        key={i}
                        src={SPRITES[navio.tipo]}
                        style={estiloNavio(navio.tamanho, navio.linha, navio.coluna, navio.direcao)}
                    />
                ))}

                {!acabou && hover && (
                    <img
                        src={SPRITES[navioAtual.tipo]}
                        style={{
                            ...estiloNavio(navioAtual.tamanho, hover.linha, hover.coluna, direcao),
                            opacity: 0.5,
                            filter: previaValida(hover.linha, hover.coluna)
                                ? 'none'
                                : 'sepia(1) saturate(6) hue-rotate(-40deg)'
                        }}
                    />
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

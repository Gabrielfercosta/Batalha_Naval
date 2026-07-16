import { useState, useEffect } from 'react';
import { posicionarNavioMinado } from '../api/api';
import { estiloNavio as estiloNavioBase, SPRITES_POR_TAMANHO as SPRITES } from '../utils/navios';

const TAMANHOS = [5, 4, 3, 3, 2];
const CELULA = 28;
const GRID = 16;

function PosicionarNaviosMinado({ jogador, gameId, aoTerminar, aoVoltar }) {
    const [direcao, setDirecao] = useState('HORIZONTAL');
    const [naviosColocados, setNaviosColocados] = useState([]);
    const [hover, setHover] = useState(null);
    const [mensagem, setMensagem] = useState('');
    const [enviando, setEnviando] = useState(false);

    const restantes = [...TAMANHOS];
    for (const n of naviosColocados) {
        const idx = restantes.indexOf(n.tamanho);
        if (idx >= 0) restantes.splice(idx, 1);
    }
    const tamanhoAtual = restantes[0];
    const acabou = restantes.length === 0;

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
        if (acabou) return false;
        for (let i = 0; i < tamanhoAtual; i++) {
            const l = direcao === 'HORIZONTAL' ? linha : linha + i;
            const c = direcao === 'HORIZONTAL' ? coluna + i : coluna;
            if (l >= GRID || c >= GRID) return false;
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
        setNaviosColocados((atuais) => [...atuais, { tamanho: tamanhoAtual, linha, coluna, direcao }]);
        setMensagem('');
    }

    function removerNavio(indice) {
        setNaviosColocados((atuais) => atuais.filter((_, i) => i !== indice));
        setMensagem('');
    }

    async function continuar() {
        if (enviando) return;
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
            aoTerminar(naviosColocados, ocupadas);
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

    return (
        <div className="painel" style={{ position: 'relative', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 12 }}>
            <button onClick={aoVoltar} style={{ position: 'absolute', top: 16, left: 16, margin: 0, padding: '6px 16px', fontSize: 14 }}>← Voltar</button>
            <h2>Posicione seus navios</h2>
            <button onClick={() => setDirecao(direcao === 'HORIZONTAL' ? 'VERTICAL' : 'HORIZONTAL')}>Direção: {direcao} (ou tecle R)</button>
            <p style={{ fontSize: 13, margin: 0, color: '#9fb8d8' }}>Clique num navio já colocado para removê-lo e reposicionar.</p>
            {mensagem && <p style={{ color: 'var(--perigo)' }}>{mensagem}</p>}

            <div className="pos-layout">
                <div className="pos-board" style={{ position: 'relative', display: 'inline-block' }} onMouseLeave={() => setHover(null)}>
                    <table className="tabuleiro"><tbody>{linhas}</tbody></table>
                    {naviosColocados.map((n, i) => (
                        <img
                            key={i}
                            src={SPRITES[n.tamanho]}
                            onClick={() => removerNavio(i)}
                            title="Clique para remover"
                            style={{ ...estiloNavio(n.tamanho, n.linha, n.coluna, n.direcao), pointerEvents: 'auto', cursor: 'pointer' }}
                        />
                    ))}
                    {!acabou && hover && <img src={SPRITES[tamanhoAtual]} style={{ ...estiloNavio(tamanhoAtual, hover.linha, hover.coluna, direcao), opacity: 0.5, filter: previaValida(hover.linha, hover.coluna) ? 'none' : 'sepia(1) saturate(6) hue-rotate(-40deg)' }} />}
                </div>

                {!acabou && (
                    <div className="vitrine">
                        <img src={SPRITES[tamanhoAtual]} alt="navio" className="vitrine-img" />
                        <p className="vitrine-nome">Navio</p>
                        <p className="vitrine-tamanho">{tamanhoAtual} casas ({TAMANHOS.length - restantes.length + 1} de {TAMANHOS.length})</p>
                    </div>
                )}
            </div>

            {acabou && <button onClick={continuar} disabled={enviando}>{enviando ? 'Enviando...' : 'Continuar'}</button>}
        </div>
    );
}

export default PosicionarNaviosMinado;

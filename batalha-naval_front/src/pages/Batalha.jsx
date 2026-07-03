import { useState, useEffect } from 'react';
import { conectar, atirar } from '../ws/socket';
import { buscarPartida } from '../api/api';

const SPRITES = {
    PORTA_AVIOES: '/navios/carrier.png',
    ENCOURACADO: '/navios/battleship.png',
    CRUZADOR: '/navios/cruiser.png',
    SUBMARINO: '/navios/submarine.png',
    DESTROYER: '/navios/destroyer.png'
};

const CELULA = 40;

function Batalha({ jogador, gameId, meusNavios, voltarLobby }) {
    const [client, setClient] = useState(null);
    const [meusTiros, setMeusTiros] = useState({});
    const [tirosInimigos, setTirosInimigos] = useState({});
    const [turno, setTurno] = useState('');
    const [status, setStatus] = useState('EM_ANDAMENTO');
    const [vencedor, setVencedor] = useState(null);
    const [mensagem, setMensagem] = useState('');

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
        buscarPartida(gameId).then((p) => {
            setTurno(p.turnoAtual);
            setStatus(p.status);
            setVencedor(p.vencedor);
        });
    }, [gameId]);

    function clicarInimigo(linha, coluna) {
        if (status !== 'EM_ANDAMENTO') return;
        if (turno !== jogador) return;
        const chave = `${linha}-${coluna}`;
        if (meusTiros[chave]) return;
        atirar(client, gameId, jogador, linha, coluna);
    }

    function classeCelula(resultado) {
        if (resultado === 'ACERTO' || resultado === 'AFUNDADO') return 'celula fogo';
        if (resultado === 'AGUA') return 'celula agua-tiro';
        return 'celula';
    }

    function montarTabuleiro(tiros, clicavel, ehInimigo) {
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
        const horizontal = dir === 'HORIZONTAL';
        const comprimento = tamanho * CELULA;
        const base = { position: 'absolute', width: CELULA, height: comprimento, objectFit: 'fill', pointerEvents: 'none' };
        if (!horizontal) return { ...base, left: coluna * CELULA, top: linha * CELULA };
        const centroX = coluna * CELULA + comprimento / 2;
        const centroY = linha * CELULA + CELULA / 2;
        return { ...base, left: centroX - CELULA / 2, top: centroY - comprimento / 2, transform: 'rotate(90deg)' };
    }

    function camadaNavios() {
        return meusNavios.map((navio, i) => (
            <img key={i} src={SPRITES[navio.tipo]} style={estiloNavio(navio.tamanho, navio.linha, navio.coluna, navio.direcao)} />
        ));
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
                    <h3 style={{ margin: 0 }}>{minhaVez ? '🎯 Sua vez! Ataque o inimigo' : '⏳ Aguardando o adversário...'}</h3>
                )}
                {(status === 'POSICIONANDO' || status === 'AGUARDANDO') && (
                    <h3 style={{ margin: 0 }}>Esperando o outro jogador ficar pronto...</h3>
                )}
                {mensagem && <p style={{ color: '#ffb4b4', margin: '8px 0 0' }}>{mensagem}</p>}
            </div>

            <div className="tabuleiros">
                <div style={{ textAlign: 'center' }}>
                    <h4>Mar Inimigo</h4>
                    {montarTabuleiro(meusTiros, true, true)}
                </div>
                <div style={{ textAlign: 'center' }}>
                    <h4>Seu Mar</h4>
                    <div style={{ position: 'relative', display: 'inline-block' }}>
                        {montarTabuleiro(tirosInimigos, false, false)}
                        {camadaNavios()}
                    </div>
                </div>
            </div>
        </div>
    );
}

export default Batalha;

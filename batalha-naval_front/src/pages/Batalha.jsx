import { useState, useEffect } from 'react';
import { conectar, atirar } from '../ws/socket';
import { buscarPartida } from '../api/api';
import { tocarMusica, tocarSom } from '../audio/musica';

const SPRITES = {
    PORTA_AVIOES: '/navios/carrier.png',
    ENCOURACADO: '/navios/battleship.png',
    CRUZADOR: '/navios/cruiser.png',
    SUBMARINO: '/navios/submarine.png',
    DESTROYER: '/navios/destroyer.png'
};
const ESPESSURA = 1.3;
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
                    if (tiro.resultado === 'ACERTO' || tiro.resultado === 'AFUNDADO') {
                        tocarSom('explosao');
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
        if (status === 'FINALIZADA') {
            tocarMusica(vencedor === jogador ? 'vitoria' : 'derrota', false);
        }
    }, [status, vencedor]);

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
        const base = {
            position: 'absolute',
            width: `calc(var(--celula) * ${ESPESSURA})`,
            height: `calc(var(--celula) * ${tamanho})`,
            objectFit: 'fill',
            pointerEvents: 'none',
            '--rot': horizontal ? '90deg' : '0deg',
            transform: 'rotate(var(--rot))'
        };
        if (!horizontal) {
            const left = coluna + 0.5 - ESPESSURA / 2;
            return { ...base, left: `calc(var(--celula) * ${left})`, top: `calc(var(--celula) * ${linha})` };
        }
        const left = coluna + tamanho / 2 - ESPESSURA / 2;
        const top = linha + 0.5 - tamanho / 2;
        return { ...base, left: `calc(var(--celula) * ${left})`, top: `calc(var(--celula) * ${top})` };
    }

    function navioAfundado(navio) {
        for (let i = 0; i < navio.tamanho; i++) {
            const chave = navio.direcao === 'HORIZONTAL'
                ? `${navio.linha}-${navio.coluna + i}`
                : `${navio.linha + i}-${navio.coluna}`;
            const r = tirosInimigos[chave];
            if (r !== 'ACERTO' && r !== 'AFUNDADO') return false;
        }
        return true;
    }

    function camadaNavios() {
        return meusNavios.map((navio, i) => (
            <img
                key={i}
                src={SPRITES[navio.tipo]}
                className={navioAfundado(navio) ? 'navio-afundando' : ''}
                style={estiloNavio(navio.tamanho, navio.linha, navio.coluna, navio.direcao)}
            />
        ));
    }

    const minhaVez = turno === jogador;

    return (
        <>
            <div className="painel painel-batalha">
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

            <span className="rotulo-lago rotulo-esquerdo">Mar Inimigo</span>
            <span className="rotulo-lago rotulo-direito">Seu Mar</span>

            <div className="arena">
                <div className="lago-grid lago-esquerdo">
                    {montarTabuleiro(meusTiros, true, true)}
                </div>
                <div className="lago-grid lago-direito">
                    {montarTabuleiro(tirosInimigos, false, false)}
                    {camadaNavios()}
                </div>
            </div>
        </>
    );
}

export default Batalha;

import { useState, useEffect } from 'react';
import Login from './pages/Login';
import Cadastro from './pages/Cadastro';
import Lobby from './pages/Lobby';
import Posicionar from './pages/Posicionar';
import Batalha from './pages/Batalha';
import PosicionarNaviosMinado from './pages/PosicionarNaviosMinado';
import PosicionarMinasMinado from './pages/PosicionarMinasMinado';
import BatalhaMinada from './pages/BatalhaMinada';
import Home from './pages/Home';
import Loading from './pages/Loading';
import { sairDaPartida, sairDaPartidaMinada } from './api/api';
import { tocarMusica, retomar, alternarMudo, estaMudo, definirVolume, pegarVolume } from './audio/musica';

function App() {
    const [tela, setTela] = useState('home');
    const [jogador, setJogador] = useState('');
    const [gameId, setGameId] = useState('');
    const [meusNavios, setMeusNavios] = useState([]);
    const [ocupadasMinado, setOcupadasMinado] = useState([]);
    const [minhasMinas, setMinhasMinas] = useState([]);
    const [destinoLoading, setDestinoLoading] = useState('');
    const [modoAtual, setModoAtual] = useState('');
    const [mudo, setMudo] = useState(estaMudo());
    const [somAtivado, setSomAtivado] = useState(false);
    const [volume, setVolume] = useState(pegarVolume());
    const [mostrarVolume, setMostrarVolume] = useState(false);

    useEffect(() => {
        const tokenSalvo = localStorage.getItem('token');
        const jogadorSalvo = localStorage.getItem('jogador');
        if (tokenSalvo && jogadorSalvo) {
            setJogador(jogadorSalvo);
            setTela('lobby');

            const gameSalvo = localStorage.getItem('gameId');
            const modoSalvo = localStorage.getItem('modo');
            if (gameSalvo) {
                if (modoSalvo === 'minada') sairDaPartidaMinada(gameSalvo, jogadorSalvo).catch(() => {});
                else sairDaPartida(gameSalvo, jogadorSalvo).catch(() => {});
                localStorage.removeItem('gameId');
                localStorage.removeItem('modo');
            }
        }
    }, []);

    useEffect(() => {
        if (tela === 'posicionar' || tela === 'posicionarNaviosMinado' || tela === 'posicionarMinasMinado') {
            tocarMusica('posicionamento');
        } else if (tela === 'batalha' || tela === 'batalhaMinada') {
            tocarMusica('batalha');
        } else if (tela === 'login' || tela === 'cadastro' || tela === 'home' || tela === 'lobby') {
            tocarMusica('inicial');
        }
    }, [tela]);

    useEffect(() => {
        function ativarSom() {
            retomar();
            setSomAtivado(true);
        }
        window.addEventListener('pointerdown', ativarSom);
        return () => window.removeEventListener('pointerdown', ativarSom);
    }, []);

    function aoLogar(nome) {
        setJogador(nome);
        setTela('lobby');
    }

    function sair() {
        if (gameId) {
            if (modoAtual === 'minada') sairDaPartidaMinada(gameId, jogador).catch(() => {});
            else sairDaPartida(gameId, jogador).catch(() => {});
        }
        localStorage.removeItem('token');
        localStorage.removeItem('jogador');
        localStorage.removeItem('gameId');
        localStorage.removeItem('modo');
        setJogador('');
        setGameId('');
        setModoAtual('');
        setTela('login');
    }

    function entrarEmJogo(id, modo, telaInicial) {
        setGameId(id);
        setModoAtual(modo);
        localStorage.setItem('gameId', id);
        localStorage.setItem('modo', modo);
        setTela(telaInicial);
    }

    function voltarLobby() {
        if (gameId) {
            if (modoAtual === 'minada') sairDaPartidaMinada(gameId, jogador).catch(() => {});
            else sairDaPartida(gameId, jogador).catch(() => {});
        }
        localStorage.removeItem('gameId');
        localStorage.removeItem('modo');
        setGameId('');
        setMeusNavios([]);
        setOcupadasMinado([]);
        setMinhasMinas([]);
        setModoAtual('');
        setTela('lobby');
    }

    return (
        <div className="container">
            {!somAtivado && (
                <div className="dica-som">🔊 Clique para ativar o som</div>
            )}
            <div style={{ position: 'fixed', top: 12, left: 12, zIndex: 600, display: 'flex', alignItems: 'center', gap: 8 }} onMouseEnter={() => setMostrarVolume(true)} onMouseLeave={() => setMostrarVolume(false)}>
                <button className="btn-mudo" style={{ position: 'static' }} onClick={() => setMudo(alternarMudo())}>
                    {mudo ? '🔇' : '🔊'}
                </button>
                {mostrarVolume && (
                    <input type="range" min="0" max="1" step="0.05" value={volume} onChange={(e) => { const v = parseFloat(e.target.value); setVolume(v); definirVolume(v); }} className="slider-volume"/>
                )}
            </div>
            {tela !== 'batalha' && tela !== 'batalhaMinada' && tela !== 'loading' && tela !== 'home' && <img src="/titulo.png" alt="Batalha Naval" className="titulo-logo" />
            }

            {jogador && (
                <div style={{ position: 'fixed', top: 12, right: 16, display: 'flex', alignItems: 'center', gap: 8, fontSize: 14, zIndex: 100 }}>
                    <span style={{ background: 'rgba(15, 44, 74, 0.65)', color: '#fff', fontWeight: 700, padding: '6px 12px', borderRadius: 999 }}> {jogador}</span>
                    <button onClick={sair} style={{ padding: '6px 14px', fontSize: 14 }}>Sair</button>
                </div>
            )}
            {tela === 'home' && (
                <Home irParaLogin={() => setTela('login')} irParaCadastro={() => setTela('cadastro')} />
            )}
            {tela === 'login' && (
                <Login aoLogar={aoLogar} irParaCadastro={() => setTela('cadastro')} />
            )}
            {tela === 'cadastro' && (
                <Cadastro aoLogar={aoLogar} irParaLogin={() => setTela('login')} />
            )}
            {tela === 'lobby' && (
                <Lobby
                    jogador={jogador}
                    aoIniciarPartida={(id) => entrarEmJogo(id, 'classico', 'posicionar')}
                    aoIniciarMinada={(id) => entrarEmJogo(id, 'minada', 'posicionarNaviosMinado')}
                />
            )}

            {tela === 'posicionar' && (
                <Posicionar
                    jogador={jogador}
                    gameId={gameId}
                    aoVoltar={voltarLobby}
                    aoComecarBatalha={(navios) => { setMeusNavios(navios); setDestinoLoading('batalha'); setTela('loading'); }}
                />
            )}

            {tela === 'batalha' && (
                <Batalha jogador={jogador} gameId={gameId} meusNavios={meusNavios} voltarLobby={voltarLobby} />
            )}

            {tela === 'posicionarNaviosMinado' && (
                <PosicionarNaviosMinado
                    jogador={jogador}
                    gameId={gameId}
                    aoVoltar={voltarLobby}
                    aoTerminar={(navios, ocupadas) => {
                        setMeusNavios(navios);
                        setOcupadasMinado(ocupadas);
                        setTela('posicionarMinasMinado');
                    }}
                />
            )}

            {tela === 'posicionarMinasMinado' && (
                <PosicionarMinasMinado
                    jogador={jogador}
                    gameId={gameId}
                    naviosColocados={meusNavios}
                    ocupadas={ocupadasMinado}
                    aoVoltar={voltarLobby}
                    aoComecar={(minas) => { setMinhasMinas(minas); setDestinoLoading('batalhaMinada'); setTela('loading'); }}
                />
            )}

            {tela === 'loading' && (
                <Loading aoTerminar={() => setTela(destinoLoading)} />
            )}

            {tela === 'batalhaMinada' && (
                <BatalhaMinada jogador={jogador} gameId={gameId} meusNavios={meusNavios} minhasMinas={minhasMinas} voltarLobby={voltarLobby} />
            )}
        </div>
    );
}

export default App;

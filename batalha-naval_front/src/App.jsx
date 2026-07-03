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

function App() {
    const [tela, setTela] = useState('home');
    const [jogador, setJogador] = useState('');
    const [gameId, setGameId] = useState('');
    const [meusNavios, setMeusNavios] = useState([]);
    const [ocupadasMinado, setOcupadasMinado] = useState([]);
    const [minhasMinas, setMinhasMinas] = useState([]);
    const [destinoLoading, setDestinoLoading] = useState('');
    const [modoAtual, setModoAtual] = useState('');

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

    function aoLogar(nome) { setJogador(nome); setTela('lobby'); }

    function sair() {
        localStorage.removeItem('token');
        localStorage.removeItem('jogador');
        localStorage.removeItem('gameId');
        localStorage.removeItem('modo');
        setJogador('');
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
            {tela !== 'batalha' && tela !== 'batalhaMinada' && tela !== 'loading' && tela !== 'home' && <h1>Batalha Naval</h1>}

            {jogador && (
                <div style={{ position: 'fixed', top: 12, right: 16, display: 'flex', alignItems: 'center', gap: 8, fontSize: 14, zIndex: 100 }}>
                    <span>👤 {jogador}</span>
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
                <Cadastro irParaLogin={() => setTela('login')} />
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

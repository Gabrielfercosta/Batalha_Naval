import { useState, useEffect } from 'react';
import Login from './pages/Login';
import Cadastro from './pages/Cadastro';
import Lobby from './pages/Lobby';
import Posicionar from './pages/Posicionar';
import Batalha from './pages/Batalha';
import PosicionarNaviosMinado from './pages/PosicionarNaviosMinado';
import PosicionarMinasMinado from './pages/PosicionarMinasMinado';
import BatalhaMinada from './pages/BatalhaMinada';

function App() {
    const [tela, setTela] = useState('login');
    const [jogador, setJogador] = useState('');
    const [gameId, setGameId] = useState('');
    const [meusNavios, setMeusNavios] = useState([]);
    const [ocupadasMinado, setOcupadasMinado] = useState([]);
    const [minhasMinas, setMinhasMinas] = useState([]);

    useEffect(() => {
        const tokenSalvo = localStorage.getItem('token');
        const jogadorSalvo = localStorage.getItem('jogador');
        if (tokenSalvo && jogadorSalvo) {
            setJogador(jogadorSalvo);
            setTela('lobby');
        }
    }, []);

    function aoLogar(nome) { setJogador(nome); setTela('lobby'); }

    function sair() {
        localStorage.removeItem('token');
        localStorage.removeItem('jogador');
        setJogador('');
        setTela('login');
    }

    function voltarLobby() {
        setGameId('');
        setMeusNavios([]);
        setOcupadasMinado([]);
        setMinhasMinas([]);
        setTela('lobby');
    }

    return (
        <div className="container">
            {tela !== 'batalha' && tela !== 'batalhaMinada' && <h1>Batalha Naval</h1>}

            {jogador && (
                <div style={{ position: 'fixed', top: 12, right: 16, display: 'flex', alignItems: 'center', gap: 8, fontSize: 14, zIndex: 100 }}>
                    <span>👤 {jogador}</span>
                    <button onClick={sair} style={{ padding: '6px 14px', fontSize: 14 }}>Sair</button>
                </div>
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
                    aoIniciarPartida={(id) => { setGameId(id); setTela('posicionar'); }}
                    aoIniciarMinada={(id) => { setGameId(id); setTela('posicionarNaviosMinado'); }}
                />
            )}

            {tela === 'posicionar' && (
                <Posicionar
                    jogador={jogador}
                    gameId={gameId}
                    aoComecarBatalha={(navios) => { setMeusNavios(navios); setTela('batalha'); }}
                />
            )}

            {tela === 'batalha' && (
                <Batalha jogador={jogador} gameId={gameId} meusNavios={meusNavios} voltarLobby={voltarLobby} />
            )}

            {tela === 'posicionarNaviosMinado' && (
                <PosicionarNaviosMinado
                    jogador={jogador}
                    gameId={gameId}
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
                    aoComecar={(minas) => { setMinhasMinas(minas); setTela('batalhaMinada'); }}
                />
            )}

            {tela === 'batalhaMinada' && (
                <BatalhaMinada jogador={jogador} gameId={gameId} meusNavios={meusNavios} minhasMinas={minhasMinas} voltarLobby={voltarLobby} />
            )}
        </div>
    );
}

export default App;

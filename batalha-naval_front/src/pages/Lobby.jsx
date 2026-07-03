import { useState, useEffect } from 'react';
import { criarPartida, listarAbertas, entrarPartida, criarPartidaMinada, listarAbertasMinada, entrarPartidaMinada } from '../api/api';

function Lobby({ jogador, aoIniciarPartida, aoIniciarMinada }) {
    const [salas, setSalas] = useState([]);
    const [nome, setNome] = useState('');
    const [senha, setSenha] = useState('');
    const [modo, setModo] = useState('classico');
    const [mensagem, setMensagem] = useState('');

    async function carregarSalas() {
        const classicas = await listarAbertas();
        const minadas = await listarAbertasMinada();
        const todasClassicas = classicas.map(s => ({ ...s, modo: 'classico' }));
        const todasMinadas = minadas.map(s => ({ ...s, modo: 'minada' }));
        setSalas([...todasClassicas, ...todasMinadas]);
    }

    useEffect(() => { carregarSalas(); }, []);

    async function criar() {
        if (nome.trim() === '') { setMensagem('Dê um nome pra sala.'); return; }
        try {
            if (modo === 'classico') {
                const p = await criarPartida(jogador, nome, senha);
                aoIniciarPartida(p.gameId);
            } else {
                const p = await criarPartidaMinada(jogador, nome, senha);
                aoIniciarMinada(p.gameId);
            }
        } catch (e) { setMensagem(e.message); }
    }

    async function entrar(sala) {
        let senhaDigitada = '';
        if (sala.temSenha) senhaDigitada = window.prompt('Senha da sala:') || '';
        try {
            if (sala.modo === 'classico') {
                await entrarPartida(sala.gameId, jogador, senhaDigitada);
                aoIniciarPartida(sala.gameId);
            } else {
                await entrarPartidaMinada(sala.gameId, jogador, senhaDigitada);
                aoIniciarMinada(sala.gameId);
            }
        } catch (e) { setMensagem(e.message); }
    }

    return (
        <div className="painel" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 12, maxWidth: 520 }}>
            <h2>Lobby</h2>

            <div style={{ display: 'flex', gap: 8 }}>
                <button onClick={() => setModo('classico')} style={{ opacity: modo === 'classico' ? 1 : 0.5 }}>🚢 Clássico</button>
                <button onClick={() => setModo('minada')} style={{ opacity: modo === 'minada' ? 1 : 0.5 }}>💣 Minada</button>
            </div>

            <input value={nome} onChange={(e) => setNome(e.target.value)} placeholder="Nome da sala" />
            <input value={senha} onChange={(e) => setSenha(e.target.value)} placeholder="Senha (opcional)" />
            <div>
                <button onClick={criar}>Criar sala</button>
                <button onClick={carregarSalas}>Atualizar</button>
            </div>

            {mensagem && <p style={{ color: 'var(--perigo)' }}>{mensagem}</p>}

            <h3 style={{ fontSize: 20 }}>Salas abertas</h3>
            {salas.length === 0 && <p>Nenhuma sala aberta.</p>}
            <ul className="lista-salas">
                {salas.map((sala) => (
                    <li key={sala.gameId} className="sala-item">
                        <span>{sala.modo === 'minada' ? '💣' : '🚢'} {sala.temSenha ? '🔒 ' : ''}{sala.nome}</span>
                        <button onClick={() => entrar(sala)}>Entrar</button>
                    </li>
                ))}
            </ul>
        </div>
    );
}

export default Lobby;

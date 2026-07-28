import { useState } from 'react';
import { logar } from '../api/api';

function Login({ aoLogar, irParaCadastro }) {
    const [username, setUsername] = useState('');
    const [senha, setSenha] = useState('');
    const [erro, setErro] = useState('');
    const [carregando, setCarregando] = useState(false);

    async function entrar() {
        setCarregando(true);
        setErro('');
        try {
            const resposta = await logar(username, senha);
            localStorage.setItem('token', resposta.token);
            localStorage.setItem('jogador', username);
            aoLogar(username);
        } catch (e) {
            setErro(e.message);
        } finally {
            setCarregando(false);
        }
    }

    return (
        <div className="painel" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 12, maxWidth: 340 }}>
            <h2>Login</h2>
            <input value={username} onChange={(e) => setUsername(e.target.value)} placeholder="Usuário" disabled={carregando} />
            <input type="password" value={senha} onChange={(e) => setSenha(e.target.value)} placeholder="Senha" disabled={carregando} />
            <button onClick={entrar} disabled={carregando}>{carregando ? 'Entrando...' : 'Entrar'}</button>
            {erro && <p style={{ color: 'var(--perigo)', margin: 0 }}>{erro}</p>}
            <p style={{ fontSize: 14, margin: 0 }}>
                Não tem conta?{' '}
                <button onClick={irParaCadastro} style={{ padding: '6px 14px', fontSize: 14 }} disabled={carregando}>Criar Pinguim</button>
            </p>
        </div>
    );
}

export default Login;

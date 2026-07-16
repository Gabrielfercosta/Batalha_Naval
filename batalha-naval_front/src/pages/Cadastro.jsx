import { useState } from 'react';
import { registrar } from '../api/api';

function Cadastro({ aoLogar, irParaLogin }) {
    const [username, setUsername] = useState('');
    const [senha, setSenha] = useState('');
    const [erro, setErro] = useState('');
    const [carregando, setCarregando] = useState(false);

    function validar() {
        if (!/^[a-zA-Z0-9_]{3,20}$/.test(username)) {
            return 'O nome de usuário deve ter de 3 a 20 caracteres, usando apenas letras, números e _ (underline).';
        }
        if (senha.length < 6 || senha.length > 50) {
            return 'A senha deve ter de 6 a 50 caracteres.';
        }
        if (senha.includes(' ')) {
            return 'A senha não pode conter espaços.';
        }
        if (!/[a-zA-Z]/.test(senha) || !/\d/.test(senha)) {
            return 'A senha deve conter ao menos uma letra e um número.';
        }
        return '';
    }

    async function cadastrar() {
        if (carregando) return;
        const problema = validar();
        if (problema) {
            setErro(problema);
            return;
        }
        setCarregando(true);
        setErro('');
        try {
            const resposta = await registrar(username, senha);
            localStorage.setItem('token', resposta.token);
            localStorage.setItem('jogador', username);
            aoLogar(username);
        } catch (e) {
            setErro(e.message);
            setCarregando(false);
        }
    }

    return (
        <div className="painel" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 12, maxWidth: 340 }}>
            <h2>Criar Pinguim</h2>
            <input value={username} onChange={(e) => setUsername(e.target.value)} placeholder="Usuário" />
            <input type="password" value={senha} onChange={(e) => setSenha(e.target.value)} placeholder="Senha" />
            <p style={{ fontSize: 12, margin: 0, color: 'var(--texto-suave, #9fb8d8)', textAlign: 'center' }}>
                Usuário: 3 a 20 caracteres (letras, números e _). Senha: 6+ caracteres, com ao menos uma letra e um número.
            </p>
            <button onClick={cadastrar} disabled={carregando}>{carregando ? 'Cadastrando...' : 'Cadastrar'}</button>
            {erro && <p style={{ color: 'var(--perigo)', margin: 0 }}>{erro}</p>}
            <p style={{ fontSize: 14, margin: 0 }}>
                Já tem conta?{' '}
                <button onClick={irParaLogin} style={{ padding: '6px 14px', fontSize: 14 }}>Fazer login</button>
            </p>
        </div>
    );
}

export default Cadastro;

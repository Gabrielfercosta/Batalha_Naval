import { useState } from 'react';
import { registrar } from '../api/api';

function Cadastro({ irParaLogin }) {
    const [username, setUsername] = useState('');
    const [senha, setSenha] = useState('');
    const [erro, setErro] = useState('');
    const [sucesso, setSucesso] = useState('');

    async function cadastrar() {
        try {
            await registrar(username, senha);
            setSucesso('Pinguim criado! Agora faça login.');
            setErro('');
        } catch (e) {
            setErro(e.message);
            setSucesso('');
        }
    }

    return (
        <div className="painel" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 12, maxWidth: 340 }}>
            <h2>Criar Pinguim</h2>
            <input value={username} onChange={(e) => setUsername(e.target.value)} placeholder="Usuário" />
            <input type="password" value={senha} onChange={(e) => setSenha(e.target.value)} placeholder="Senha" />
            <button onClick={cadastrar}>Cadastrar</button>
            {erro && <p style={{ color: 'var(--perigo)', margin: 0 }}>{erro}</p>}
            {sucesso && <p style={{ color: 'var(--sucesso)', margin: 0 }}>{sucesso}</p>}
            <p style={{ fontSize: 14, margin: 0 }}>
                Já tem conta?{' '}
                <button onClick={irParaLogin} style={{ padding: '6px 14px', fontSize: 14 }}>Fazer login</button>
            </p>
        </div>
    );
}

export default Cadastro;

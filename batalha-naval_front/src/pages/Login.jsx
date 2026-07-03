import { useState } from 'react';
import { logar } from '../api/api';

function Login({ aoLogar, irParaCadastro }) {
    const [username, setUsername] = useState('');
    const [senha, setSenha] = useState('');
    const [erro, setErro] = useState('');

    async function entrar() {
        try {
            const resposta = await logar(username, senha);
            localStorage.setItem('token', resposta.token);
            localStorage.setItem('jogador', username);
            aoLogar(username);
        } catch (e) {
            setErro(e.message);
        }
    }

    return (
        <div>
            <h2>Login</h2>
            <div>
                <input
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    placeholder="Usuário"
                />
            </div>
            <div>
                <input
                    type="password"
                    value={senha}
                    onChange={(e) => setSenha(e.target.value)}
                    placeholder="Senha"
                />
            </div>
            <button onClick={entrar}>Entrar</button>

            {erro && <p style={{ color: 'red' }}>{erro}</p>}

            <p>
                Não tem conta?{' '}
                <button onClick={irParaCadastro}>Cadastrar</button>
            </p>
        </div>
    );
}

export default Login;

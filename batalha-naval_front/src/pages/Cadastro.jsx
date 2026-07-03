import { useState } from 'react';
import { registrar } from '../api/api';

function Cadastro({ aoCadastrar, irParaLogin }) {
    const [username, setUsername] = useState('');
    const [senha, setSenha] = useState('');
    const [erro, setErro] = useState('');
    const [sucesso, setSucesso] = useState('');

    async function cadastrar() {
        try {
            await registrar(username, senha);
            setSucesso('Cadastro feito! Agora faça login.');
            setErro('');
        } catch (e) {
            setErro(e.message);
            setSucesso('');
        }
    }

    return (
        <div>
            <h2>Cadastro</h2>
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
            <button onClick={cadastrar}>Cadastrar</button>

            {erro && <p style={{ color: 'red' }}>{erro}</p>}
            {sucesso && <p style={{ color: 'green' }}>{sucesso}</p>}

            <p>
                Já tem conta?{' '}
                <button onClick={irParaLogin}>Fazer login</button>
            </p>
        </div>
    );
}

export default Cadastro;

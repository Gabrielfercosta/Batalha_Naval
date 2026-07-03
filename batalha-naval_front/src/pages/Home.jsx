import { useState } from 'react';

function Home({ aoEntrar }) {
    const [nome, setNome] = useState('');

    function entrar() {
        if (nome.trim() === '') return;
        aoEntrar(nome.trim());
    }

    return (
        <div>
            <h2>Quem é você?</h2>
            <input
                value={nome}
                onChange={(e) => setNome(e.target.value)}
                placeholder="Digite seu nome"
            />
            <button onClick={entrar}>Entrar</button>
        </div>
    );
}

export default Home;

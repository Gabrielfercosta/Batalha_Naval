import { useEffect, useState } from 'react';

const TELAS = [
    '/loading.gif',
    '/loading2.gif',
    '/loading3.gif'
];

function Loading({ aoTerminar }) {
    const [gif] = useState(() => TELAS[Math.floor(Math.random() * TELAS.length)]);

    useEffect(() => {
        const timer = setTimeout(aoTerminar, 3000);
        return () => clearTimeout(timer);
    }, [aoTerminar]);

    return (
        <div className="loading-tela">
            <img src={gif} alt="Carregando..." className="loading-gif" />
            <div className="loading-barra">
                <div className="loading-preenchimento" />
            </div>
            <p className="loading-texto">Preparando-se para a Batalha...</p>
        </div>
    );
}

export default Loading;

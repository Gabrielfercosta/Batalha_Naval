function Home({ irParaLogin, irParaCadastro }) {
    return (
        <div className="boasvindas">
            <div className="botoes-entrada">
                <button className="btn-entrada" onClick={irParaCadastro}>Registrar-se</button>
                <button className="btn-entrada" onClick={irParaLogin}>Login</button>
            </div>
        </div>
    );
}

export default Home;

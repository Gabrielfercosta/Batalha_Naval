const FAIXAS = {
    inicial: '/musica/inicial.mp3',
    posicionamento: '/musica/posicionamento.mp3',
    batalha: '/musica/batalha.mp3',
    vitoria: '/musica/vitoria.mp3',
    derrota: '/musica/derrota.mp3'
};

let audioAtual = null;
let faixaAtual = '';
let mudo = localStorage.getItem('mudo') === 'true';
let volume = parseFloat(localStorage.getItem('volume'));
if (isNaN(volume)) volume = 0.5;

export function tocarMusica(nome, loop = true) {
    if (faixaAtual === nome) return;
    pararMusica();
    const audio = new Audio(FAIXAS[nome]);
    audio.loop = loop;
    audio.volume = volume;
    audio.muted = mudo;
    audio.play().catch(() => {});
    audioAtual = audio;
    faixaAtual = nome;
}

export function pararMusica() {
    if (audioAtual) {
        audioAtual.pause();
        audioAtual = null;
    }
    faixaAtual = '';
}

export function retomar() {
    if (audioAtual && audioAtual.paused) audioAtual.play().catch(() => {});
}

export function alternarMudo() {
    mudo = !mudo;
    localStorage.setItem('mudo', mudo);
    if (audioAtual) audioAtual.muted = mudo;
    return mudo;
}

export function estaMudo() {
    return mudo;
}

export function definirVolume(v) {
    volume = v;
    localStorage.setItem('volume', v);
    if (audioAtual) audioAtual.volume = v;
}

export function pegarVolume() {
    return volume;
}

const SONS = {
    explosao: '/explosao.mp3'
};

export function tocarSom(nome) {
    const som = new Audio(SONS[nome]);
    som.volume = volume;
    som.muted = mudo;
    som.play().catch(() => {});
}

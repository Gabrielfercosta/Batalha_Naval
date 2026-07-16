export const ESPESSURA = 1.3;

export function estiloNavio(tamanho, linha, coluna, dir, unidade) {
    const horizontal = dir === 'HORIZONTAL';
    const leftCel = horizontal
        ? coluna + tamanho / 2 - ESPESSURA / 2
        : coluna + 0.5 - ESPESSURA / 2;
    const topCel = horizontal
        ? linha + 0.5 - tamanho / 2
        : linha;
    return {
        position: 'absolute',
        width: unidade(ESPESSURA),
        height: unidade(tamanho),
        left: unidade(leftCel),
        top: unidade(topCel),
        objectFit: 'fill',
        pointerEvents: 'none',
        transform: horizontal ? 'rotate(90deg)' : 'none'
    };
}

export const SPRITES_POR_TIPO = {
    PORTA_AVIOES: '/navios/carrier.png',
    ENCOURACADO: '/navios/battleship.png',
    CRUZADOR: '/navios/cruiser.png',
    SUBMARINO: '/navios/submarine.png',
    DESTROYER: '/navios/destroyer.png'
};

export const SPRITES_POR_TAMANHO = {
    5: '/navios/carrier.png',
    4: '/navios/battleship.png',
    3: '/navios/cruiser.png',
    2: '/navios/destroyer.png'
};

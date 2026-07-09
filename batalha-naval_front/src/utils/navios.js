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

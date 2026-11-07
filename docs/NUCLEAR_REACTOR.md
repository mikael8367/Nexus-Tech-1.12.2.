# Reator Nuclear

Multibloco do Nexus Tech Machines que queima barras de urânio para gerar grandes quantidades de FE, com controle de calor por água e gelo e risco real de meltdown.

## Estrutura

| Item | Valor |
| --- | --- |
| Blocos necessários | 45 (3 de largura, 5 de altura, 3 de profundidade) |
| Eleição de controlador | canto mais baixo de qualquer caixa completa, igual à Mina Digital |
| Verificação de estrutura | a cada 20 ticks, apenas no controlador |

Monte um paralelepípedo 3x5x3 usando apenas blocos de Reator Nuclear. O bloco eleito como controlador executa a lógica; clicar em qualquer bloco da estrutura abre o mesmo painel.

## Combustível e energia

| Item | Valor |
| --- | --- |
| Buffer | 5.000.000 FE |
| Geração | 4.096 FE/t enquanto houver barra e espaço no buffer |
| Duração de cada barra | 12.000 ticks (10 min, cerca de 49M FE por barra) |
| Abastecimento | clique com a barra de urânio na mão em qualquer bloco da estrutura |

## Calor e resfriamento

O calor vai de 0 a 1.000 e a barra só queima se houver espaço no buffer de energia.

| Fonte | Efeito |
| --- | --- |
| Reação ativa | +2 de calor por tick |
| Resfriamento ambiente | -1 de calor por tick |
| Tanque de água (20.000 mB) | -2 de calor por tick, consumindo 4 mB/t enquanto o calor for maior que 0 |
| Gelo ou gelo compactado na casca externa | -1 de calor por tick por bloco, limite de -8 |
| Gelo quente | com calor 700 ou mais, 1 bloco de gelo derrete em água a cada 60 ticks |

Encha o tanque clicando com um balde de água (1.000 mB por balde, devolve o balde vazio). Água e gelo também atuam durante o meltdown, em ritmo maior.

## Meltdown

Quando o calor chega a 1.000:

1. Alarme sonoro (pling com pitch crescente) e partículas de fumaça e lava na estrutura.
2. Contagem regressiva de 60 segundos no actionbar para jogadores a até 64 blocos.
3. Durante a contagem a reação para e o calor sobe +1 por tick; para cancelar é preciso vencer o runaway com resfriamento: água -3/t (12 mB/t) e gelo -2 por bloco (limite -16).
4. Se o calor cair abaixo de 400 antes do fim da contagem, o meltdown é cancelado (resgate possível). Se a contagem acabar, o reator explode com o poder de 100 TNTs espalhados pela área da estrutura.

## Receitas

- Lingote de urânio: funda o minério de urânio (gera entre y 5 e 30, em veios de até 4).
- Barra de urânio (x2): chumbo em volta de urânio e diamante no centro ("LUL", "UDU", "LUL").
- Revestimento do reator: ferro, chumbo e alumínio ("ILI", "LAL", "ILI").
- Bloco do reator (x45, uma estrutura completa): revestimento em volta de urânio e bloco de ouro ("CUC", "UGU", "CUC").

## Checklist de testes manuais

- [ ] Montar 3x5x3: a GUI mostra as barras de calor, água e combustível.
- [ ] Estrutura incompleta mostra o aviso e não aceita urânio nem balde.
- [ ] Barra de urânio gera FE e o buffer alimenta cabos e máquinas.
- [ ] Balde de água enche 1.000 mB e devolve o balde vazio.
- [ ] Gelo na casca externa aparece no contador e derrete com o calor alto.
- [ ] Levar o calor ao máximo: alarme, partículas, contagem de 60s e explosão de 100 TNTs.
- [ ] Resgate: água e gelo suficientes cancelam o meltdown abaixo de 400 de calor.

# Reator Nuclear

Multibloco do Nexus Tech Machines que queima barras de urânio para gerar grandes quantidades de FE, com controle de calor por água, gelo, neve e Resfriadores Térmicos, e risco real de meltdown.

## Estrutura

| Item | Valor |
| --- | --- |
| Blocos necessários | 45 (3 de largura, 5 de altura, 3 de profundidade) |
| Eleição de controlador | canto mais baixo de qualquer caixa completa, igual à Mina Digital |
| Verificação de estrutura | a cada 20 ticks, apenas no controlador |
| Modelo | 3D: chaminé com tampa no topo, tubos nas laterais e medidor saliente na frente |

Monte um paralelepípedo 3x5x3 usando apenas blocos de Reator Nuclear. O bloco eleito como controlador executa a lógica; clicar em qualquer bloco da estrutura abre o mesmo painel.

## Ligar e desligar

O reator começa desligado. Agache (shift) e clique com a mão vazia em qualquer bloco da estrutura:

- A estrutura inteira acende (textura frontal brilhante) ou apaga.
- Um clique de alavanca toca e uma mensagem aparece na actionbar.
- Desligado, a barra de urânio não é consumida, nada é gerado e o calor cai como com a reação parada.
- O estado persiste no save e a GUI mostra "Reação desligada.".
- Durante o meltdown não dá para desligar o runaway: só resfriando abaixo de 400 ou explodindo.

## Combustível e energia

| Item | Valor |
| --- | --- |
| Buffer | 5.000.000 FE |
| Geração | 4.096 FE/t ligado, com barra e espaço no buffer |
| Duração de cada barra | 12.000 ticks (10 min, cerca de 49M FE por barra) |
| Abastecimento | clique com a barra de urânio na mão em qualquer bloco da estrutura |

## Calor e resfriamento

O calor vai de 0 a 1.000. Todas as fontes abaixo devem tocar a casca externa da estrutura (a camada de blocos ao redor dela).

| Fonte | Efeito |
| --- | --- |
| Reação ativa | +2 de calor por tick |
| Resfriamento ambiente | -1 de calor por tick |
| Tanque de água (20.000 mB, balde = +1.000 mB) | -2 de calor por tick, consumindo 4 mB/t enquanto o calor for maior que 0 |
| Gelo ou gelo compactado | -1 de calor por tick por bloco, limite de -8 |
| Neve ou camada de neve | -1 de calor por tick por bloco, limite de -4 |
| Água (fonte) ao redor | -1 de calor por tick por bloco, limite de -6 |
| Resfriador Térmico | -2 de calor por tick por bloco, limite de -12; nunca derrete |

Derretimentos (a água resultante continua resfriando):

| Evento | Condição |
| --- | --- |
| Gelo vira água | 1 bloco a cada 60 ticks com calor 700 ou mais |
| Neve vira água | 1 bloco a cada 60 ticks com calor 400 ou mais |
| Durante o meltdown | gelo e neve derretem 3x mais rápido; a água da casca evapora (1 bloco por segundo) |

Ordem de custo: gelo e neve são baratos mas derretem e viram só água fraca; o Resfriador Térmico (ferro + alumínio + bloco de neve) é permanente e o mais forte.

## Meltdown

Quando o calor chega a 1.000:

1. Alarme sonoro (pling com pitch crescente) e partículas de fumaça e lava na estrutura.
2. Contagem regressiva de 60 segundos no actionbar para jogadores a até 64 blocos.
3. Durante a contagem a reação para e o calor sobe +1 por tick. Para cancelar é preciso vencer o runaway: tanque -3/t (12 mB/t), gelo -2 por bloco (limite -16), neve -1 (limite -8), água da casca -2 (limite -12) e Resfriadores -3 por bloco (limite -24).
4. Se o calor cair abaixo de 400 antes do fim da contagem, o meltdown é cancelado (resgate possível). Se a contagem acabar, o reator explode com o poder de 100 TNTs espalhados pela área da estrutura.

## Receitas

- Lingote de urânio: funda o minério de urânio (gera entre y 5 e 30, em veios de até 4).
- Barra de urânio (x2): chumbo em volta de urânio e diamante no centro ("LUL", "UDU", "LUL").
- Revestimento do reator: ferro, chumbo e alumínio ("ILI", "LAL", "ILI").
- Bloco do reator (x45, uma estrutura completa): revestimento em volta de urânio e bloco de ouro ("CUC", "UGU", "CUC").
- Resfriador Térmico (x4): alumínio e ferro em volta de bloco de neve ("AIA", "ISI", "AIA").

## Checklist de testes manuais

- [ ] Montar 3x5x3: a GUI mostra as barras de calor, água e combustível.
- [ ] Shift+clique com a mão vazia: liga e desliga, a estrutura acende, o som toca.
- [ ] Desligado, a barra de urânio não queima e o calor cai sozinho.
- [ ] Estrutura incompleta mostra o aviso e não aceita urânio nem balde.
- [ ] Barra de urânio gera FE e o buffer alimenta cabos e máquinas.
- [ ] Balde de água enche 1.000 mB e devolve o balde vazio.
- [ ] Gelo, neve, água e Resfriadores na casca aparecem nos contadores da GUI.
- [ ] Gelo derrete em água com calor 700+; neve com 400+.
- [ ] Levar o calor ao máximo: alarme, partículas, contagem de 60s e explosão de 100 TNTs.
- [ ] Resgate: resfriamento suficiente cancela o meltdown abaixo de 400 de calor.
- [ ] Sair e voltar do mundo: o reator continua ligado ou desligado conforme foi deixado.

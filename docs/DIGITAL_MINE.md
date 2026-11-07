# Mina Digital (Digital Mine)

A Mina Digital é a máquina de escavação do Nexus Tech Machines, inspirada no Digital Miner do Mekanism. A própria máquina é um **multibloco 3x2x3** montado com 18 blocos "Mina Digital", e ela escava **uma coluna 64x128x64 abaixo da estrutura** (64 de largura x 128 de altura x 64 de profundidade, centrada na estrutura), coletando apenas os blocos escolhidos no filtro e guardando os itens no inventário interno.

## Estrutura (3x2x3)

- Monte um paralelepípedo de 3 de largura x 2 de altura x 3 de profundidade usando apenas blocos de Mina Digital (18 blocos, qualquer orientação).
- O **controlador** é eleito automaticamente: é o bloco do canto de menor coordenada da estrutura. Todos os blocos da estrutura calculam o mesmo vencedor, então exatamente um bloco minera.
- Qualquer bloco da estrutura serve para interagir: clicar com item na mão registra o filtro, agachar com a mão vazia retira itens, e a GUI aberta é sempre a do controlador.
- Se a estrutura for desmontada, a máquina para sozinha; ao remontar, ela continua de onde parou.

## Especificações

| Propriedade | Valor |
| --- | --- |
| Estrutura | 3x2x3 blocos (18 posições) do próprio bloco |
| Área de escavação | 64x128x64 abaixo da estrutura (524.288 posições), camada superior primeiro |
| Energia interna | 100.000 FE (somente o controlador aceita energia) |
| Entrada de energia | até 512 FE/t (Forge Energy) |
| Custo | 50 FE por bloco escavado |
| Velocidade | varre até 2.048 posições/tick e minera até 16 blocos/tick (limitado pela energia) |
| Inventário | 9 slots (somente saída; pipes externos podem retirar, não inserir) |
| Filtro | até 6 tipos de item |
| Dureza | 3,5 (picareta nível 1) |

## Operação

1. Monte a estrutura 3x2x3.
2. Alimente o controlador com FE por cabo de energia (a energia só entra pelo bloco controlador).
3. Clique em qualquer bloco da estrutura **com o item/minério na mão** para registrá-lo como filtro (o item não é consumido). Filtro vazio = minera **qualquer minério** (nomes do OreDictionary que começam com `ore`).
4. A correspondência do filtro é por item+metadado **ou** por classe de OreDictionary (`oreIron`, `oreGold`, ...), então minérios de outros mods que compartilham a tag também são aceitos.
5. Agache e clique **com a mão vazia** para retirar os itens minerados; se a saída estiver vazia, o clique limpa o filtro.
6. A GUI mostra energia, progresso (x / 524.288), estado e o filtro atual.

## Regras e limitações

- Não escava bedrock nem blocos com TileEntity (baús, máquinas de outros mods) para não perder conteúdo.
- Chunks não carregados são pulados (a máquina nunca força o carregamento de chunks); minérios em chunks que carregarem depois de varridos não são coletados.
- Sem encantamento Fortune/Silk Touch: drops equivalentes a ferramenta sem encantamento.
- Se o inventário encher, a escavação pausa até liberar espaço (GUI mostra "Saída cheia").
- Ao quebrar qualquer bloco, o inventário interno cai no chão; o filtro não (os itens de filtro nunca são consumidos).

## Consumo de energia

O consumo interno usa `EnergyBuffer.spendInternal`, sem limite de extração externa, então cabos de energia não conseguem drenar o buffer interno. Custo total no pior caso: 524.288 blocos x 50 FE = 26.214.400 FE.

## Texturas

Todas as texturas das máquinas são geradas por `tools/generate_textures.py` (determinístico) e commitadas pelo GitHub Actions (`.github/workflows/generate-textures.yml`). Não edite os PNGs à mão; ajuste o script e deixe o workflow regenerar.

## Testes manuais pendentes (1.12.2, Forge 14.23.5.2860)

- [ ] Estrutura incompleta não minera; a GUI mostra o aviso.
- [ ] Estrutura 3x2x3 completa elege um único controlador e começa a escavar a coluna 64x128x64.
- [ ] Remover um bloco para a máquina na hora; remontar retoma do ponto exato.
- [ ] Filtro vazio coleta apenas minérios; filtro específico coleta só o escolhido (testar minério de outro mod).
- [ ] Cabo conectado só no bloco controlador alimenta a máquina; cabos nos outros 17 blocos não conectam.
- [ ] Sem energia, a escavação pausa e retoma ao receber FE.
- [ ] Inventário cheio pausa a escavação e a GUI mostra "Saída cheia".
- [ ] Agachar com a mão vazia retira itens de qualquer bloco da estrutura.
- [ ] Quebrar o controlador dropa o inventário sem corromper o chunk.

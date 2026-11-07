# Mina Digital (Digital Mine)

A Mina Digital é a máquina de escavação do Nexus Tech Machines, inspirada no Digital Miner do Mekanism. Ela escava **uma área de 3x2x3 blocos diretamente abaixo da máquina** (3 de largura x 2 de altura x 3 de profundidade), coletando apenas os blocos escolhidos no filtro e guardando os itens no inventário interno.

## Especificações

| Propriedade | Valor |
| --- | --- |
| Área de escavação | 3x2x3 blocos (18 posições), camada superior primeiro |
| Energia interna | 20.000 FE |
| Entrada de energia | até 512 FE/t (Forge Energy) |
| Custo | 50 FE por bloco escavado |
| Inventário | 9 slots (somente saída; pipes externos podem retirar, não inserir) |
| Filtro | até 6 tipos de item |
| Dureza | 3,5 (picareta nível 1) |

## Operação

1. Alimente a máquina com FE por cabo de energia (qualquer lado).
2. Clique na máquina **com o item/minério na mão** para registrá-lo como filtro (o item não é consumido). Filtro vazio = minera **qualquer minério** (qualquer nome do OreDictionary que comece com `ore`).
3. A correspondência do filtro é por item+metadado **ou** por classe de OreDictionary (`oreIron`, `oreGold`, ...), então minérios de outros mods que compartilham a tag também são aceitos.
4. Agache e clique **com a mão vazia** para retirar os itens minerados; se a saída estiver vazia, o clique limpa o filtro.
5. A GUI mostra energia, progresso (0/18), estado e o filtro atual.

## Regras e limitações

- Não escava bedrock nem blocos com TileEntity (baús, máquinas de outros mods) para não perder conteúdo.
- Blocos não-alvo são pulados e **não** são rescaneados: minérios colocados depois na área já varrida não são coletados.
- Sem encantamento Fortune/Silk Touch: drops equivalentes a ferramenta sem encantamento.
- Se o inventário encher, a escavação pausa até liberar espaço (GUI mostra "Saída cheia").
- Ao quebrar o bloco, o inventário interno cai no chão; o filtro não (os itens de filtro nunca são consumidos).

## Consumo de energia

O consumo interno usa `EnergyBuffer.spendInternal`, sem limite de extração externa, então cabos de energia não conseguem drenar o buffer interno da máquina. Custo total máximo por ciclo: 18 blocos x 50 FE = 900 FE.

## Testes manuais pendentes (1.12.2, Forge 14.23.5.2860)

- [ ] Mina com filtro vazio coleta apenas minérios dentro do volume 3x2x3 abaixo dela.
- [ ] Filtro por minério específico coleta apenas esse minério (testar minério de outro mod).
- [ ] Sem energia, a escavação pausa e retoma ao receber FE.
- [ ] Inventário cheio pausa a escavação e a GUI mostra "Saída cheia".
- [ ] Agachar com a mão vazia retira itens; com a saída vazia, limpa o filtro.
- [ ] Quebrar a máquina dropa o inventário sem corromper o chunk.
- [ ] Cabo conectado não consegue extrair energia do buffer interno da Mina.

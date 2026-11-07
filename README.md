# Nexus Tech 1.12.2

![Capa oficial do Nexus Tech 1.12.2](docs/nexus-tech-cover.svg)

**Nexus Tech 1.12.2** é um projeto de modpack para Minecraft Java 1.12.2 Forge. A proposta é reunir tecnologia, máquinas, energia, magia, automação, armazenamento, exploração e mods próprios.

Este repositório contém o código-fonte dos mods próprios e a documentação de integração. A coleção completa de mods, configurações e arquivos de distribuição ainda está sendo organizada; não considere o modpack completo até que o pacote seja montado e testado como conjunto.

## Conteúdo próprio

### Nexus Tech Machines
Novo mod Forge independente dentro do JAR do projeto, com:
- Gerador a carvão e carvão vegetal.
- Painel solar de geração diurna.
- Cabos de energia com capability Forge Energy.
- Minérios próprios: cobre, estanho, alumínio, chumbo e urânio, com lingotes refinados, receitas das máquinas e geração de veios no mundo.
- Forno elétrico para receitas da fornalha vanilla.
- Bigorna elétrica que repara itens danificáveis usando o sistema padrão de durabilidade, inclusive ferramentas e armaduras de outros mods que usem esse padrão.
- Mina Digital multibloco: monte a estrutura 3x2x3 com o próprio bloco e ela escava uma coluna 64x128x64 abaixo da estrutura, com filtro selecionável de minérios ou itens.
- Reator nuclear multibloco 3x5x3 movido a barras de urânio: gera muito FE, esfria com tanque de água e gelo ao redor, e entra em meltdown com alarme, contagem regressiva de 60 segundos e explosão de 100 TNTs se o calor máximo for atingido.

Veja [docs/POWER_SYSTEM.md](docs/POWER_SYSTEM.md) para especificações, operação e checklist de testes.
Veja [docs/DIGITAL_MINE.md](docs/DIGITAL_MINE.md) para especificações, operação e checklist de testes da Mina Digital.
Veja [docs/NUCLEAR_REACTOR.md](docs/NUCLEAR_REACTOR.md) para especificações, operação e checklist de testes do Reator Nuclear.

### ProjectE Storage Bridge
Integra o ProjectE ao Simple Storage Network por meio de um cabo de armazenamento virtual que utiliza conhecimento e EMC do jogador.

## Compatibilidade
- Minecraft Java 1.12.2
- Forge 14.23.5.2860 ou superior
- Java 8
- ProjectE PE1.4.1 e Simple Storage Network 1.8.3 para o mod ProjectE Storage Bridge
- Nexus Tech Machines usa Forge Energy (FE); integração com mods externos depende da capability e da versão concreta do mod conectado.

## Verificação
A compilação e os testes unitários ajudam a detectar falhas, mas não substituem testes no jogo. A documentação de cada módulo registra os testes manuais pendentes e limitações conhecidas.

# DMZ: Config Halo

Addon para o **DragonMine Z** (Forge 1.20.1) que permite cada jogador configurar a **própria auréola**:
altura e cor, por comando ou por arquivo de configuração.

> Testado apenas por leitura do bytecode do DMZ **2.2** (veja "Limitações").

## Comandos

| Comando | Quem pode | O que faz |
|---|---|---|
| `/dmzhalo on` / `/dmzhalo off` | OP / quem tiver permissão | Já existe no DMZ. Continua igual (este addon não muda). |
| `/dmzhalo config` | todos | Mostra sua altura e cor atuais. |
| `/dmzhalo config height <valor>` | todos | `0` = posição original do DMZ. Positivo sobe, negativo desce. Em **pixels** (1 pixel = 1/16 de bloco). Aceita decimais: `0.2`, `-0.3`, `1.5`, `-2.6`. Limite: ±64. |
| `/dmzhalo config color <hex>` | todos | Cor em hexadecimal: `FFD700` ou `#FFD700`. |
| `/dmzhalo config color reset` | todos | Volta para a cor original do DMZ. |
| `/dmzhalo config formheight current <valor>` | todos | Altura **extra** da auréola para a transformação em que você está agora. |
| `/dmzhalo config formheight <forma> <valor>` | todos | O mesmo, dizendo o nome da forma (ex.: `supersaiyan`). TAB sugere. `reset` remove, `clear` limpa tudo. |
| `/dmzhalo config hairheight current <valor>` | todos | Altura extra para o **tipo de cabelo** atual (`base`, `ssj`, `ssj2`, `ssj3`, `ssj4`, `custom`, `none`). |
| `/dmzhalo config hairheight <cabelo> <valor>` | todos | O mesmo, dizendo o tipo. `reset` remove, `clear` limpa tudo. |
| `/dmzhalo config rotation <x\|y\|z> <graus>` | todos | Gira a auréola em volta do próprio centro. `z` inclina para o lado. |
| `/dmzhalo config rotation reset` | todos | Zera a rotação. |

### Altura por transformação e por cabelo

A altura final da auréola é: `height` + altura da **forma** atual + altura do **cabelo** atual. Tudo é opcional e soma.
Durante a transformação a altura **sobe/desce suavemente**, usando os mesmos fatores de transição que o próprio DMZ usa para o corpo:
segurando **G** (acompanha o progresso da carga), na transformação instantânea (transição curta) e ao reverter.

- **Por forma**: `/dmzhalo config formheight current 2` (na forma em que você está) ou `/dmzhalo config formheight supersaiyan 2`.
  A forma *stack* (ex.: Kaioken) também conta, somando.
- **Por cabelo** (melhor quando várias formas usam o mesmo cabelo): `/dmzhalo config hairheight current 1.5` ou `/dmzhalo config hairheight ssj3 4`.
  Tipos de cabelo: `base`, `ssj`, `ssj2`, `ssj3`, `ssj4`, `custom` (forma com código de cabelo forçado) e `none` (forma sem cabelo).
  É a mesma regra que o DMZ usa para escolher o cabelo de cada forma.

Para ambos: `... <nome> reset` remove uma entrada, `... clear` remove todas, e o comando sem argumentos lista.

### Rotação

`/dmzhalo config rotation z 15` inclina a auréola para o lado (use `-15` para o outro lado). `x` inclina para frente/trás e `y` gira
(quase invisível num anel). `/dmzhalo config rotation reset` zera. A auréola gira em volta do **próprio centro**. Valores em graus (-180 a 180).

Todos os comandos de `config` alteram **somente a sua auréola**. Os outros jogadores veem a sua auréola com as suas configurações.
A auréola continua aparecendo só quando o DMZ a mostra (jogador morto, ou `/dmzhalo on`).

## Arquivo de configuração

`config/dmzconfighalo-client.toml` (criado na primeira vez que o jogo abre com o addon):

```toml
[halo]
    height = 0.0
    color = "reset"
    rotationX = 0.0
    rotationY = 0.0
    rotationZ = 0.0
    formHeights = ["supersaiyan=2.0", "supersaiyan4=4.5"]
    hairHeights = ["ssj=1.5", "ssj3=4.0"]
    applyOnJoin = true
```

- Ao entrar num mundo/servidor, os valores do arquivo são enviados ao servidor (só se forem diferentes do padrão).
- Se você editar o arquivo com o jogo aberto, a mudança é aplicada na hora.
- Quando você usa `/dmzhalo config ...`, o arquivo é atualizado automaticamente.

## Instalação

Coloque o `.jar` na pasta `mods` (cliente **e** servidor). Requer DragonMine Z 2.2+ (e as dependências dele).

## Como compilar

### Opção 1: GitHub Actions (mais fácil, não precisa instalar nada)
1. Crie um repositório no GitHub e envie todos os arquivos deste projeto.
2. Abra a aba **Actions** → workflow **Build** (roda sozinho a cada push; ou clique em *Run workflow*).
3. Quando terminar, baixe o `.jar` em **Artifacts → dmz-config-halo**.

### Opção 2: no seu computador
Requisitos: **JDK 17** e **Gradle 8.1.1**.
```
gradle wrapper --gradle-version 8.1.1   # só na primeira vez (cria o ./gradlew)
./gradlew build
```
O `.jar` fica em `build/libs/dmzconfighalo-1.2.0.jar`.

Antes de compilar, troque `mod_authors` em `gradle.properties` pelo seu nome.

## Como funciona (resumo técnico)

- **Render:** um Mixin em `DMZRacePartsLayer#renderTargetedBone`. Se o osso desenhado é o `halo` e o jogador tem configuração própria, o addon desenha o osso como o DMZ faz, mas com a posição Y somada à altura e com a cor escolhida. Sem configuração, o código original roda intacto.
- **Comando:** o addon registra o `/dmzhalo` *antes* do DMZ (prioridade máxima) para que o Brigadier junte os dois e `config` fique liberado para todos, mantendo `on/off` com a permissão original do DMZ.
- **Dados:** guardados nos dados persistentes do jogador (sobrevivem a morte e relogin) e sincronizados por pacote de rede para quem está perto.

## Limitações

- Feito para o DMZ **2.2**. Se uma atualização do DMZ renomear `renderTargetedBone`, o Mixin não encaixa (o jogo continua abrindo, só a customização deixa de funcionar e aparece um aviso no log).
- A auréola "de luz" (shader do *Realm of Light*, `TimeHaloRenderer`) é outro efeito e **não** é afetada por este addon.

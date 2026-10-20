# ClauDEX: arquitetura

Como funciona e **por que cada escolha**. Cada "medido" abaixo foi visto num SM-A055M (One UI, Android
15, 720×1600, 300 dpi); vale como ponto de partida, não como verdade para outro aparelho.

## 1. As três fontes de poder

| Fonte | O que dá | Limite |
|---|---|---|
| **Serviço de acessibilidade** (`DockService`, em `services/PerfectServer.kt`) | eventos de janela/foco, a lista de janelas na tela, clicar em nós de outros apps (a pílula da janela) | não vê tarefas; os retângulos vêm **recortados pela tela** e arredondados (±6 px); `w.root` é nulo logo após um lançamento |
| **Shizuku** (shell, uid 2000) | `am stack list` (todas as tarefas, com modo e limites), `am task resize`, `am stack remove`, `am start --windowingMode`, `settings put global`, `svc`, `cmd wifi`/`bluetooth_manager` | **morre a cada reinício do adbd** (§6); cada `am` custa ~100 ms (abre uma JVM) |
| **App comum** | `ActivityOptions` com limites de lançamento, `moveTaskToFront`, sobreposições, notificações | `getRunningTasks` só vê o próprio app; sem `SET_ORIENTATION` |

Regra: **acessibilidade para ver e tocar, Shizuku para mover e saber a verdade das tarefas.**

## 2. Mapa dos arquivos

| Arquivo | Papel |
|---|---|
| `services/PerfectServer.kt` | ~7000 linhas, herdado do upstream: `DockService` (barra, gestos, lançamento, encaixe, alternador, orientação) e `NotificationService` |
| `utils/WindowPlanner.kt` | geometria pura do modo Automático: `plan`, `largestFreeRect`, `fitOrientation` (só para limites de lançamento) |
| `utils/WindowSnapper.kt` | zonas de encaixe por arraste e o `tile` (lado a lado) pelo piso de 220 dp |
| `utils/CaptionControl.kt` | aciona os botões da pílula da janela (Maximizar/Minimizar) por acessibilidade |
| `utils/ShizukoManager.kt` | vínculo e morte do Shizuku, `grantAll` (chaves de freeform, permissões) |
| `utils/ShizukuNotice.kt` | notificação contínua enquanto o Shizuku está fora |
| `utils/EventJournal.kt` | diário persistente `Android/data/<pkg>/files/claudex-eventos.log` (256 KB, rotação) |
| `panels/QuickPanel.kt` | painel rápido (níveis, conexões, pareados) |
| `panels/SideRail.kt` | trilho lateral do retrato |
| `adapters/DockAppAdapter.kt`, `SwitcherAdapter.kt` | ícones da barra (selo, marca de aberto, arrastar para cima) e o alternador |

Código novo vai **em arquivo próprio**; o `PerfectServer.kt` só recebe a ligação.

## 3. Janelas: o ciclo completo

```
            toque no favorito fechado
                       │
                       ▼
   plan(área livre, janelas na tela) ──► limites de lançamento (9:16 se o app trava retrato)
                       │
                       ▼
   fitToPlan (900 ms depois): lê a tarefa REAL no am stack list e a leva ao retângulo inteiro
                       │
     ┌─────────────────┴──────────────────┐
     ▼                                    ▼
 JANELA ──arrastar ícone p/ cima──►  TELA CHEIA
        ◄─favorito / expandir / lado a lado─┘
```

- **O limite de lançamento é só um pedido.** Tarefa que já existe o ignora, e app travado em retrato troca
  largura e altura (medido: 1510×665 virou 665×1510). **Redimensionar a tarefa viva depois é respeitado ao
  pixel**, inclusive em formato largo para app só-retrato (o PoGO a 840×665 desenha bem). Por isso o 9:16
  vale só no lançamento, e o resto dá o retângulo inteiro.
- **Tela cheia → janela**: relançar o app com opções de janela converte a mesma tarefa, se o app reaproveita
  a instância (`singleTask`, padrão: PoGO, Waze). `singleInstancePerTask` (Chrome) abre uma **segunda**
  tarefa; nesse caso só traz para a frente (`relaunchMakesNewTask`).
- **Janela → tela cheia**: `am start --windowingMode 1` é ignorado. O botão **Maximizar** da pílula da One
  UI (`com.android.systemui:id/caption_handle` → `maximize_window`) faz isso, e o ClauDEX o aciona por
  acessibilidade (precisa de `flagReportViewIds`). Onde os ids não existem, só preenche a área.
- **Piso do topo**: o gerenciador empurra janelas para baixo de um topo mínimo (55 px medido) que nenhum
  inset informa. O piso é **aprendido** só de um deslocamento vertical puro (mesma esquerda e direita);
  aprender de um redimensionamento que não pegou já ensinou 83 por engano.
- **Piso de tamanho**: 220 dp (412 px), do próprio freeform. Quartos só existem se metade da altura couber.

## 4. Área útil

`smartAvailableArea()` = tela − **insets reservados** de status e navegação (`getInsetsIgnoringVisibility`;
os visíveis mentem, porque a área de trabalho é imersiva e as barras voltam quando o app abre) − piso
aprendido − a barra, **só se fixada**. A barra sob demanda não reserva nada: ela some quando há janela.

A barra repousa **sobre o inset inferior da navegação** (`placeDockAboveNav`). No celular em paisagem esse
inset é 0 (a barra de navegação fica ao lado); num tablet costuma estar embaixo nas duas orientações.

## 5. Orientação, barras e trilho

- A **área de trabalho** pede paisagem (`LauncherActivity.requestedOrientation`). A janela sobreposta da
  barra **não pede orientação**: quando pedia, chamar a barra sobre um app em tela cheia retrato girava a
  tela e esconder girava de volta (8 giros em 17 s, medido).
- **1,5 s de silêncio depois de um giro**: o novo layout move cada janela 1 px, e isso parecia um arraste.
- **Retrato** (a barra cobria Voltar/Recentes): a barra e a alça somem, e o trilho da direita assume.
- **Encaixe por arraste**: a borda só conta se o dedo **viajou** até ela (48 dp). A One UI toma todas as
  bordas (cima/baixo = tela dividida, lados = "stash"); o ClauDEX só age se a tarefa continuar livre e não
  guardada (`mode=freeform`, `mFreeformStashMode=0`).

## 6. Shizuku: o ponto fraco, medido

- O `shizuku_server` vive no **cgroup do adbd** (`/proc/<pid>/cgroup` = `0::/uid_0/pid_<adbd>`),
  `oom_score_adj -1000`. A memória nunca o mata, mas **todo reinício do adbd mata**: `adb tcpip`, ligar o
  cabo no PC (a função USB muda), mexer em Depuração USB ou sem fio.
- **Nada o religa até o próximo boot.** De 05/10 a 20/10, ele esteve disponível em **43% do tempo de uso**.
  As mortes caem quase sempre entre 14h30 e 16h, quando o cabo é ligado.
- Hoje: notificação contínua enquanto está fora (também 30 s depois da barra subir sem ele). O toque abre o
  Shizuku, e o Iniciar dele religa pela depuração sem fio.
- Religar pelo PC: `adb shell '$(ls $(dirname $(pm path moe.shizuku.privileged.api | sed "s/package://"))/lib/*/libshizuku.so)'`.

## 7. Custo

| Medida | Valor |
|---|---|
| CPU em repouso | 0 a 0,05% de um núcleo; arrastando janelas, ~1% |
| Memória | PSS 100-120 MB (gráficos ~25 MB de cache do HWUI; só a barra tem superfície alocada) |
| Uma semana | processo vivo 5 dias seguidos, 4 min de CPU; 0 crashes em 3 semanas |
| Mortes | LOW_MEMORY com adj 0 quando PoGO + Chrome + WhatsApp apertam os 3,7 GB do A05; volta sozinho |
| Lado a lado | 0,12 s entre janelas (era 1,5 s com a cadeia de fallbacks) |

Armadilhas que já custaram caro, todas com correção no código:
- **Janela invisível que come toque**: view `VISIBLE` com alpha 0, sem `NOT_TOUCHABLE`.
- **Histórico de 24 h recarregado a cada atualização da barra**: carregava APKs de outros apps.
- **`freezeRotation` sem permissão a cada troca de janela.**
- **Comandos que não existem na ROM**: `am task remove`, `set-windowing-mode`. Use `am stack remove` e
  sonde a capacidade uma vez.

## 8. Diário de eventos

`adb shell cat /sdcard/Android/data/com.youki.dex.claudex/files/claudex-eventos.log`: uma linha por evento
(`launch`, `fit`, `snap`, `tile`, `expand`, `favorite`, `focus`, `config`, `rail`, `shizuku`...). As linhas
de estado (`focus`/`desktop`/`config`) trazem orientação, barra, Shizuku e todas as janelas na tela. É a
primeira fonte para explicar o que o dono relata dias depois; o logcat só guarda horas. Para saber **por
que** o processo morreu: `dumpsys activity exit-info com.youki.dex.claudex`.

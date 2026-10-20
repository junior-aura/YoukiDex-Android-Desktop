# ClauDEX: transição para o tablet titular

Escrito em 20/10/2026, ao fim da fase de testes no A05. O tablet (Multilaser M10 ou Positivo Tab10) ainda
não foi medido. Tudo aqui é **o que conferir**, não o que vai acontecer. Medir antes de concluir: um número
de outro aparelho é hipótese até ser lido neste.

## 0. Antes de instalar: retrato do aparelho (somente leitura)

```
adb shell getprop ro.product.model; getprop ro.build.version.release; getprop ro.build.version.sdk
adb shell wm size; wm density                       # densidade decide dp -> px: piso 220 dp, barra 44 dp
adb shell settings get global enable_freeform_support; settings get global force_resizable_activities
adb shell am help | grep -E "^  (stack|task)"       # quais subcomandos existem nesta ROM
adb shell "am help | grep -c set-windowing-mode"    # 0 = ausente (como no A05)
adb shell cmd window help | grep -E "multi-window|ignore-orientation"
adb shell settings get secure navigation_mode       # 0 = 3 botões; 2 = gestos
adb shell grep MemTotal /proc/meminfo               # RAM: o A05 tem 3,7 GB e mata processos em uso pesado
```

Guarde a saída: é a linha de base de tudo que vem depois.

## 1. Shizuku: a prioridade nº 1

**O que já se sabe:** o servidor morre a cada reinício do adbd (§6 da arquitetura). No A05 isso deixou o
ClauDEX **57% do tempo de uso sem as funções de janela**. É genérico do Android, não da Samsung: o servidor
iniciado pelo adb vive no cgroup do adbd.

**Conferir no tablet:**
1. `cat /proc/$(pidof shizuku_server)/cgroup`: se aparecer `pid_<adbd>`, vale o mesmo.
2. Ligar e desligar o cabo, e ver se o adbd renasce (`ps -A -o PID,STIME,NAME | grep adbd`) e o servidor
   morre junto.
3. "Iniciar no boot" do Shizuku funciona? (Depende da depuração sem fio e de Wi-Fi no boot.)

**Mitigações, da mais barata à definitiva:**
- **Já no código:** notificação contínua com toque para abrir o Shizuku.
- **Kit:** conectar pela **depuração sem fio** (`adb pair` uma vez, depois `adb connect`), nunca
  `adb tcpip`. O adbd não reinicia, e o Shizuku fica de pé.
- **Definitiva (projeto novo):** o ClauDEX com um cliente adb por TLS próprio (como o Shizuku faz no boot),
  pareado uma vez com a depuração sem fio. Ao receber `binder dead`, ele mesmo roda o `libshizuku.so`.
  Exige o protocolo de pareamento (SPAKE2) e TLS. Bibliotecas como a `libadb-android` fazem isso. Vale se
  o §1.2 se confirmar no tablet.

## 2. Processos e memória

- No A05 o ClauDEX foi morto **10 vezes em 3 semanas** (29/09 ×1, 30/09 ×3, 06/10 ×1, 09/10 ×2, 19/10 ×3),
  sempre por LOW_MEMORY e sempre com o processo em primeiro plano: o sistema estava sem saída. Volta
  sozinho em ~20 s.
- No tablet: `dumpsys activity exit-info com.youki.dex.claudex` depois de uma semana. Se as mortes sumirem,
  a RAM era o gargalo. Se continuarem, medir PSS por componente (`dumpsys meminfo`).
- O ClauDEX custa 0 a 0,05% de CPU parado. Não é ele que pesa; o que pesa são os apps em janela.

## 3. Área útil × área de trabalho

- A área vem dos **insets reservados**, não dos visíveis. Conferir no diário: a primeira linha
  `launch ... smart=Rect(...)` com a área de trabalho vazia deve ser a tela menos as barras de status e
  navegação.
- **Piso do topo:** reaprendido sozinho na primeira janela (`floor N learned` no diário). Se não aparecer,
  o gerenciador desta ROM não empurra janelas, o que é bom.
- **Piso de tamanho** (220 dp): num tablet de ~800 dp de altura útil, **quartos passam a existir** e o lado
  a lado faz 2×2. Conferir `tile: N windows, N placed`.
- **Densidade:** o dono usa 280 no A05 (físico 300). No tablet, escolher a densidade **antes** de julgar
  tamanhos, porque tudo é dp.

## 4. Posição e gestos

- **Barra de navegação embaixo nas duas orientações** (típico de tablet): a barra do ClauDEX deve repousar
  acima dela. O diário mostra `dock: above bottom nav, y=<inset>`. Conferir na tela que Voltar/Início
  continuam tocáveis, com a barra chamada.
- **Trilho lateral:** foi feito porque, no retrato do celular, a barra cobria Voltar/Recentes. Num tablet
  com a barra acima da navegação o trilho pode ser desnecessário. É a preferência `portrait_side_rail`.
  Decidir pelo uso.
- **Gestos de borda do sistema:** na One UI as bordas laterais são do "stash" e as de cima/baixo da tela
  dividida. Num AOSP de tablet podem estar livres, e aí o **encaixe por arraste passa a agir de verdade**.
  Testar com arraste humano (`input motionevent` com pausa antes do UP), não com `input swipe`.
- **Navegação por gestos** (`navigation_mode=2`): o deslize da borda de baixo é do sistema. A alça da barra
  sob demanda fica nessa borda; conferir se os dois não brigam.
- **Pílula da janela:** os ids (`caption_handle`, `maximize_window`...) são da One UI. **Hipótese a
  verificar:** o "desktop windowing" do AOSP usa nomes parecidos no WM Shell. Conferir com
  `adb shell "input tap <pílula>; sleep 0.8; uiautomator dump --windows /sdcard/Download/ui.xml"`, no mesmo
  comando, porque o menu some. Sem os ids, arrastar para cima só preenche a área.

## 5. Recursos que dependem da ROM: sonde, não suponha

| Recurso | No A05 | Como sondar no tablet |
|---|---|---|
| `am stack list` (`RootTask id=`) | existe | `am stack list` |
| `am task remove` | **não existe** | o ClauDEX usa `am stack remove` |
| `am task set-windowing-mode` | não existe | sondado uma vez (`am help`) |
| `--windowingMode 5` converte tela cheia → janela | sim (singleTask/padrão) | relançar um app padrão aberto em tela cheia |
| `wm set-ignore-orientation-request` | existe; **persiste** | não usar sem decisão do dono |
| Minimizar (bolha) | One UI | botão `minimize_window` da pílula |
| Selo de notificação | listener do ClauDEX | sem mudança |

## 6. Boas práticas que valeram a pena

1. **Diário primeiro**, palpite depois: `claudex-eventos.log` + `exit-info` explicaram tudo o que o dono
   relatou dias depois.
2. **Uma fase = um commit + build do CI conferido** (hash e certificado) + teste no aparelho com o estado
   lido de volta (`am stack list`, `dumpsys window`).
3. **Depois de cada interação, `logcat -b crash`.** E conferir que o app subiu (pid, serviço vinculado,
   foco) antes de testar qualquer coisa.
4. **Olhar a tela.** Retângulos certos e janela desenhada a 64% já aconteceram juntos.
5. **Medir o custo de cada chamada ao shell** (~100 ms cada): sondar a capacidade uma vez e guardar.
6. **Migração única roda antes do ouvinte de preferências**, senão entra em laço de crash.
7. **Nunca matar o jogo do dono para testar.** O BACK num app em tela cheia abre "Quer sair?".

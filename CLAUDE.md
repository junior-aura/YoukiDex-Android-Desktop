# CLAUDE.md: ClauDEX (fork do YoukiDEX, branch `kit-debug`)

Launcher de modo desktop para Android. Leia antes de tudo `docs/claudex/README.md`, `ARQUITETURA.md`
(como funciona e por quê) e `TRANSICAO-TABLET.md` (o que conferir num aparelho novo).

## Repositório e build

- Trabalhe **só na `kit-debug`**, pela worktree `.claude/worktrees/kit-debug`. A `main` espelha o upstream.
- Não há toolchain local: **push dispara o CI** (`.github/workflows/kit-debug.yml`), que gera
  `claudex-release.apk` (`com.youki.dex.claudex`, o de uso) e `claudex-debug.apk` (`com.youki.dex.debug`,
  para medir), com `SHA256SUMS` e `SIGNER.txt`.
- **Chave estável** (secret `KIT_DEBUG_KEYSTORE`, certificado SHA-256 `0331f7c3…cb8671`). Confira hash e
  certificado do APK baixado antes de `adb install -r`.
- Mensagem de commit pelo arquivo (`git commit -F`). Uma fase = um commit + build conferido + teste no
  aparelho.

## Invariantes (cada uma já custou caro)

1. **Capacidade, não modelo.** Nada de ramificar por fabricante: sonde o comando, o id ou o inset, e caia
   para um caminho mais simples se faltar.
2. **Código novo em arquivo próprio** (`utils/`, `panels/`). `services/PerfectServer.kt` (~7000 linhas,
   herdado) só recebe a ligação.
3. **A verdade das tarefas vem do `am stack list` via Shizuku**, nunca de `getRunningTasks` (só vê o
   próprio app) nem dos retângulos da acessibilidade (recortados pela tela).
4. **Todo `am` custa ~100 ms.** Uma chamada por ação; sonde capacidade uma vez e guarde.
5. **Migração única roda antes de registrar o ouvinte de preferências**, senão laço de crash.
6. **Handler próprio por responsabilidade**: `removeCallbacksAndMessages(null)` num handler compartilhado
   cancela trabalho alheio.
7. **Janela sobreposta invisível não pode ser tocável**: `VISIBLE` com alpha 0 come toques.
8. **Não escrever configuração do sistema que persiste** (`wm set-ignore-orientation-request`, densidade)
   sem decisão do dono.

## Testar no aparelho

- Depois de instalar: o app subiu? (`pidof`, serviço de acessibilidade vinculado, foco). Depois de cada
  interação: `adb logcat -b crash -d`.
- **Diário**: `adb shell cat /sdcard/Android/data/com.youki.dex.claudex/files/claudex-eventos.log`.
  **Morte do processo**: `dumpsys activity exit-info com.youki.dex.claudex`.
- Gesto humano: `input motionevent DOWN/MOVE/UP` com pausa antes do UP, não `input swipe`.
- O Shizuku morre a cada reinício do adbd (`adb tcpip`, cabo, toggles de depuração). Religar:
  `adb shell '$(ls $(dirname $(pm path moe.shizuku.privileged.api | sed "s/package://"))/lib/*/libshizuku.so)'`.
  Para testar uma queda sem perder o adb pelo Wi-Fi: `kill $(pidof shizuku_server)`.
- O aparelho é do dono: nunca matar o app em uso para testar; BACK num jogo em tela cheia abre "Quer sair?".

## Estilo

Comentários em inglês, explicando **o que foi medido** e por que o código é assim. Docs em português.

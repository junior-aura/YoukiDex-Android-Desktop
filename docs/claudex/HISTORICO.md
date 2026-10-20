# ClauDEX: histórico (fase A05, 25/09 → 20/10/2026)

Registro, não guia. As lições que ainda valem estão em `ARQUITETURA.md` e `TRANSICAO-TABLET.md`.

## Rodadas e decisões do dono

| Data | Rodada | Decisões e resultados |
|---|---|---|
| 25/09 | Fork + CI | branch `kit-debug`, chave estável, build release (ativo) e debug (medição); ícones na densidade da tela |
| 25/09 | Modo **Automático** | janela nova ocupa o espaço livre; piso do topo aprendido |
| 28/09 | Diário, encaixe, alternador | Shizuku no boot; encaixe por capacidade; botões expandir e **lado a lado**; alternador próprio (o do sistema dava ANR) |
| 28/09 | Painéis | barra **enxuta** (44 dp), **só favoritos**, notificação **só selo**, painel rápido; gaveta medida e mantida |
| 29/09 | Briga de rotação | a sobreposição da barra deixou de pedir paisagem; o encaixe exige viagem do dedo |
| 29/09 | **Trilho lateral** | no retrato, à direita, com favoritos + Janelas + Opções (a barra cobria Voltar/Recentes) |
| 29/09 | Preencher | "proporção não importa, usabilidade sim": a janela ocupa o retângulo inteiro |
| 29/09 | Estudo no aparelho | faixa invisível que comia toques; lado a lado de 1,5 s para 0,12 s; barra sabe quem está aberto |
| 29/09 | Em janela | favorito aberto em tela cheia vira janela; **barra fixa não**: "menos barra, mais tela" |
| 30/09 | 1º dia de uso | janela → tela cheia pelo Maximizar da pílula; expandir e lado a lado convertem tela cheia |
| 05/10 | 1 semana | causa das quedas do Shizuku provada (cgroup do adbd); notificação contínua |
| 06/10 | Root? | **não**: Knox, Pokémon GO e atualizações custam mais do que ganharia; reabrir só no tablet |
| 20/10 | Fechamento | barra sobre a navegação de baixo pelo inset real; documentação; entrega |

## Commits (`main..kit-debug`, 46)

```
3cd82f8 25/09 kit-debug: side-by-side debuggable build + CI
0855a36 25/09 Load app icons at the screen density, not DENSITY_XXXHIGH
220801e 25/09 CI: sign debug builds with the stable key explicitly
aaa19bb 25/09 ClauDEX release variant, same stable key for both builds
7c75951 25/09 Shizuku/root grant-all: stop clobbering device-wide state
6db2c6b 25/09 On-demand dock: out of the way until called
2eef03a 25/09 Smart launch mode: new windows fit what is already on screen
b488d07 25/09 Smart launch: subtract reserved system bars, not just visible ones
f5e0106 25/09 Smart launch: learn the window manager's floor for freeform tops
344f422 25/09 Smart launch: learn the top floor on every launch, not via getRunningTasks
17ec9d0 25/09 Smart launch: match the clamp on edges, accessibility clips the height
bd6fc52 25/09 Smart launch: find the shifted window by geometry, and log the fit
063cc76 25/09 Post-launch checks on their own handler: hideDock was cancelling them
92cb541 25/09 Smart launch: portrait-locked apps get a portrait window that fits
ecd1463 28/09 Event journal, drag spy, and the force-resizable key that reboots erased
01c5dd7 28/09 Fix the rules that only worked "sometimes", ghost windows, idle-build prompt
0f13a33 28/09 Boot: judge the accessibility service by "configured", not "bound"
4ded6ea 28/09 Snap on drop by capability, and an expand button in the dock
ec9addb 28/09 Wire the expand button where the local animateBtn is in scope
883cad2 28/09 Snap: match the dragged window with a few px of tolerance
2048eef 28/09 Snap: judge the zone by the finger, and only offer quarters that fit
93e82b0 28/09 Snap: leave windows the system stashed or split alone
c8c9aac 28/09 Side-by-side button in the dock: tile the open windows by what fits
b329c1d 28/09 Launch check: find the task over Shizuku, never a false 'Something went wrong'
b32f7d3 28/09 ClauDEX task switcher on the dock's recents button
02bca1c 28/09 Switcher button in the dock's status pill
ec570d4 28/09 stackTasks: take the package from the base component, not topActivity
c5a5606 28/09 Side by side includes minimized windows and brings them back
2284203 28/09 Slim dock: 44 dp, favorites only, one Windows button, swipe up = fullscreen
b83b1ba 28/09 Fix the slim-dock crash loop; quick panel for levels and connections
79ff185 28/09 Keep the pill; slim-dock composition; badge-only notifications
cd5ad10 28/09 Dock icons follow the dock height; quick panel says how a connect ended
9c7459d 28/09 Badge only: own notification popup off; Alerts opens the shade reliably
62d73e7 28/09 Quick panel: fix the crash when it closed with Bluetooth proxies open
d9d9de0 29/09 Stop the orientation fight around fullscreen apps; snap needs real travel
29c2a9b 29/09 Portrait: a side rail on the right replaces the bottom dock
94272e1 29/09 Windows fill their rectangle; the post-launch fit reads the real task
b7f9a76 29/09 Remove an invisible touch-eating strip; floor, dock and rotation waste
31169a8 29/09 Resize a task in one Shizuku call; the keyboard is not the focused app
8a791e1 29/09 Snap and tile learn the freeform floor while it is unknown
c5ff6cd 29/09 Dock "Close" closes: find the tasks and remove them the way the ROM allows
0612582 29/09 Dock favorites know which ones are open
89e5590 29/09 Tapping an open fullscreen favorite turns it into a window; no swipe-pin
5df290b 30/09 Window to fullscreen through the window's own Maximize; fullscreen to window from expand/tile
e9a98a9 05/10 A lasting notice while Shizuku is down, also when it was down at start
8bfb546 20/10 Dock rests on a bottom navigation bar by its real inset, on every API
```

## Números de fechamento (A05, 05/10 → 20/10)

- 2558 eventos de uso em 15 dias (de 44 a 314 por dia); **0 crashes do ClauDEX** em todo o período.
- Shizuku disponível em **43%** do tempo de uso (97 h com ele, 130 h sem).
- CPU de 0,03 a 0,05% de um núcleo; PSS de 100 a 120 MB.

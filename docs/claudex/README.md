# ClauDEX

Launcher de **modo desktop** para Android: área de trabalho em paisagem, janelas livres que se arrumam
sozinhas, barra sob demanda e um trilho lateral no retrato. É um fork do
[YoukiDEX](https://github.com/junior-aura/YoukiDex-Android-Desktop) (branch `kit-debug`), compilado por
CI com chave estável.

Foi construído de 25/09 a 20/10/2026 num **Galaxy A05 (SM-A055M, One UI, Android 15)**, que é a bancada
de testes. O destino é um tablet (Multilaser M10 ou Positivo Tab10). Tudo é decidido **por capacidade, não
por modelo**: onde um recurso do sistema não existe, o ClauDEX cai para um caminho mais simples, sem quebrar.

## Instalar

| Arquivo | Pacote | Para quê |
|---|---|---|
| `claudex-release.apk` | `com.youki.dex.claudex` | o de uso: vira a tela inicial (HOME) |
| `claudex-debug.apk` | `com.youki.dex.debug` | só para medir (permite `run-as`); deixe inativo |

1. Confira o hash (`SHA256SUMS`) e o certificado (`SIGNER.txt`, SHA-256 `0331f7c3…cb8671`) antes de
   instalar. O mesmo certificado em todas as versões permite atualizar com `adb install -r` sem apagar
   dados.
2. Instale o **Shizuku**, inicie pela **Depuração sem fio** e ative "Iniciar no boot".
3. Abra o ClauDEX, dê as permissões que ele pedir: acessibilidade, sobreposição, notificações e Shizuku.
   No primeiro contato com o Shizuku ele grava sozinho as chaves de janela livre
   (`enable_freeform_support`, `force_resizable_activities`). **Em Android mais antigo, reinicie uma vez**
   para elas valerem.
4. Defina o ClauDEX como tela inicial.

## Usar

| Gesto / botão | Faz |
|---|---|
| Puxar a alça de baixo (paisagem) | chama a barra; ela some sozinha quando há janela na tela |
| Tocar num favorito fechado | abre no modo **Automático**: ocupa o espaço livre que houver |
| Tocar num favorito **aberto em janela** | traz a janela para a frente |
| Tocar num favorito **aberto em tela cheia** | vira **janela** (sai da tela cheia sem fechar) |
| Arrastar o ícone para **cima** | **tela cheia**; se já estava em janela, usa o Maximizar da própria janela |
| Deslizar a barra para cima | abre a gaveta de apps (a barra sob demanda **não** se fixa) |
| **Janelas** (□ na pílula) | alternador: trazer, fechar (X), **lado a lado**, **expandir** |
| Status (relógio/Wi-Fi/bateria) | painel rápido: brilho, volume, Wi-Fi, dados, Bluetooth e pareados |
| Ponto no favorito / no sino | notificação nova; o sino abre a cortina do Android |
| **Retrato**: alça fina na borda direita | trilho lateral com favoritos + Janelas + Opções, no lugar da barra |
| Arrastar uma janela até a borda | encaixe por capacidade: age só onde o sistema não toma a borda |

Se o Shizuku cair, uma notificação fica na cortina até ele voltar. O toque abre o Shizuku, e o
**Iniciar** dele religa tudo.

## Documentos

- [`ARQUITETURA.md`](ARQUITETURA.md): como funciona por dentro e por quê, com o que foi medido.
- [`TRANSICAO-TABLET.md`](TRANSICAO-TABLET.md): o que conferir no primeiro dia no tablet, e os riscos.
- [`HISTORICO.md`](HISTORICO.md): as rodadas, commit a commit, e as decisões do dono.

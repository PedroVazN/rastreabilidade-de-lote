# Rastreabilidade de Lote

Projeto 7 da TechInova. O aplicativo acompanha a passagem de lotes por etapas da fabricação e permite que a Qualidade consulte o histórico ou reprove um lote com motivo.

## Integrantes

- Pedro Vaz — [@PedroVazN](https://github.com/PedroVazN)
- Nathan Ferreira Silva — [@NathanSec](https://github.com/NathanSec)
- Marcio Petigrosso — [@petzada](https://github.com/petzada)

## Abrir no Android Studio

Abra a pasta que contém `settings.gradle`. Use o JDK do Android Studio para o Gradle, instale o Android SDK 36 e execute o módulo `app` em um aparelho ou emulador Android 11 ou superior. Para gerar o APK no Windows, rode `.\gradlew.bat assembleDebug`. O APK é criado em `app/build/outputs/apk/debug/app-debug.apk`.

## Estado da base

O app usa Java, layouts XML e ZXing. Após o login, o aplicativo mostra o perfil autorizado e abre a tela principal. A consulta do histórico acompanha o lote e a subcoleção `historico` no Firestore enquanto as demais tarefas do [backlog](DOCS/backlog.md) avançam.

O Operador de etapa cria o lote no Firestore. A etiqueta QR aparece após confirmação da gravação. O app recupera a última etiqueta daquele operador neste aparelho e acompanha o documento no Firestore.

- `MainActivity` conecta o formulário e a etiqueta.
- `model/Lote` representa o lote.
- `data/LotesLocais` guarda os registros da base local.
- `res/layout` define a interface.

## Firebase e contas de teste

1. No Firebase Console, registre um app Android com o pacote `br.edu.unisenai.rastreabilidade` no projeto da equipe. Copie o `google-services.json` para `app/` antes de sincronizar o Gradle. O arquivo é ignorado pelo Git.
2. Habilite o provedor de e-mail e senha e crie o Cloud Firestore em modo de produção.
3. Crie as duas contas de demonstração em Authentication. Para cada conta, crie no console o documento `usuarios/{uid}` com `perfil` do tipo string e valor `operador` ou `qualidade`.
4. Publique o conteúdo de `firestore.rules` no projeto correto pelo console ou, com o Firebase CLI autenticado, rode `firebase deploy --only firestore:rules --project ID_DO_PROJETO`.
5. Abra o app nas duas contas. O Operador de etapa vê o cadastro de lotes. A Qualidade pode abrir a consulta. Use **Sair** para trocar de conta.

Compartilhe as credenciais de demonstração pelo canal combinado com o professor. Contas recém-cadastradas ficam aguardando a atribuição do perfil pelo console.

### Dados compartilhados

- `usuarios/{uid}` contém `perfil` e pode ter `nome` para identificar o autor de eventos.
- `etapas/{etapaId}` contém `nome` e `ordem` (inteiro começando em 1).
- `lotes/{loteId}` contém `produto`, `quantidade`, `criadoEm` (timestamp do servidor), `criadoPor` (UID), `status` (`ativo` ou `reprovado`) e `etapaAtual` (0 antes da primeira etapa).
- `lotes/{loteId}/historico/{eventoId}` usa `tipo` (`etapa` ou `reprovacao`), `autorUid`, `autorNome` e `registradoEm` (timestamp do servidor). Eventos de etapa incluem `etapa` e `ordem`. Eventos de reprovação incluem `motivo`.
- A reprovação grava em um único batch `status: reprovado` no lote e o evento com ID fixo `historico/reprovacao`. Só a Qualidade pode fazer essa transição, e apenas em lote `ativo`. O `autorNome` precisa ser o `nome` de `usuarios/{uid}` ou, sem nome válido, `Qualidade`.

O QR contém o `loteId`. Para abrir a consulta a partir da leitura, use o extra `HistoricoActivity.EXTRA_LOTE`.

## Evidências e entrega

O [print do cadastro local](DOCS/tela-cadastro.png) mostra a base no emulador. Adicione os prints dos dois perfis após testar o MVP. O [checklist da base](DOCS/Checklist-CORE.docx) está em `DOCS/`, junto ao [roteiro da apresentação](DOCS/roteiro.md). Salve o PDF dos slides nessa pasta. Anexe o APK da versão final à entrega ou a uma release do GitHub. Registre aqui o link acessível do vídeo quando a demonstração estiver gravada.

## Trabalho em equipe

Use o [backlog](DOCS/backlog.md) para abrir uma issue por história. Passe cada issue de **A fazer** para **Fazendo** quando alguém assumir. Crie uma branch por issue, inclua `Closes #n` no corpo do PR e peça revisão de outro integrante. Mova para **Feito** depois de verificar os critérios. Todos os integrantes precisam abrir PRs próprios e revisar PRs de colegas.

O grupo informou três integrantes. O enunciado cita quatro, então a formação precisa ser confirmada com o professor.

# Rastreabilidade de Lote

Projeto 7 da TechInova. O aplicativo acompanha a passagem de lotes por etapas da fabricação e permite que a Qualidade consulte o histórico ou reprove um lote com motivo.

## Integrantes

- Pedro Vaz — [@PedroVazN](https://github.com/PedroVazN)
- Nathan Ferreira Silva — [@NathanSec](https://github.com/NathanSec)
- Marcio Petigrosso — [@petzada](https://github.com/petzada)

## Abrir no Android Studio

Abra a pasta que contém `settings.gradle`. Use o JDK do Android Studio para o Gradle, instale o Android SDK 36 e execute o módulo `app` em um aparelho ou emulador Android 11 ou superior. Para gerar o APK no Windows, rode `.\gradlew.bat assembleDebug`. O APK é criado em `app/build/outputs/apk/debug/app-debug.apk`.

## Estado da base

O cadastro grava produto, quantidade e horário no aparelho e gera um QR com o ID do lote. A última etiqueta reaparece ao reabrir o app. A versão inicial usa Java, layouts XML e ZXing. O fluxo de produção entre aparelhos depende das tarefas do [backlog](DOCS/backlog.md).

- `MainActivity` conecta o formulário e a etiqueta.
- `model/Lote` representa o lote.
- `data/LotesLocais` guarda os registros da base local.
- `res/layout` define a interface.

## Firebase e contas de teste

Ao integrar o Firebase, registre um app Android com o pacote `br.edu.unisenai.rastreabilidade`, habilite o provedor de e-mail e senha e configure o Cloud Firestore. Copie `google-services.json` para `app/` no seu computador. O arquivo está no `.gitignore`. A tarefa NC1–NC4 adicionará os plugins, SDKs e regras, além do guia de implantação.

Crie uma conta exclusiva de demonstração para cada perfil e atribua o perfil pelo documento `usuarios/{uid}` no console. Preencha nesta seção o e-mail das duas contas de teste quando estiverem disponíveis. Distribua as senhas de demonstração pelo canal combinado com o professor.

## Evidências e entrega

O [print do cadastro local](DOCS/tela-cadastro.png) mostra a base no emulador. Adicione os prints dos dois perfis após testar o MVP. O [checklist da base](DOCS/Checklist-CORE.docx) está em `DOCS/`, junto ao [roteiro da apresentação](DOCS/roteiro.md). Salve o PDF dos slides nessa pasta. Anexe o APK da versão final à entrega ou a uma release do GitHub. Registre aqui o link acessível do vídeo quando a demonstração estiver gravada.

## Trabalho em equipe

Use o [backlog](DOCS/backlog.md) para abrir uma issue por história. Passe cada issue de **A fazer** para **Fazendo** quando alguém assumir. Crie uma branch por issue, inclua `Closes #n` no corpo do PR e peça revisão de outro integrante. Mova para **Feito** depois de verificar os critérios. Todos os integrantes precisam abrir PRs próprios e revisar PRs de colegas.

O grupo informou três integrantes. O enunciado cita quatro, então a formação precisa ser confirmada com o professor.

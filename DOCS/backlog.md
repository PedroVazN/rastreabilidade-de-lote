# Histórias do Projeto 7

Distribuição inicial para três pessoas. Vinculem cada história a uma issue com seu responsável e a uma branch própria. A divisão pode mudar após o grupo confirmar os responsáveis. As seis histórias CORE cobrem ES1 e têm critérios no formato Dado / Quando / Então.

## BASE — Abrir a base Android e gerar uma etiqueta local

**Responsável sugerido** Pessoa A

Como integrante do grupo, quero abrir a base no Android Studio e conferir o cadastro local para começar a integração com o Firebase a partir de um fluxo executável.

- Dada a base aberta com o SDK configurado, quando eu compilar a variante debug, então o Gradle gera o APK.
- Dado um produto e uma quantidade válidos, quando eu criar um lote, então aparece uma etiqueta QR com o identificador do lote.

Esta issue cobre o PR inicial. C1 registra a integração pendente com o Firestore.

## C1 — Criar lote e gerar etiqueta QR

**Responsável sugerido** Pessoa A

Como operador de etapa, quero criar um lote para identificar as peças da produção.

- Dado um produto e uma quantidade inteira positiva, quando eu criar o lote, então o app grava produto, quantidade e data no Firestore e exibe um QR com o ID do lote.
- Dada uma quantidade vazia, inválida ou menor que um, quando eu enviar, então o app mostra a validação em português e não cria o lote.

Integrar o cadastro local da base ao Firestore. Usar data do servidor quando o documento for gravado.

## C2 — Cadastrar as etapas na ordem do processo

**Responsável sugerido** Pessoa B

Como integrante da Qualidade, quero cadastrar as etapas em ordem para definir o fluxo de produção.

- Dadas as etapas corte, solda, pintura e inspeção final, quando eu salvar a sequência, então os aparelhos carregam as etapas na mesma ordem.
- Dado um cadastro com nome vazio ou ordem duplicada, quando eu salvar, então o cadastro é recusado e a sequência válida permanece disponível.

Perfil responsável sugerido para o cadastro. Definir como lotes em andamento preservam sua sequência se o processo mudar.

## C3 — Ler o QR e concluir a próxima etapa

**Responsável sugerido** Pessoa B

Como operador de etapa, quero ler o QR do lote para registrar que concluí a próxima operação.

- Dado um lote ativo que passou pelo corte, quando eu ler o QR e confirmar a próxima etapa, então a solda é registrada com meu UID, nome e horário.
- Dado um QR com código inválido ou desconhecido, quando eu tentar abrir o lote, então o app exibe uma mensagem e mantém o Firestore intacto.

Depende de C1 e C2. Usar timestamp do servidor.

## C4 — Impedir etapas puladas ou duplicadas

**Responsável sugerido** Pessoa B

Como operador de etapa, quero que o app valide a ordem para manter a rastreabilidade confiável.

- Dado um lote que concluiu a solda, quando eu tentar registrar inspeção final antes da pintura, então a gravação é recusada.
- Dado que dois aparelhos confirmam a mesma próxima etapa, quando as tentativas chegam juntas, então apenas uma conclusão é gravada e o lote avança uma única vez.

Trabalhar com C3 e NC3. Validar a mudança e gravar o evento junto com o avanço do lote em uma transação.

## C5 — Consultar o histórico por QR ou código

**Responsável sugerido** Pessoa C

Como integrante da Qualidade, quero consultar o lote por QR ou código para investigar problemas na fabricação.

- Dado um lote que passou por duas etapas, quando eu consultar pelo código, então a linha do tempo mostra os eventos em ordem com autor e horário.
- Dado o QR de um lote reprovado, quando eu consultar seu histórico, então consigo ver as etapas anteriores e o motivo da reprovação.

Usar RecyclerView, estado vazio e listener no histórico.

## C6 — Reprovar o lote com motivo e bloquear registros

**Responsável sugerido** Pessoa C

Como integrante da Qualidade, quero reprovar um lote com motivo para impedir que ele continue na produção.

- Dado um lote ativo, quando a Qualidade informar um motivo e confirmar a reprovação, então o histórico exibe o motivo e o lote passa a reprovado.
- Dado um lote reprovado, quando um operador tentar concluir uma etapa, então as regras recusam a gravação e nenhum evento de conclusão é adicionado.

Dado um motivo vazio ou um usuário operador tentando reprovar, quando a operação for enviada, então ela é recusada.

Trabalhar com C4 e NC3 para tratar corrida entre reprovação e avanço.

## NC1–NC4 — Configurar Firebase, sessão, perfis e regras

**Responsável sugerido** Pessoa A

Como integrante da equipe, quero autenticação e permissões por perfil para usar o app com dados sincronizados.

- Dadas contas de Operador e de Qualidade, quando cada pessoa entrar ou reabrir o app, então a sessão persiste e a interface respeita o perfil de usuarios/{uid}.
- Dado um operador autenticado, quando tentar alterar o próprio perfil ou reprovar um lote, então as regras do Firestore recusam a operação.

Habilitar login/cadastro por e-mail, logout e tratamento de conta sem perfil. Atribuir perfis no console. Usar listeners nas listas principais e testar as regras.

## NC5–NC8 — Verificar listas, formulários e documentar configuração

**Responsável sugerido** Pessoa A

Como integrante do grupo, quero conferir a experiência nas telas e registrar como executar o projeto.

- Dado um formulário parcialmente preenchido, quando a tela for girada, então os valores digitados permanecem.
- Dada uma lista sem resultados, quando ela abrir, então a tela mostra o estado vazio. Dado um formulário inválido, quando for enviado, então a mensagem é em português.

Completar o README com os integrantes, prints reais, configuração do Firebase e contas de demonstração. Manter google-services.json fora do Git.

## ENTREGA — Preparar APK, slides, vídeo e checklist final

**Responsável sugerido** Pessoas A, B e C

Como integrante do grupo, quero reunir as evidências do MVP para a entrega e a apresentação.

- Dado o código revisado na main, quando a versão final for compilada, então o APK de debug corresponde ao commit e fica anexado à entrega ou ao GitHub Releases.
- Dado o ensaio com os dois perfis, quando o grupo concluir a preparação, então os slides em PDF e o checklist Word estão em DOCS e o link do vídeo abre para quem o recebe.

Gravar uma demonstração real e registrar os resultados do checklist. Usar até seis slides.

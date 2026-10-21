# TerriFlow — Configuração e Arquitetura

## 1. Firebase Console

1. Acesse https://console.firebase.google.com
2. Crie um projeto chamado "TerriFlow"
3. Adicione um app Android com package name: `com.mraphaelpy.terriflow`
4. Baixe o `google-services.json` e coloque em: `app/google-services.json`

## 2. Ativar serviços Firebase

No console Firebase, ative:
- **Authentication** → E-mail/senha e Google
- **Cloud Firestore** → Criar banco (modo produção)
- **Cloud Messaging** → Já habilitado por padrão

## 3. Regras e índices do Firestore

Sempre que `firestore.rules` ou `firestore.indexes.json` mudarem, implante com o Firebase CLI:
```
firebase deploy --only firestore:rules,firestore:indexes
```
ou cole o conteúdo de cada arquivo manualmente no console.

## 4. Primeiro acesso

1. Cadastre-se pelo app.
2. Na tela de congregação, escolha **Criar congregação**: quem cria vira `SUPER_ADMIN`
   e recebe o código de convite.
3. Os demais irmãos se cadastram e usam **Entrar com código**; entram como `RESPONSIBLE`.
4. Admins podem promover outros usuários na tela **Usuários**.

## 5. Estrutura do Firestore

```
users/{uid}                                   ponteiro global, gravado só pelo próprio usuário
  congregationId: string

congregations/{congId}
  name, code (convite), createdAt, createdBy (uid do criador)

congregations/{congId}/users/{uid}
  name, email, role (SUPER_ADMIN | ADMIN | RESPONSIBLE), active, photoUrl,
  congregationId, createdAt
  fcmTokens: string[]                         alterado só via arrayUnion/arrayRemove

congregations/{congId}/territories/{territoryId}
  code (T-00042 ou personalizado), name, description, location, notes,
  status (AVAILABLE | ASSIGNED | IN_PROGRESS | PAUSED | COMPLETED | RETURNED | ARCHIVED),
  currentResponsibleId/Name, pastResponsibleIds,
  createdAt, assignedAt, startedAt, completedAt, returnedAt, updatedAt,
  latitude, longitude, boundaryPoints, blockPolygons,
  completedBlocks (índices das quadras feitas no ciclo atual), syncVersion,
  serverUpdatedAt (horário do servidor, usado na sincronização incremental)

congregations/{congId}/territories/{territoryId}/events/{eventId}   imutáveis
  territoryId, userId, userName, type, timestamp, extra, congregationId, serverCreatedAt

congregations/{congId}/territories/{territoryId}/notes/{noteId}   só admins e dirigente atual
  kind (DO_NOT_CALL | FIELD_NOTE), address, text, authorId, authorName, createdAt

congregations/{congId}/campaigns/{campaignId}
  name, start, end, createdBy

congregations/{congId}/notifications/{notificationId}
  userId (destinatário), title, body, type, territoryId, territoryCode, read, createdAt
```

## 6. Arquitetura

Clean Architecture com Hilt, em três camadas. As dependências apontam sempre para o domínio.

```
presentation/   Telas Compose + ViewModels. Só conhece interfaces e casos de uso do domínio.
domain/         Kotlin puro (sem Android/Firebase), coberto por testes unitários.
  model/          Entidades, DomainException/AuthException, TerritoryCode
  policy/         TerritoryActionPolicy: quem pode fazer cada ação e em qual status
  repository/     Interfaces dos repositórios
  service/        Interfaces de serviços de plataforma (SystemNotifier)
  usecase/        Regras de negócio (atribuir, iniciar, finalizar, devolver, importar...)
data/           Implementações: Room (local), Firestore (remoto) e mesclagem entre eles
di/             Módulos Hilt (bindings de interfaces, escopo de aplicação)
sync/           Workers e listeners em tempo real
core/           Utilitários (KML, coordenadas, imagem, atualizador do app)
```

Regras práticas para novas funcionalidades:
- **Regra de negócio** vai em um caso de uso; a UI não decide permissões.
  Para ações em território, use `TerritoryActionGuard.authorize(...)` e, para exibir
  botões, `TerritoryActionPolicy.allowedActions(...)` — as duas usam a mesma regra.
- **Erros para o usuário** são `DomainException`/`AuthException` com mensagem pronta;
  o ViewModel apenas exibe `e.message`.
- **Acesso ao Firebase** só na camada `data/`, sempre recebido por injeção.
- **Eventos antes do território**: ao alterar o responsável, grave o evento antes do
  território; as regras só aceitam eventos de um não-admin enquanto ele é o responsável.

## 7. Estratégia de sincronização

- **Offline-first**: toda escrita vai primeiro para o Room; a UI observa o Room.
- **Envio**: o `RemoteWriter` emite a gravação no Firestore em segundo plano (o próprio
  Firestore a enfileira enquanto estiver offline). Registros sem confirmação ficam com
  `synced = false` e o `SyncWorker` (a cada 1 hora, com internet) os reenvia.
- **Recebimento**: o `RealtimeSyncManager` mantém listeners de territórios, notificações
  e eventos recentes enquanto houver usuário logado, reiniciando a cada login/logout.
- **Conflitos**: alterações locais pendentes nunca são sobrescritas; fora isso, a versão
  remota vence quando `syncVersion` for igual ou maior (`TerritoryRemoteMerger`).
- **Eventos**: imutáveis no servidor; antes de reenviar um pendente verifica-se se já existe.
- **Incremental**: cada gravação leva um horário do servidor (`serverUpdatedAt` /
  `serverCreatedAt`) e as sincronizações buscam só o que mudou desde a última vez
  (`SyncStateStore`). Uma cópia completa é feita uma vez por dia (territórios e usuários)
  ou por semana (eventos e notificações) para detectar exclusões.

## 8. Relatórios e acompanhamento

- **Ano de serviço**: de 1º de setembro a 31 de agosto (`ServiceYear`).
- **Designações** são reconstruídas a partir dos eventos (`AssignmentHistory`) e alimentam
  a meta de cobertura, o alerta de 12 meses, os tempos médios e o **S-13** (PDF/CSV).
- **Auditoria** e **Cobertura** leem o histórico do banco local, mantido pela sincronização incremental.
- **Lembretes** ao dirigente: 7 e 30 dias sem iniciar; 2 e 4 meses com o território.
  Admins podem "Cobrar devolução" a qualquer momento.
- **Limite sugerido** de territórios ativos por dirigente: `TerritoryRotationHelper.MAX_ACTIVE_PER_RESPONSIBLE`.

## 9. Testes

```
./gradlew testDebugUnitTest
```

O `AppDatabaseMigrationTest` (Robolectric) recria o banco em cada versão antiga e aplica
as migrações. Ao mudar uma entidade do Room, suba a versão, crie a migração, adicione-a em
`AppDatabase.ALL_MIGRATIONS` e registre as colunas novas em `COLUMNS_ADDED` no teste.

## 10. Permissão de notificações (Android 13+)

O app solicita `POST_NOTIFICATIONS` automaticamente na primeira execução.

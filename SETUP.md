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
  currentResponsibleId/Name/PhotoUrl, pastResponsibleIds,
  createdAt, assignedAt, startedAt, completedAt, returnedAt, updatedAt,
  latitude, longitude, boundaryPoints, blockPolygons, syncVersion

congregations/{congId}/territories/{territoryId}/events/{eventId}   imutáveis
  territoryId, userId, userName, type, timestamp, extra, congregationId

congregations/{congId}/notifications/{notificationId}
  userId ("" = toda a congregação), title, body, type, territoryId, territoryCode,
  read, createdAt
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
  `synced = false` e o `SyncWorker` (a cada 15 min, com internet) os reenvia.
- **Recebimento**: o `RealtimeSyncManager` mantém listeners de territórios, notificações
  e eventos recentes enquanto houver usuário logado, reiniciando a cada login/logout.
- **Conflitos**: alterações locais pendentes nunca são sobrescritas; fora isso, a versão
  remota vence quando `syncVersion` for igual ou maior (`TerritoryRemoteMerger`).
- **Eventos**: imutáveis no servidor; antes de reenviar um pendente verifica-se se já existe.

## 8. Testes

```
./gradlew testDebugUnitTest
```

## 9. Permissão de notificações (Android 13+)

O app solicita `POST_NOTIFICATIONS` automaticamente na primeira execução.

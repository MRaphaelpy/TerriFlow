# TerriFlow — Configuração Inicial

## 1. Firebase Console

1. Acesse https://console.firebase.google.com
2. Crie um projeto chamado "TerriFlow"
3. Adicione um app Android com package name: `com.mraphaelpy.terriflow`
4. Baixe o `google-services.json` e coloque em: `app/google-services.json`

## 2. Ativar serviços Firebase

No console Firebase, ative:
- **Authentication** → E-mail/senha
- **Cloud Firestore** → Criar banco (modo produção)
- **Cloud Messaging** → Já habilitado por padrão

## 3. Firestore Security Rules

Cole o conteúdo de `firestore.rules` no console do Firestore → Regras.

## 4. Firestore Indexes

Implante os índices com Firebase CLI:
```
firebase deploy --only firestore:indexes
```
ou cole o conteúdo de `firestore.indexes.json` manualmente no console.

## 5. Criar primeiro usuário ADMIN

Após registrar o primeiro usuário pelo app, acesse o Firestore no console e altere manualmente:
```
users/{userId}/role = "ADMIN"
```

## 6. Estrutura Firestore

```
users/{userId}
  name: string
  email: string
  role: "ADMIN" | "RESPONSIBLE"
  createdAt: timestamp
  active: boolean
  fcmTokens: string[]

territories/{territoryId}
  code: string         (ex: T-00042, imutável)
  name: string
  description: string
  location: string
  notes: string
  status: string       (AVAILABLE | ASSIGNED | IN_PROGRESS | PAUSED | COMPLETED | RETURNED | ARCHIVED)
  currentResponsibleId: string | null
  currentResponsibleName: string | null
  createdAt: timestamp
  assignedAt: timestamp | null
  startedAt: timestamp | null
  completedAt: timestamp | null
  returnedAt: timestamp | null
  updatedAt: timestamp
  syncVersion: number

territories/{territoryId}/events/{eventId}
  territoryId: string
  userId: string
  userName: string
  type: string         (CREATED | ASSIGNED | STARTED | PAUSED | RESUMED | COMPLETED | RETURNED | TRANSFERRED | UPDATED | ARCHIVED)
  timestamp: timestamp
  extra: map<string, string>

notifications/{notificationId}
  userId: string
  title: string
  body: string
  type: string
  territoryId: string | null
  territoryCode: string | null
  read: boolean
  createdAt: timestamp
```

## 7. Estratégia de Sincronização

**Offline-first**: Todas as escritas vão primeiro para o Room (local).
**Sync para nuvem**: WorkManager executa a cada 15 minutos quando há internet.
**Resolução de conflitos**: `syncVersion` determina quem vence — versão maior prevalece. Se local está não-sincronizado e remoto tem versão maior, remoto vence (assumindo que o servidor é fonte da verdade para dados de outros usuários). Alterações locais pendentes têm prioridade sobre dados remotos do mesmo ID.
**Idempotência de eventos**: Verificação de existência antes de inserir evita duplicação.

## 8. Permissão de notificações (Android 13+)

O app solicita permissão `POST_NOTIFICATIONS` automaticamente. Certifique-se de aceitá-la na primeira execução.

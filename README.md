# TerriFlow

Aplicativo Android para organizar os territórios de pregação de uma congregação: quem está com cada território, desde quando, quais quadras já foram feitas e quais territórios estão há mais tempo sem ser trabalhados.

Funciona offline, sincroniza entre os aparelhos pelo Firebase e se atualiza sozinho a partir das releases do GitHub.

## Funcionalidades

**Territórios**
- Cadastro com nome, descrição, endereço, pino no mapa e contorno desenhado
- Importação de KML/KMZ do Google My Maps: cada polígono vira um território e as quadras são reconhecidas automaticamente
- Fluxo completo: disponível → atribuído → em andamento → pausado → finalizado/devolvido
- Progresso por quadra no ciclo atual
- Anotações de campo e endereços "não visitar", visíveis só para admins e para o dirigente atual
- Histórico de cada território agrupado por dirigente

**Mapa**
- Todos os territórios com uma cor única cada, número e dirigente
- Números das quadras sobre o desenho nos territórios do próprio usuário
- Localização com direção (bússola), distância até o território e botão "Como chegar"
- Abre direto na região da congregação

**Acompanhamento**
- Painel inicial com resumo por status e os territórios do usuário
- Cobertura do ano de serviço (1º de setembro a 31 de agosto), territórios há mais de 12 meses sem ser trabalhados e tempo médio por dirigente
- Sugestão de rodízio ao atribuir: quem ainda não trabalhou ali e quem está com poucos territórios
- Auditoria de todas as ações, com filtros por pessoa, tipo e período
- Relatório S-13 em PDF e CSV
- Campanhas especiais com período definido

**Pessoas e avisos**
- Congregações isoladas entre si, com entrada por código de convite
- Perfis: Super Administrador, Administrador e Responsável
- Notificações quando um território é atribuído ou transferido e lembretes de territórios parados (7 e 30 dias sem iniciar; 2 e 4 meses com o dirigente)

## Tecnologias

| Área | Uso |
|---|---|
| Linguagem | Kotlin 2.1 |
| Interface | Jetpack Compose, Material 3 Expressive |
| Arquitetura | Clean Architecture (domain / data / presentation), Hilt |
| Dados locais | Room (offline-first), DataStore |
| Nuvem | Firebase Auth, Cloud Firestore, Cloud Messaging, Crashlytics |
| Segundo plano | WorkManager |
| Mapas | osmdroid (OpenStreetMap, sem chave de API) |
| Testes | JUnit, MockK, Robolectric, kotlinx-coroutines-test |

Android 8.0 (API 26) ou superior.

## Como rodar

Requisitos: Android Studio recente e JDK 17.

```bash
git clone https://github.com/MRaphaelpy/TerriFlow.git
cd TerriFlow
./gradlew installDebug
```

O `app/google-services.json` do projeto já está no repositório. Para usar seu próprio projeto Firebase, siga o [SETUP.md](SETUP.md): ele explica como criar o projeto, ativar os serviços e publicar as regras e os índices do Firestore.

### Testes

```bash
./gradlew testDebugUnitTest
```

Os testes cobrem as regras de negócio do domínio, as permissões por perfil, a sincronização e todas as migrações do banco local.

## Primeiro acesso

1. Crie uma conta no app.
2. Escolha **Criar congregação**. Quem cria vira Super Administrador e recebe o código de convite.
3. Os demais irmãos criam a conta e usam **Entrar com código**.
4. Na tela **Usuários**, os administradores definem o perfil de cada um.

## Estrutura do código

```
app/src/main/java/com/mraphaelpy/terriflow/
├── domain/        regras de negócio em Kotlin puro: modelos, políticas, casos de uso
├── data/          Room, Firestore e a mesclagem entre os dois
├── presentation/  telas Compose e ViewModels
├── sync/          sincronização em tempo real e em segundo plano
├── di/            módulos do Hilt
└── core/          utilitários: KML, coordenadas, relatórios, atualizador
```

Os detalhes da arquitetura, do modelo de dados no Firestore e da estratégia de sincronização estão no [SETUP.md](SETUP.md).

## Versões e atualização

Cada push na branch `main` roda os testes, gera o APK assinado e publica uma release no GitHub. O app consulta a última release, avisa quando há versão nova, baixa o APK e confere o SHA-256 antes de instalar.

Mudanças só em documentação (`*.md`) ou nas regras do Firestore não geram release.

## Autor

Desenvolvido por [Marcos Rafael](https://github.com/MRaphaelpy).

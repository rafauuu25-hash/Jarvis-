# JARVIS — Assistente de voz para Android + Servidor Windows

Aplicativo Android (Kotlin) com visual futurista que funciona como um
assistente de voz, capaz de se comunicar com um servidor local rodando
em um computador Windows, pela mesma rede Wi-Fi.

```
CELULAR ANDROID  →  WI-FI  →  SERVIDOR JARVIS NO WINDOWS
```

> Este projeto é original: nenhum nome, logotipo ou elemento visual de
> personagens fictícios protegidos foi utilizado. O visual é uma
> interface própria, com tema escuro e detalhes em ciano, inspirada de
> forma genérica em painéis tecnológicos de ficção científica.

## Estrutura do projeto

```
JARVIS/
├── app/                        → aplicativo Android (Kotlin)
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/jarvis/assistant/
│       │   ├── MainActivity.kt        → tela principal e voz
│       │   ├── SettingsActivity.kt    → tela de configurações
│       │   ├── CommandInterpreter.kt  → interpreta comandos falados/digitados
│       │   ├── NetworkClient.kt       → comunicação HTTP com o PC (OkHttp)
│       │   └── Prefs.kt               → armazenamento local (SharedPreferences)
│       └── res/                       → layouts, cores, strings, ícones
├── pc/
│   ├── jarvis_server.py        → servidor Python para Windows
│   └── README_PC.md            → instruções detalhadas do servidor
├── build.gradle / settings.gradle / gradle.properties
├── gradlew / gradlew.bat       → Gradle Wrapper
└── README.md                   → este arquivo
```

## Funcionalidades

- Reconhecimento de voz nativo do Android (`SpeechRecognizer`), em pt-BR.
- Resposta falada com Text-to-Speech nativo do Android.
- Campo de texto para digitar comandos manualmente.
- Indicador visual de "ouvindo" e de conexão com o PC.
- Comunicação HTTP local (OkHttp) com autenticação por token.
- Lista fechada de comandos seguros — o app nunca envia texto livre
  como comando de shell para o PC.
- Tela de configurações para IP, porta, token, voz e idioma.
- Servidor Python (biblioteca padrão, sem dependências externas) que
  só executa ações pré-programadas.

## Como gerar o APK (app-debug.apk)

### Pré-requisitos

- **Android Studio** (recomendado) ou o **Android SDK** com `sdkmanager`
  instalados, além do **JDK 17**.
- Conexão com a internet na primeira compilação, para o Gradle baixar
  as dependências do projeto (AndroidX, Material, OkHttp etc.).

### ⚠️ Sobre o Gradle Wrapper

Este projeto inclui `gradlew`, `gradlew.bat` e
`gradle/wrapper/gradle-wrapper.properties`, mas **não inclui o binário
`gradle-wrapper.jar`** (ele precisa ser baixado da internet e este
ambiente de geração não tinha acesso à rede). Antes de compilar, escolha
uma das opções abaixo:

**Opção A — Abrir no Android Studio (mais simples)**
1. Abra a pasta `JARVIS/` em *"Open an existing project"* no Android Studio.
2. O Android Studio detecta o wrapper incompleto e oferece para
   regenerá-lo/sincronizar automaticamente (ou vá em
   *File → Sync Project with Gradle Files*).
3. Depois de sincronizar, use *Build → Build Bundle(s) / APK(s) → Build APK(s)*.

**Opção B — Gerar o wrapper manualmente (se você já tem o Gradle instalado)**
```
gradle wrapper --gradle-version 8.4
```
Isso vai criar o `gradle-wrapper.jar` correto dentro de `gradle/wrapper/`.
Depois disso, o comando abaixo funciona normalmente.

### Compilar via linha de comando

Depois de ter o wrapper completo (Opção A ou B acima):

```
cd JARVIS
./gradlew assembleDebug        # Linux/macOS
gradlew.bat assembleDebug      # Windows
```

O APK gerado ficará em:

```
app/build/outputs/apk/debug/app-debug.apk
```

## Como instalar no Android

1. Copie o `app-debug.apk` para o celular (cabo USB, link de nuvem, etc.).
2. No celular, permita "Instalar apps de fontes desconhecidas" para o
   gerenciador de arquivos usado.
3. Toque no arquivo `app-debug.apk` para instalar.
4. Abra o aplicativo **JARVIS**.

## Configuração inicial no app

1. Toque no ícone de engrenagem (Configurações).
2. Informe o **IP do PC** (ex: `192.168.1.20`), a **porta** (`8765`) e o
   **token** (deve ser igual ao definido em `pc/jarvis_server.py`).
3. Toque em **"Testar conexão"**.
4. Volte à tela principal e toque no botão de microfone para falar um comando.

Veja o guia completo do servidor em [`pc/README_PC.md`](pc/README_PC.md).

## Comandos disponíveis (v1.0)

| Comando falado                      | Tipo   | Ação                                  |
|--------------------------------------|--------|----------------------------------------|
| "JARVIS, abra o YouTube"             | PC     | Abre o YouTube no navegador do PC      |
| "JARVIS, abra o Google"              | PC     | Abre o Google no navegador do PC       |
| "JARVIS, abra a calculadora"         | PC     | Abre a Calculadora do Windows          |
| "JARVIS, abra o bloco de notas"      | PC     | Abre o Bloco de Notas                  |
| "JARVIS, abra o explorador de arquivos" | PC  | Abre o Explorador de Arquivos          |
| "JARVIS, qual é a hora?"             | Local  | Responde a hora atual do celular       |
| "que dia é hoje"                     | Local  | Responde a data atual do celular       |
| "abrir configurações"                | App    | Abre a tela de configurações           |
| "testar conexão"                     | App    | Testa a conexão com o PC               |

## Segurança

- Toda comunicação com o PC exige um **token** enviado no cabeçalho
  `X-Jarvis-Token`. Requisições sem o token correto são rejeitadas.
- O servidor só executa uma lista fechada de comandos pré-programados —
  nunca comandos de shell arbitrários vindos do celular.
- O servidor aceita apenas conexões vindas da faixa de IP da rede local
  configurada. **Não exponha a porta 8765 à internet.**
- Nenhum token ou senha real está fixado no código — os valores de
  exemplo (`jarvis_local_token`) devem ser trocados pelo usuário antes
  do uso, tanto no app quanto no servidor.

## Próximos passos (roadmap sugerido)

O projeto já está preparado, na arquitetura de rede e no servidor, para
receber novos comandos autorizados no futuro — basta adicionar uma nova
função em `COMANDOS_PERMITIDOS` no `jarvis_server.py` e um novo
`RemoteCommand` correspondente em `CommandInterpreter.kt`.

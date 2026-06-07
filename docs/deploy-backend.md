# Deploy do Backend Ayanami Learn

Este projeto esta preparado para deploy no Render usando Docker e `render.yaml`.

## 1. Publicar o codigo no GitHub

Repo alvo:

```powershell
https://github.com/EduhxH/ayanami-learn.git
```

O arquivo `.env` nao deve ser enviado para o GitHub. Use `.env.example` apenas como referencia.

## 2. Criar o servico no Render

1. Acesse o Render Dashboard.
2. Crie um novo Blueprint.
3. Conecte o repo `EduhxH/ayanami-learn`.
4. Confirme o arquivo `render.yaml`.
5. Preencha as variaveis marcadas como secretas.

Variaveis obrigatorias:

```text
MONGO_URI
FIREBASE_WEB_API_KEY
FIREBASE_SERVICE_ACCOUNT_JSON
GROQ_API_KEY
GEMINI_API_KEY
PUBLIC_BASE_URL
```

Variaveis opcionais, usadas para TTS fora do Gemini Live:

```text
CARTESIA_API_KEY
```

`PUBLIC_BASE_URL` deve ser a URL publica do proprio servico Render, por exemplo:

```text
https://ayanami-learn-backend.onrender.com
```

## 3. Firebase Admin

No Render, prefira colar o JSON inteiro da service account em:

```text
FIREBASE_SERVICE_ACCOUNT_JSON
```

Nao use `FIREBASE_SERVICE_ACCOUNT_PATH` em producao, porque o arquivo local nao existe dentro do container.

## 4. MongoDB Atlas

Se o MongoDB Atlas bloquear a conexao do Render, va em Network Access.

Para projeto escolar, o caminho mais simples e permitir:

```text
0.0.0.0/0
```

Para producao real, use uma estrategia com IP fixo/allowlist mais restrita.

## 5. Testar o backend

Depois do deploy, abra:

```text
https://SEU-SERVICO.onrender.com/health
```

Resposta esperada:

```text
OK
```

Depois teste:

```text
https://SEU-SERVICO.onrender.com/
```

Resposta esperada:

```text
Ayanami Learn Backend Online!
```

## 6. Compilar o APK apontando para producao

Quando tiver a URL do Render:

```powershell
.\gradlew.bat :app:assembleDebug -PAYANAMI_API_BASE_URL=https://SEU-SERVICO.onrender.com
```

Para release:

```powershell
.\gradlew.bat :app:assembleRelease -PAYANAMI_API_BASE_URL=https://SEU-SERVICO.onrender.com
```

O WebSocket sera derivado automaticamente:

```text
https://... -> wss://...
http://...  -> ws://...
```

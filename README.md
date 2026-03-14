# Mobile API Inspector Plugin

IntelliJ IDEA uchun Android, iOS va Web request/response ko‘rish paneli.

Imkoniyatlar:

- chapda searchable request list
- chapda host + path ko‘rinishidagi endpoint list
- o‘ngda detail view
- formatted JSON
- copy raw
- copy cURL
- local ingest server
- browser uchun CORS support

Web uchun ishlatish:

```js
import installApiInspector from './web-sdk/api-inspector-web-sdk.js'

installApiInspector()
```

Shundan keyin `fetch` va `XMLHttpRequest` requestlari `API Inspector` paneliga tushadi.

Lokal build:

```bash
cd /Users/ruzibekov/Projects/mobile-api-inspector-plugin
./gradlew buildPlugin
```

Lokal IntelliJ install’dan foydalanish kerak bo‘lsa:

```bash
cd /Users/ruzibekov/Projects/mobile-api-inspector-plugin
IDEA_LOCAL_PATH="/Applications/IntelliJ IDEA.app" ./gradlew buildPlugin
```

Plugin zip:

```bash
build/distributions/mobile-api-inspector-plugin-0.1.0.zip
```

Marketplace-ready publish:

```bash
cd /Users/ruzibekov/Projects/mobile-api-inspector-plugin
export IDEA_VERSION=2025.3.3
export PUBLISH_TOKEN=REDACTED
export CERTIFICATE_CHAIN=REDACTED
export PRIVATE_KEY=REDACTED
export PRIVATE_KEY_PASSWORD=REDACTED
export PUBLISH_CHANNEL=default
./gradlew test verifyPluginProjectConfiguration verifyPluginStructure buildPlugin signPlugin publishPlugin
```

Kerakli secret nomlari:

- `JETBRAINS_MARKETPLACE_TOKEN`
- `JETBRAINS_PLUGIN_CERTIFICATE_CHAIN`
- `JETBRAINS_PLUGIN_PRIVATE_KEY`
- `JETBRAINS_PLUGIN_PRIVATE_KEY_PASSWORD`

GitHub workflow:

- `.github/workflows/mobile-api-inspector-release.yml`

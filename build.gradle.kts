plugins {
  kotlin("jvm") version "2.1.21"
  id("org.jetbrains.intellij.platform") version "2.10.2"
}

group = "com.ruzibekov"
version = "0.2.0"

repositories {
  mavenCentral()
  intellijPlatform {
    defaultRepositories()
  }
}

val localIdeaPath = providers.environmentVariable("IDEA_LOCAL_PATH")
val ideaVersion = providers.environmentVariable("IDEA_VERSION")
  .orElse("2025.3.3")
val publishChannel = providers.environmentVariable("PUBLISH_CHANNEL")
  .orElse("default")

dependencies {
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
  testImplementation(kotlin("test"))
  testImplementation("junit:junit:4.13.2")

  intellijPlatform {
    val customIdeaPath = localIdeaPath.orNull
    if (!customIdeaPath.isNullOrBlank()) {
      local(customIdeaPath)
    } else {
      intellijIdea(ideaVersion)
    }
    bundledPlugin("com.intellij.java")
  }
}

kotlin {
  jvmToolchain(21)
}

intellijPlatform {
  buildSearchableOptions = false

  pluginConfiguration {
    id = "com.ruzibekov.api-inspector"
    name = "API Inspector"
    version = project.version.toString()
    description = """
      <p>Inspect Android, iOS, and Web API traffic directly inside IntelliJ IDEA.</p>
      <p>API Inspector adds a dedicated tool window for browsing captured requests, reviewing formatted JSON payloads, and copying raw payloads or cURL commands without leaving the IDE.</p>
      <ul>
        <li>Searchable request list with host and path visibility</li>
        <li>Formatted request and response details</li>
        <li>Copy raw payload and generated cURL</li>
        <li>Auto port discovery for local mobile integrations</li>
        <li>Filtering, pinning, and pause capture controls</li>
        <li>Web SDK support for fetch and XMLHttpRequest capture</li>
      </ul>
    """.trimIndent()
    changeNotes = """
      <ul>
        <li>Initial public release</li>
        <li>Android, iOS, and Web API inspection support</li>
        <li>Search, filtering, pinning, and pause capture workflows</li>
        <li>Formatted JSON details with raw and cURL copy actions</li>
      </ul>
    """.trimIndent()

    ideaVersion {
      sinceBuild = "253"
    }

    vendor {
      name = "Ruzibekov"
    }
  }

  pluginVerification {
    ides {
      recommended()
    }
  }

  signing {
    certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
    privateKey = providers.environmentVariable("PRIVATE_KEY")
    password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
  }

  publishing {
    token = providers.environmentVariable("PUBLISH_TOKEN")
    channels = publishChannel.map { listOf(it) }
  }
}

tasks {
  test {
    useJUnit()
  }

  publishPlugin {
    dependsOn(buildPlugin)
  }
}

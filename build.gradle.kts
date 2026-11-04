import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.4.21"
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

kotlin { jvmToolchain(21) }

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    intellijPlatform {
        rider(providers.gradleProperty("riderVersion")) { useInstaller = false }
        bundledPlugin("com.intellij.modules.jcef")
        bundledModule("intellij.platform.ui.jcef")
        bundledModule("intellij.libraries.jcef")
        jetbrainsRuntime()
    }
    testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.13.4")
    testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion { sinceBuild = "262" }
    }
    buildSearchableOptions = false
}

tasks.test { useJUnitPlatform() }

// ---- The designer host (.NET) and the webview, bundled into the plugin ------------------------------

val dotnet: String = providers.gradleProperty("dotnetPath").orNull
    ?: listOf(System.getenv("DOTNET_ROOT")?.let { "$it/dotnet" }, "/opt/dotnet/dotnet", "/usr/share/dotnet/dotnet", "/usr/lib/dotnet/dotnet")
        .firstOrNull { it != null && file(it).canExecute() } ?: "dotnet"

val applyNetFormsPatches by tasks.registering(Exec::class) {
    description = "Applies upstream-patches/*.patch to the NetForms submodule (once)."
    inputs.files(fileTree("upstream-patches"))
    commandLine("bash", file("host/apply-patches.sh").absolutePath)
}

val hostOut = layout.buildDirectory.dir("host/linux-x64")

val publishHost by tasks.registering(Exec::class) {
    description = "Builds Formwork.Host (NetForms' designer host + Formwork's buffer bridge) for Linux x64."
    dependsOn(applyNetFormsPatches)
    inputs.files(fileTree("host") { exclude("**/bin/**", "**/obj/**") })
    inputs.files(fileTree("external/NetForms/src") { exclude("**/bin/**", "**/obj/**") })
    outputs.dir(hostOut)
    environment("DOTNET_CLI_TELEMETRY_OPTOUT", "1")
    environment("DOTNET_NOLOGO", "1")
    commandLine(
        dotnet, "publish", file("host/Formwork.Host/Formwork.Host.csproj").absolutePath,
        "-c", "Release", "-r", "linux-x64", "--self-contained", "false",
        "-p:DebugType=none", "-p:GenerateDocumentationFile=false",
        "-o", hostOut.get().asFile.absolutePath,
    )
}

tasks.processResources {
    // NetForms' designer webview (MIT), served to JCEF from the plugin's resources.
    from("external/NetForms/designer/media") { into("formwork/webview/netforms") }
    from("external/NetForms/LICENSE") { into("formwork/licenses/NetForms") }
    from("external/NetForms/THIRD-PARTY-NOTICES.md") { into("formwork/licenses/NetForms") }
}

tasks.withType<org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask>().configureEach {
    dependsOn(publishHost)
    from(hostOut) { into(pluginName.map { "$it/host" }) }
}

// ./gradlew runIde -PopenPath=/path/to/project   (opens that folder or solution in the sandbox IDE)
tasks.named<org.jetbrains.intellij.platform.gradle.tasks.RunIdeTask>("runIde") {
    providers.gradleProperty("openPath").orNull?.let { args(it) }
    jvmArgumentProviders += CommandLineArgumentProvider {
        listOf("-Didea.trust.all.projects=false", "-Dide.show.tips.on.startup.default.value=false", "-Djb.consents.confirmation.enabled=false", "-Djb.privacy.policy.text=<!--999.999-->")
    }
}

// The sandbox Rider needs a JetBrains account login for its licence, which Formwork's tests must not do.
// The plugin only uses the shared IntelliJ Platform for its UI, so UI screenshots run in the free
// IntelliJ IDEA Community of the same platform version (2026.2). See TESTING-FOR-JACOB.md for the
// checks only Rider can do.
//   ./gradlew runIdea -PopenPath=/path/to/project
intellijPlatformTesting {
    runIde {
        register("runIdea") {
            type = IntelliJPlatformType.IntellijIdea
            version = providers.gradleProperty("ideaVersion")
            task {
                providers.gradleProperty("openPath").orNull?.let { args(it) }
                jvmArgumentProviders += CommandLineArgumentProvider {
                    listOf("-Dide.show.tips.on.startup.default.value=false", "-Djb.consents.confirmation.enabled=false", "-Djb.privacy.policy.text=<!--999.999-->", "-Didea.trust.all.projects=true",
                        // This test machine runs as root, where JCEF's sandbox can't start. Test runs only.
                        "-Dide.browser.jcef.sandbox.enable=false", "-Dide.browser.jcef.run-under-superuser.allowed=true")
                }
            }
        }
    }
}

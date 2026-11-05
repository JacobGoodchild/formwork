
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

/** The platform branch the build compiles against (261 = 2026.1, 262 = 2026.2), from the IDE chosen above. */
fun platformBranch(): Int {
    providers.gradleProperty("localIde").orNull?.let { dir ->
        val build = file("$dir/build.txt").takeIf { it.exists() }?.readText()?.trim() ?: return 262
        return build.substringAfter('-').substringBefore('.').toIntOrNull() ?: 262
    }
    val version = if (providers.gradleProperty("buildAgainst").orNull == "rider") providers.gradleProperty("riderVersion").get()
                  else providers.gradleProperty("ideaVersion").get()
    val (year, minor) = version.split('.').let { it[0].toInt() to it[1].toInt() }
    return (year - 2000) * 10 + minor
}

dependencies {
    intellijPlatform {
        // Formwork uses only the shared IntelliJ Platform (plus JCEF), so it compiles the same against any 2026.1 or
        // 2026.2 IDE. By default it builds against IntelliJ IDEA 2026.2.4 - the IDE the UI tests run in (the sandbox Rider
        // needs a JetBrains account login; see CLAUDE.md) - which also keeps one 10 GB IDE off the disk.
        // ./gradlew buildPlugin -PbuildAgainst=rider builds against Rider 2026.2.4.1 instead.
        // -PlocalIde=/path/to/an/installed/IDE uses an IDE already on disk instead (set in ~/.gradle/gradle.properties).
        val localIde = providers.gradleProperty("localIde").orNull
        when {
            localIde != null -> local(localIde)
            providers.gradleProperty("buildAgainst").orNull == "rider" -> rider(providers.gradleProperty("riderVersion")) { useInstaller = false }
            else -> intellijIdea(providers.gradleProperty("ideaVersion"))
        }
        // JCEF (the built-in browser the design surface runs in) is part of the core platform up to 2026.1, and its
        // own bundled plugin from 2026.2. plugin.xml's <depends>com.intellij.modules.jcef</depends> covers both; the
        // build only has to ask for the separate plugin when compiling against 2026.2 or newer.
        if (platformBranch() >= 262) {
            bundledPlugin("com.intellij.modules.jcef")
            bundledModule("intellij.platform.ui.jcef")
            bundledModule("intellij.libraries.jcef")
        }
        jetbrainsRuntime()
    }
    testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.13.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.13.4")
    testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion { sinceBuild = "261" }
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
    outputs.upToDateWhen { false }
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
        // Precompiled (ReadyToRun): the designer opens much faster than with everything compiled at start.
        "-p:PublishReadyToRun=true",
        "-o", hostOut.get().asFile.absolutePath,
    )
}

val publishConverter by tasks.registering(Exec::class) {
    description = "Builds NetForms' project converter (used for old .NET Framework projects) next to the host."
    dependsOn(publishHost)
    inputs.files(fileTree("external/NetForms/tools/NetForms.Convert") { exclude("**/bin/**", "**/obj/**") })
    outputs.file(hostOut.map { it.file("NetForms.Convert.dll") })
    environment("DOTNET_CLI_TELEMETRY_OPTOUT", "1")
    environment("DOTNET_NOLOGO", "1")
    commandLine(
        dotnet, "publish", file("external/NetForms/tools/NetForms.Convert/NetForms.Convert.csproj").absolutePath,
        "-c", "Release", "-r", "linux-x64", "--self-contained", "false", "-p:DebugType=none",
        "-o", hostOut.get().asFile.absolutePath,
    )
}

tasks.processResources {
    // NetForms' designer webview (MIT), served to JCEF from the plugin's resources.
    from("external/NetForms/designer/media") { into("formwork/webview/netforms") }
    from("external/NetForms/LICENSE") { into("formwork/licenses/NetForms") }
    from("external/NetForms/THIRD-PARTY-NOTICES.md") { into("formwork/licenses/NetForms") }
    from("LICENSE") { into("formwork/licenses/Formwork") }
    // NetForms' item templates (MIT): dialog, About box, login form, splash screen.
    for (t in listOf("netforms-dialog", "netforms-aboutbox", "netforms-login", "netforms-splash"))
        from("external/NetForms/templates/$t") { include("*.cs"); into("formwork/templates/$t") }
}

tasks.withType<org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask>().configureEach {
    dependsOn(publishHost, publishConverter)
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
// IntelliJ IDEA of the same platform version. See TESTING-FOR-JACOB.md for the
// checks only Rider can do.
//   ./gradlew runIdea -PopenPath=/path/to/project
intellijPlatformTesting {
    runIde {
        register("runIdea") {
            // The same IDE the plugin is built against (IntelliJ IDEA 2026.1.5.1 by default; -PlocalIde=... for another).
            // IDEA's OS-integration daemon can't start in this container and pops up error reports over the UI.
            plugins { disablePlugin("com.intellij.platform.daemon") }
            task {
                args(listOfNotNull(providers.gradleProperty("openPath").orNull, providers.gradleProperty("openFile").orNull))
                jvmArgumentProviders += CommandLineArgumentProvider {
                    listOf("-Dide.show.tips.on.startup.default.value=false", "-Djb.consents.confirmation.enabled=false", "-Djb.privacy.policy.text=<!--999.999-->", "-Didea.trust.all.projects=true",
                        // This test machine runs as root, where JCEF's sandbox can't start. Test runs only.
                        "-Dide.browser.jcef.sandbox.enable=false", "-Dide.browser.jcef.run-under-superuser.allowed=true",
                        // IDEA's own OS-integration daemon cannot start here; its error balloons get in the way of screenshots.
                        "-Didea.fatal.error.notification=disabled")
                }
            }
        }
    }
}

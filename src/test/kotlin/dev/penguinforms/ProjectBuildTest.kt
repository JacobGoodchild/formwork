package dev.penguinforms

import dev.penguinforms.project.ProjectConverter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Every sample, converted with "Make this project work on Linux too", builds on Linux with `dotnet build`
 * (what Rider's Build and Run do). Needs the .NET 10 SDK and NuGet the first time: FORMWORK_BUILD_TESTS=1.
 */
@EnabledIfEnvironmentVariable(named = "FORMWORK_BUILD_TESTS", matches = "1")
class ProjectBuildTest {
    @ParameterizedTest
    @ValueSource(strings = ["VsTemplate.Net8", "VsTemplate.Net9", "VsTemplate.Net10", "Calculator", "LoginForm", "MenuDemo", "TabsDemo", "TimerPicture"])
    fun `converted sample builds on Linux`(sample: String) {
        val dir = TestSupport.copySample(sample)
        val csproj = dir.walkTopDown().first { it.name.endsWith(".csproj") }
        val result = ProjectConverter.convert(csproj.readText())
        assertTrue(result is ProjectConverter.Result.Converted, result.toString())
        csproj.writeText((result as ProjectConverter.Result.Converted).text)
        val p = ProcessBuilder(TestSupport.dotnet, "build", csproj.path, "-nologo", "-v", "q")
            .redirectErrorStream(true).apply { environment()["DOTNET_CLI_TELEMETRY_OPTOUT"] = "1"; environment()["DOTNET_NOLOGO"] = "1" }.start()
        val out = p.inputStream.bufferedReader().readText()
        p.waitFor(5, TimeUnit.MINUTES)
        assertEquals(0, p.exitValue(), out)
        val name = csproj.name.removeSuffix(".csproj")
        assertTrue(File(csproj.parentFile, "bin/Debug/net10.0/$name.dll").isFile, "built for net10.0 with NetForms")
        assertTrue(Regex("""\b0 Error""").containsMatchIn(out) || !out.contains("error"), out)
    }
}

@EnabledIfEnvironmentVariable(named = "FORMWORK_BUILD_TESTS", matches = "1")
class NewProjectTest {
    @org.junit.jupiter.api.Test
    fun `a new Penguin Forms project is Microsoft's template, converted`() {
        val dir = java.nio.file.Files.createTempDirectory("penguinforms-new").toFile()
        val p = ProcessBuilder(TestSupport.dotnet, "new", "winforms", "-f", "net8.0", "-n", "WinFormsApp1", "-o", File(dir, "WinFormsApp1").path, "--no-restore")
            .redirectErrorStream(true).start()
        val log = p.inputStream.bufferedReader().readText(); p.waitFor(2, TimeUnit.MINUTES)
        assertEquals(0, p.exitValue(), log)
        val csproj = File(dir, "WinFormsApp1/WinFormsApp1.csproj")
        val r = ProjectConverter.convert(csproj.readText()) as ProjectConverter.Result.Converted
        assertEquals(File(TestSupport.repo, "src/test/resources/golden/projects/VsTemplate.Net8.after.csproj").readText(), r.text)
    }
}

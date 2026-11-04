package dev.formwork

import dev.formwork.project.ProjectConverter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/** Golden before/after files for "Make this project work on Linux too". Set FORMWORK_UPDATE_GOLDEN=1 to rewrite them. */
class ProjectConverterTest {
    private val golden = File(TestSupport.repo, "src/test/resources/golden/projects")

    private fun check(name: String, before: File) {
        val result = ProjectConverter.convert(before.readText())
        assertTrue(result is ProjectConverter.Result.Converted, "$name: $result")
        val after = (result as ProjectConverter.Result.Converted).text
        val expected = File(golden, "$name.after.csproj")
        if (System.getenv("FORMWORK_UPDATE_GOLDEN") == "1" || !expected.isFile) { expected.parentFile.mkdirs(); expected.writeText(after) }
        assertEquals(expected.readText(), after, "$name: the converted project differs from the golden file")
        // Converting again changes nothing.
        assertTrue(ProjectConverter.convert(after) is ProjectConverter.Result.AlreadyDone)
        // Everything but the two moved lines is kept, line for line.
        val removed = before.readLines().filter { it.contains("<TargetFramework>") || it.contains("<UseWindowsForms>") }
        val kept = before.readLines() - removed.toSet()
        assertTrue(after.lines().map { it.trimEnd('\r') }.containsAll(kept.map { it.trimEnd('\r') }), "$name: every other line is kept")
        assertEquals(before.readText().contains("\r\n"), after.contains("\r\n"), "line endings kept")
    }

    @Test fun `Visual Studio template, NET 8`() = check("VsTemplate.Net8", File(TestSupport.repo, "samples/VsTemplate.Net8/WinFormsApp1/WinFormsApp1.csproj"))
    @Test fun `Visual Studio template, NET 9`() = check("VsTemplate.Net9", File(TestSupport.repo, "samples/VsTemplate.Net9/WinFormsApp1/WinFormsApp1.csproj"))
    @Test fun `Visual Studio template, NET 10`() = check("VsTemplate.Net10", File(TestSupport.repo, "samples/VsTemplate.Net10/WinFormsApp1/WinFormsApp1.csproj"))
    @Test fun `Calculator sample`() = check("Calculator", File(TestSupport.repo, "samples/Calculator/Calculator/Calculator.csproj"))

    @Test
    fun `a project with more settings keeps them all`() {
        val before = """<Project Sdk="Microsoft.NET.Sdk">
  <PropertyGroup>
    <OutputType>WinExe</OutputType>
    <TargetFramework>net8.0-windows</TargetFramework>
    <Nullable>enable</Nullable>
    <UseWindowsForms>true</UseWindowsForms>
    <ImplicitUsings>enable</ImplicitUsings>
    <ApplicationIcon>app.ico</ApplicationIcon>
    <AssemblyName>MyGame</AssemblyName>
  </PropertyGroup>

  <ItemGroup>
    <Content Include="app.ico" />
  </ItemGroup>

</Project>"""
        val tmp = File.createTempFile("formwork", ".csproj").apply { writeText(before) }
        check("CustomSettings", tmp)
    }

    @Test
    fun `projects that can't or needn't be changed say why`() {
        assertTrue(ProjectConverter.convert("""<Project Sdk="Microsoft.NET.Sdk"><PropertyGroup><TargetFramework>net8.0</TargetFramework></PropertyGroup></Project>""") is ProjectConverter.Result.NotPossible)
        assertTrue(ProjectConverter.convert("""<Project Sdk="Microsoft.NET.Sdk"><ItemGroup><PackageReference Include="NetForms" Version="0.1.5" /></ItemGroup></Project>""") is ProjectConverter.Result.AlreadyDone)
        val old = File(TestSupport.repo, "external/NetForms/tests/Fixtures/NetFrameworkApp/FrameworkApp.csproj").readText()
        assertTrue(ProjectConverter.convert(old) is ProjectConverter.Result.OldFormat)
    }
}

class LegacyProjectConverterTest {
    @org.junit.jupiter.api.Test
    fun `an old NET Framework project becomes the two-world project`() {
        val old = java.io.File(TestSupport.repo, "external/NetForms/tests/Fixtures/NetFrameworkApp/FrameworkApp.csproj").readText()
        // What NetForms' converter writes for it (recorded from `NetForms.Convert --apply` on a copy).
        val converted = java.io.File(TestSupport.repo, "src/test/resources/golden/projects/FrameworkApp.netforms-convert.csproj").readText()
        val after = dev.formwork.project.LegacyProjectConverter.dualTarget(converted, old)
        val expected = java.io.File(TestSupport.repo, "src/test/resources/golden/projects/FrameworkApp.after.csproj")
        if (System.getenv("FORMWORK_UPDATE_GOLDEN") == "1" || !expected.isFile) expected.writeText(after)
        org.junit.jupiter.api.Assertions.assertEquals(expected.readText(), after)
        org.junit.jupiter.api.Assertions.assertTrue(after.contains("<TargetFramework>net481</TargetFramework>"))
        org.junit.jupiter.api.Assertions.assertTrue(after.contains("<Reference Include=\"System.Net.Http\" />"))
        org.junit.jupiter.api.Assertions.assertTrue(after.contains("<Compile Remove=\"Unlisted.cs\" />"), "the file list is kept exactly")
        org.junit.jupiter.api.Assertions.assertTrue(dev.formwork.project.ProjectConverter.convert(after) is dev.formwork.project.ProjectConverter.Result.AlreadyDone)
    }
}

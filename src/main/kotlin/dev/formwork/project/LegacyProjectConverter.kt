package dev.formwork.project

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import dev.formwork.host.DotnetLocator
import dev.formwork.host.HostLauncher
import dev.formwork.settings.FormworkSettings
import java.io.File
import java.nio.file.Files

/**
 * Old-format .NET Framework 4.x projects (`ToolsVersion`, `<Reference Include="System.Windows.Forms" />`).
 * NetForms' own converter (bundled next to the designer host) rewrites a *copy* of the project in the SDK style for
 * Linux - keeping the exact file list, resources and AssemblyInfo - and reports what won't work on Linux. Formwork
 * then splits the result: on Windows, `net48`-style SDK project with the real Windows Forms (Visual Studio 2022
 * opens these); elsewhere `net10.0` with NetForms. Riskier than the modern case, so the preview says so.
 */
object LegacyProjectConverter {
    /** The framework references an SDK-style .NET Framework project with UseWindowsForms already gets. */
    private val implicitReferences = setOf(
        "System", "System.Core", "System.Data", "System.Drawing", "System.Xml", "System.Xml.Linq", "System.Data.DataSetExtensions",
        "Microsoft.CSharp", "System.Windows.Forms", "System.Deployment", "mscorlib",
    )

    fun tfmOf(oldText: String): String {
        val v = Regex("<TargetFrameworkVersion>\\s*v([0-9.]+)\\s*</TargetFrameworkVersion>").find(oldText)?.groupValues?.get(1) ?: "4.8"
        return "net" + v.replace(".", "")
    }

    /** Turns NetForms' Linux-only SDK project into the two-world one. Pure text, so it is unit-tested. */
    fun dualTarget(converted: String, oldText: String): String {
        val tfm = tfmOf(oldText)
        val nl = if (converted.contains("\r\n")) "\r\n" else "\n"
        var out = converted.replace(Regex("""[ \t]*<TargetFramework>net10\.0</TargetFramework>[ \t]*\r?\n"""), "")
        val netforms = Regex("""[ \t]*<PackageReference Include="NetForms" Version="([^"]+)" />[ \t]*\r?\n""").find(out)
        val version = ProjectConverter.NETFORMS_VERSION // pinned, whatever the converter wrote
        if (netforms != null) out = out.removeRange(netforms.range)
        out = out.replace(Regex("""\s*<ItemGroup>\s*</ItemGroup>"""), "")
        val extra = Regex("""<Reference\s+Include\s*=\s*"([^",]+)"\s*/>""").findAll(oldText).map { it.groupValues[1] }
            .filter { it !in implicitReferences }.distinct().toList()
        val groupEnd = out.indexOf("</PropertyGroup>").let { out.indexOf('\n', it) + 1 }
        val block = buildString {
            append(nl)
            append("  <!-- Formwork: on Windows (college), the real Windows Forms on .NET Framework, as the project was. -->$nl")
            append("  <PropertyGroup Condition=\"'\$(OS)' == 'Windows_NT'\">$nl")
            append("    <TargetFramework>$tfm</TargetFramework>$nl")
            append("    <UseWindowsForms>true</UseWindowsForms>$nl")
            append("  </PropertyGroup>$nl")
            if (extra.isNotEmpty()) {
                append("  <ItemGroup Condition=\"'\$(OS)' == 'Windows_NT'\">$nl")
                extra.forEach { append("    <Reference Include=\"$it\" />$nl") }
                append("  </ItemGroup>$nl")
            }
            append(nl)
            append("  <!-- Formwork: anywhere else (Linux at home), NetForms runs the same code on .NET 10. -->$nl")
            append("  <PropertyGroup Condition=\"'\$(OS)' != 'Windows_NT'\">$nl")
            append("    <TargetFramework>net10.0</TargetFramework>$nl")
            append("  </PropertyGroup>$nl")
            append("  <ItemGroup Condition=\"'\$(OS)' != 'Windows_NT'\">$nl")
            append("    <PackageReference Include=\"NetForms\" Version=\"$version\" />$nl")
            append("  </ItemGroup>$nl")
        }
        return out.substring(0, groupEnd) + block + out.substring(groupEnd)
    }

    /** Converts a copy of the project's folder with NetForms' converter, then makes it two-world. */
    fun convert(csproj: File): ProjectConverter.Result {
        val tool = HostLauncher.hostDll()?.parentFile?.let { File(it, "NetForms.Convert.dll") }?.takeIf { it.isFile }
            ?: return ProjectConverter.Result.NotPossible("Formwork's copy of the NetForms converter is missing. Please reinstall the plugin.")
        val dotnet = DotnetLocator.find(FormworkSettings.getInstance().state.dotnetPath)
            ?: return ProjectConverter.Result.NotPossible("Formwork needs the .NET 10 SDK for this, and couldn't find 'dotnet'.")
        val temp = Files.createTempDirectory("formwork-convert").toFile()
        try {
            val copy = File(temp, csproj.parentFile.name)
            csproj.parentFile.copyRecursively(copy) { _, _ -> OnErrorAction.SKIP }
            copy.walkTopDown().filter { it.isDirectory && it.name in setOf("bin", "obj", ".vs") }.toList().forEach { it.deleteRecursively() }
            val target = File(copy, csproj.name)
            val out = ExecUtil.execAndGetOutput(GeneralCommandLine(dotnet.path, tool.path, target.path, "--apply").withCharset(Charsets.UTF_8), 120_000)
            if (out.exitCode != 0 || !target.readText().contains("Sdk=")) {
                return ProjectConverter.Result.NotPossible("The NetForms converter couldn't convert this project:\n" + (out.stdout + out.stderr).take(2000))
            }
            val oldText = csproj.readText()
            val text = dualTarget(target.readText(), oldText)
            val report = out.stdout.lines().map { it.trim() }.filter { it.startsWith("- ") || it.startsWith("warning") || it.startsWith("error") }
            val warnings = listOf(
                "This is an old-style .NET Framework project. It becomes a new-style (SDK) project: Visual Studio 2022 opens it, but please check it at college (build, open the designer, run) before you rely on it.",
            ) + report.filter { !it.startsWith("- ") }.map { it.replace(Regex("^(warning|error)\\s+"), "") }
            val notes = listOf("On Windows it keeps the real Windows Forms on ${tfmOf(oldText)}; on Linux it uses .NET 10 and NetForms.") +
                report.filter { it.startsWith("- ") }.map { it.removePrefix("- ") }
            return ProjectConverter.Result.Converted(text, notes, warnings)
        } finally {
            temp.deleteRecursively()
        }
    }
}

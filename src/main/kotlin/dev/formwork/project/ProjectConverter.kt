package dev.formwork.project

/**
 * "Make this project work on Linux too" (brief section 7): rewrites a Visual Studio Windows Forms project file so
 * it picks the real Windows Forms on Windows (college, Visual Studio) and NetForms everywhere else (home, Rider on
 * Linux). The code files never change.
 *
 * The rewrite is a minimal text edit: only the `TargetFramework` and `UseWindowsForms` lines move into
 * OS-conditioned groups; every other line, comment, blank line and the line endings stay exactly as they were.
 */
object ProjectConverter {
    /** NetForms package version written into converted projects (the release the designer host is built from). */
    const val NETFORMS_VERSION = "0.1.5"

    sealed class Result {
        /** The new project text, plus plain-English notes for the preview. */
        data class Converted(val text: String, val notes: List<String>, val warnings: List<String>) : Result()
        data class AlreadyDone(val reason: String) : Result()
        /** An old-format (.NET Framework) project: handled by [LegacyProjectConverter]. */
        object OldFormat : Result()
        data class NotPossible(val reason: String) : Result()
    }

    private val tfmLine = Regex("""^([ \t]*)<TargetFramework>\s*([^<]+?)\s*</TargetFramework>[ \t]*\r?\n""", RegexOption.MULTILINE)
    private val tfmsLine = Regex("""<TargetFrameworks>\s*([^<]+?)\s*</TargetFrameworks>""")
    private val useWinFormsLine = Regex("""^[ \t]*<UseWindowsForms>\s*(?i:true)\s*</UseWindowsForms>[ \t]*\r?\n""", RegexOption.MULTILINE)

    fun isOldFormat(text: String): Boolean =
        !Regex("""<Project[^>]*\bSdk\s*=""").containsMatchIn(text) && (text.contains("ToolsVersion") || text.contains("<TargetFrameworkVersion>"))

    fun usesWindowsForms(text: String): Boolean =
        useWinFormsLine.containsMatchIn(text) || (isOldFormat(text) && Regex("""<Reference\s+Include\s*=\s*"System\.Windows\.Forms[",]""").containsMatchIn(text))

    fun alreadyWorksOnLinux(text: String): Boolean =
        Regex("""<PackageReference\s+Include\s*=\s*"NetForms"""", RegexOption.IGNORE_CASE).containsMatchIn(text)

    fun convert(text: String): Result {
        if (alreadyWorksOnLinux(text)) return Result.AlreadyDone("This project already references NetForms, so it already works on Linux.")
        if (isOldFormat(text)) return if (usesWindowsForms(text)) Result.OldFormat else Result.NotPossible("This project doesn't use Windows Forms, so there's nothing to change.")
        if (!useWinFormsLine.containsMatchIn(text)) return Result.NotPossible("This project doesn't use Windows Forms (it has no <UseWindowsForms>true</UseWindowsForms>), so there's nothing to change.")
        if (tfmsLine.containsMatchIn(text)) return Result.NotPossible("This project builds for several frameworks at once (<TargetFrameworks>). Formwork can only change projects with one <TargetFramework>; please ask for help with this one.")
        val tfms = tfmLine.findAll(text).toList()
        if (tfms.size != 1) return Result.NotPossible("Formwork expected exactly one <TargetFramework> line in the project file and found ${tfms.size}.")
        if (Regex("""<UseWPF>\s*(?i:true)""").containsMatchIn(text)) return Result.NotPossible("This project also uses WPF, which doesn't run on Linux.")

        val tfm = tfms[0].groupValues[2]
        val indent = tfms[0].groupValues[1].ifEmpty { "    " }
        val unit = if (indent.startsWith("\t")) "\t" else indent.substring(0, maxOf(1, indent.length / 2))
        val nl = if (text.contains("\r\n")) "\r\n" else "\n"
        val warnings = ArrayList<String>()
        if (!tfm.contains("-windows")) warnings.add("Its target framework '$tfm' isn't a Windows one; it is kept for Windows anyway.")

        // 1. Take the two lines out of their group (they move into the conditioned groups below).
        val firstGroupEnd = text.indexOf("</PropertyGroup>", tfms[0].range.first)
        var out = text.removeRange(tfms[0].range)
        val useMatch = useWinFormsLine.find(out)!!
        out = out.removeRange(useMatch.range)

        // 2. After the group that held them, add the Windows half and the Linux half.
        val groupEnd = out.indexOf("</PropertyGroup>", minOf(tfms[0].range.first, useMatch.range.first).coerceAtMost(out.length))
            .takeIf { it >= 0 } ?: (firstGroupEnd - (text.length - out.length))
        val lineEnd = out.indexOf('\n', groupEnd).let { if (it < 0) out.length else it + 1 }
        val i1 = unit
        val i2 = unit + unit
        val block = buildString {
            append(nl)
            append("$i1<!-- Formwork: on Windows (college), the real Windows Forms, exactly as Visual Studio made it. -->$nl")
            append("$i1<PropertyGroup Condition=\"'\$(OS)' == 'Windows_NT'\">$nl")
            append("$i2<TargetFramework>$tfm</TargetFramework>$nl")
            append("$i2<UseWindowsForms>true</UseWindowsForms>$nl")
            append("$i1</PropertyGroup>$nl")
            append(nl)
            append("$i1<!-- Formwork: anywhere else (Linux at home), NetForms runs the same code. -->$nl")
            append("$i1<PropertyGroup Condition=\"'\$(OS)' != 'Windows_NT'\">$nl")
            append("$i2<TargetFramework>net10.0</TargetFramework>$nl")
            append("$i1</PropertyGroup>$nl")
            append("$i1<ItemGroup Condition=\"'\$(OS)' != 'Windows_NT'\">$nl")
            append("$i2<PackageReference Include=\"NetForms\" Version=\"$NETFORMS_VERSION\" />$nl")
            append("$i2<Using Include=\"System.Drawing\" />$nl")
            append("$i2<Using Include=\"System.Windows.Forms\" />$nl")
            append("$i1</ItemGroup>$nl")
        }
        out = out.substring(0, lineEnd) + block + out.substring(lineEnd)

        val notes = listOf(
            "On Windows (Visual Studio at college) nothing changes: the project still uses the real Windows Forms with $tfm.",
            "On Linux the same code builds with .NET 10 and NetForms $NETFORMS_VERSION (downloaded from NuGet the first time you build).",
            "Your code files (Program.cs, Form1.cs, Form1.Designer.cs) are not touched.",
        )
        return Result.Converted(out, notes, warnings)
    }
}

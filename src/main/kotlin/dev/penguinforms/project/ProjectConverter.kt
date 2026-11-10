package dev.penguinforms.project

/**
 * "Make this project work on Linux too" (brief section 7): rewrites a Visual Studio Windows Forms project file so
 * it picks the real Windows Forms on Windows (Visual Studio) and NetForms everywhere else (home, Rider on
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

    /** The Linux-only build step that makes the running app paint Windows 10/11's system colours ([windowsColoursBlock]). */
    const val COLOURS_TARGET = "PenguinFormsWindowsColours"

    fun hasWindowsColours(text: String): Boolean = text.contains("Name=\"$COLOURS_TARGET\"")

    /**
     * On Linux, .NET's built-in system colours are Windows XP's (forms beige, #ECE9D8). NetForms carries Windows
     * 10/11's table (forms #F0F0F0, what Visual Studio's designer and a Windows PC show) but keeps the switch internal.
     * This build step, for the Linux build only, generates one file under obj/ that turns it on when the app starts.
     * It only changes how colours are *painted*: `SystemColors.Control` still reports the same value, so code that
     * compares colours behaves the same. The user's own files are not touched, and Visual Studio never runs it.
     */
    fun windowsColoursBlock(i1: String, i2: String, nl: String): String {
        val i3 = i2 + i1
        val file = "\$(IntermediateOutputPath)PenguinForms.WindowsColours.g.cs"
        val code = "// &lt;auto-generated /&gt; by Penguin Forms (see the project file);#pragma warning disable;" +
            "internal static class PenguinFormsWindowsColours { [System.Runtime.CompilerServices.ModuleInitializer] internal static void Install() { " +
            "try { var t = typeof(System.Drawing.Graphics).Assembly.GetType(&quot;System.Drawing.HermeticRendering&quot;)%3B " +
            "var p = t?.GetField(&quot;WindowsDefaultPalette&quot;)?.GetValue(null)%3B " +
            "if (p != null) t!.GetProperty(&quot;Palette&quot;)?.SetValue(null, p)%3B } catch (System.Exception) { } } }"
        return buildString {
            append("$i1<!-- Penguin Forms: on Linux, paint Windows 10/11's system colours (light grey #F0F0F0 forms, not .NET's beige),$nl")
            append("$i1     so the running app looks like the designer. Only adds a generated file under obj/; your code files are unchanged.$nl")
            append("$i1     To turn it off, add <$COLOURS_TARGET>false</$COLOURS_TARGET> to a PropertyGroup. -->$nl")
            append("$i1<Target Name=\"$COLOURS_TARGET\" BeforeTargets=\"CoreCompile\" Condition=\"'\$(OS)' != 'Windows_NT' and '\$($COLOURS_TARGET)' != 'false' and '\$(Language)' == 'C#'\">$nl")
            append("$i2<WriteLinesToFile File=\"$file\" Overwrite=\"true\" WriteOnlyWhenDifferent=\"true\" Lines=\"$code\" />$nl")
            append("$i2<ItemGroup>$nl")
            append("$i3<Compile Include=\"$file\" />$nl")
            append("$i3<FileWrites Include=\"$file\" />$nl")
            append("$i2</ItemGroup>$nl")
            append("$i1</Target>$nl")
        }
    }

    /** [text] with [windowsColoursBlock] added just before `</Project>`, indented and with line endings like the file. */
    fun addWindowsColours(text: String): String {
        if (hasWindowsColours(text)) return text
        val end = text.lastIndexOf("</Project>").takeIf { it >= 0 } ?: return text
        val nl = if (text.contains("\r\n")) "\r\n" else "\n"
        val indent = Regex("""^([ \t]+)<""", RegexOption.MULTILINE).find(text)?.groupValues?.get(1) ?: "  "
        val unit = if (indent.startsWith("\t")) "\t" else indent
        val before = text.substring(0, end).trimEnd(' ', '\t')
        val sep = if (before.endsWith("$nl$nl")) "" else nl
        return before + sep + windowsColoursBlock(unit, unit + unit, nl) + nl + text.substring(end)
    }

    fun convert(text: String): Result {
        if (alreadyWorksOnLinux(text)) {
            if (!hasWindowsColours(text) && text.lastIndexOf("</Project>") >= 0) return Result.Converted(
                addWindowsColours(text),
                listOf(
                    "This project already works on Linux. This adds one thing: when it runs on Linux, forms are light grey like on Windows and in the designer, instead of beige.",
                    "On Windows (Visual Studio) nothing changes.",
                    "Your code files are not touched.",
                ),
                emptyList(),
            )
            return Result.AlreadyDone("This project already references NetForms, so it already works on Linux.")
        }
        if (isOldFormat(text)) return if (usesWindowsForms(text)) Result.OldFormat else Result.NotPossible("This project doesn't use Windows Forms, so there's nothing to change.")
        if (!useWinFormsLine.containsMatchIn(text)) return Result.NotPossible("This project doesn't use Windows Forms (it has no <UseWindowsForms>true</UseWindowsForms>), so there's nothing to change.")
        if (tfmsLine.containsMatchIn(text)) return Result.NotPossible("This project builds for several frameworks at once (<TargetFrameworks>). Penguin Forms can only change projects with one <TargetFramework>; please ask for help with this one.")
        val tfms = tfmLine.findAll(text).toList()
        if (tfms.size != 1) return Result.NotPossible("Penguin Forms expected exactly one <TargetFramework> line in the project file and found ${tfms.size}.")
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
            append("$i1<!-- Penguin Forms: on Windows, the real Windows Forms, exactly as Visual Studio made it. -->$nl")
            append("$i1<PropertyGroup Condition=\"'\$(OS)' == 'Windows_NT'\">$nl")
            append("$i2<TargetFramework>$tfm</TargetFramework>$nl")
            append("$i2<UseWindowsForms>true</UseWindowsForms>$nl")
            append("$i1</PropertyGroup>$nl")
            append(nl)
            append("$i1<!-- Penguin Forms: anywhere else (Linux at home), NetForms runs the same code. -->$nl")
            append("$i1<PropertyGroup Condition=\"'\$(OS)' != 'Windows_NT'\">$nl")
            append("$i2<TargetFramework>net10.0</TargetFramework>$nl")
            append("$i1</PropertyGroup>$nl")
            append("$i1<ItemGroup Condition=\"'\$(OS)' != 'Windows_NT'\">$nl")
            append("$i2<PackageReference Include=\"NetForms\" Version=\"$NETFORMS_VERSION\" />$nl")
            append("$i2<Using Include=\"System.Drawing\" />$nl")
            append("$i2<Using Include=\"System.Windows.Forms\" />$nl")
            append("$i1</ItemGroup>$nl")
        }
        out = addWindowsColours(out.substring(0, lineEnd) + block + out.substring(lineEnd))

        val notes = listOf(
            "On Windows (Visual Studio) nothing changes: the project still uses the real Windows Forms with $tfm.",
            "On Linux the same code builds with .NET 10 and NetForms $NETFORMS_VERSION (downloaded from NuGet the first time you build).",
            "On Linux the running app also gets Windows 10/11's colours (light grey forms, not beige), like the designer.",
            "Your code files (Program.cs, Form1.cs, Form1.Designer.cs) are not touched.",
        )
        return Result.Converted(out, notes, warnings)
    }
}

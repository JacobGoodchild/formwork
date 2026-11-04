package dev.formwork.editor

/**
 * Where the cursor goes for a handler, as in Visual Studio: on the line inside its braces. For a fresh stub
 * that line is empty, so [indentToInsert] gives it the body's indentation (VS shows the cursor indented there).
 */
object HandlerLocator {
    data class Target(val offset: Int, val indentToInsert: String)

    fun bodyPosition(text: String, method: String, line1Based: Int?): Target? {
        val lines = text.split('\n')
        val decl = Regex("""\b${Regex.escape(method)}\s*\(""")
        var index = -1
        if (line1Based != null && line1Based - 1 in lines.indices && decl.containsMatchIn(lines[line1Based - 1])) index = line1Based - 1
        if (index < 0) index = lines.indexOfFirst { decl.containsMatchIn(it) && !it.trimStart().startsWith("//") && !it.contains("+=") && !it.contains("-=") }
        if (index < 0) return null
        // The opening brace: on this line or the next few.
        var brace = -1
        for (i in index until minOf(lines.size, index + 4)) if (lines[i].contains('{')) { brace = i; break }
        if (brace < 0) return null
        val braceIndent = lines[brace].takeWhile { it == ' ' || it == '\t' }
        val bodyIndent = braceIndent + if (braceIndent.contains('\t')) "\t" else "    "
        val bodyLine = brace + 1
        if (bodyLine !in lines.indices) return null
        val lineStart = lines.take(bodyLine).sumOf { it.length + 1 }
        val body = lines[bodyLine]
        return if (body.isBlank() && !lines[bodyLine].trim().startsWith("}")) {
            Target(lineStart + body.length, if (body.length < bodyIndent.length) bodyIndent.substring(body.length) else "")
        } else {
            Target(lineStart + body.takeWhile { it == ' ' || it == '\t' }.length, "")
        }
    }
}

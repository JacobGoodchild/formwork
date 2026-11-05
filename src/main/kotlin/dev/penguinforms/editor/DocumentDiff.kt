package dev.penguinforms.editor

import com.intellij.openapi.editor.Document

/** Replaces a document's text by changing only the middle part that differs (keeps carets, folds, markers). */
object DocumentDiff {
    data class Change(val start: Int, val endOld: Int, val replacement: String)

    fun change(old: CharSequence, new: CharSequence): Change? {
        if (old.length == new.length && old.toString() == new.toString()) return null
        var start = 0
        val max = minOf(old.length, new.length)
        while (start < max && old[start] == new[start]) start++
        var endOld = old.length
        var endNew = new.length
        while (endOld > start && endNew > start && old[endOld - 1] == new[endNew - 1]) { endOld--; endNew-- }
        return Change(start, endOld, new.subSequence(start, endNew).toString())
    }

    fun replace(doc: Document, text: String) {
        val c = change(doc.immutableCharSequence, text) ?: return
        doc.replaceString(c.start, c.endOld, c.replacement)
    }
}

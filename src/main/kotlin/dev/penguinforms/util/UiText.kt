package dev.penguinforms.util

import com.intellij.openapi.util.text.StringUtil

/**
 * The IDE's message dialogs and notifications read their text as HTML, so a project-file tag in a message
 * (`<UseWindowsForms>`) vanished. Everything we show goes through here: escaped, line breaks kept.
 */
object UiText {
    /** Notification content (already HTML): escaped, `\n` as a line break. */
    fun html(text: String): String = StringUtil.escapeXmlEntities(text).replace("\r\n", "\n").replace("\n", "<br>")

    /** A Messages dialog's text: whole HTML, so it is shown the same however the IDE decides to render it. */
    fun dialog(text: String): String = "<html>${html(text)}</html>"
}

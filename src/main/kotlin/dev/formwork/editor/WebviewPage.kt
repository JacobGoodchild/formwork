package dev.formwork.editor

/**
 * Builds the designer page: NetForms' webview (designer.css, geometry.js, designer.js, shipped unchanged in
 * the plugin under formwork/webview/netforms) plus Formwork's bridge and styling.
 */
object WebviewPage {
    private fun resource(path: String): String =
        WebviewPage::class.java.getResourceAsStream("/formwork/webview/$path")?.use { String(it.readAllBytes(), Charsets.UTF_8) }
            ?: error("Formwork resource missing: $path")

    /** JSON made safe to sit inside a <script> element (no </script>, no line separators JavaScript rejects). */
    fun scriptSafe(json: String): String = json.replace("<", "\\u003c").replace(">", "\\u003e").replace("&", "\\u0026")
        .replace("\u2028", "\\u2028").replace("\u2029", "\\u2029")

    /** [sendFunction] is JavaScript `function(text){...}` that hands a string to Kotlin (a JBCefJSQuery). */
    fun html(sendFunction: String, themeCss: String, initialState: String): String {
        val bridge = resource("bridge.js")
            .replace("/*FW_SEND*/null", sendFunction)
            .replace("/*FW_STATE*/null", scriptSafe(initialState))
        return """<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<meta http-equiv="Content-Security-Policy" content="default-src 'none'; img-src data:; style-src 'unsafe-inline'; script-src 'unsafe-inline'; font-src data:">
<title>Formwork Designer</title>
<style id="fw-theme">$themeCss</style>
<style>${resource("netforms/designer.css")}</style>
<style>${resource("formwork.css")}</style>
</head>
<body>
<div id="app">
  <div id="toolbar"></div>
  <div id="main">
    <aside id="toolbox"></aside>
    <section id="workspace"><div id="stage"></div><div id="tray"></div></section>
    <aside id="inspector"></aside>
  </div>
  <div id="status"></div>
</div>
<script>$bridge</script>
<script>${resource("netforms/geometry.js")}</script>
<script>${resource("netforms/designer.js")}</script>
<script>${resource("formwork.js")}</script>
</body>
</html>"""
    }
}

package dev.penguinforms.project

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.io.File

/**
 * The project's own controls and UserControls for the toolbox, from its last build (VS shows them in a
 * "<Project> Components" group after a build). Ported from NetForms' designer/src/libraries.ts (the parts
 * a single project needs). Loading them runs the project's code, so the caller only loads them when the
 * project is trusted.
 */
class ProjectLibraries(val projectFile: File?, val output: File?) {
    val projectName: String? get() = projectFile?.name?.removeSuffix(".csproj")

    /** Toolbox groups for the host's `toolbox` request. */
    fun groups(): JsonArray = JsonArray().apply {
        if (output != null && projectName != null) add(JsonObject().apply {
            addProperty("id", "project")
            addProperty("name", "$projectName Components")
            add("assemblies", JsonArray().apply { add(output.path) })
        })
    }

    /** What changes when the project is rebuilt (to reload only when needed). */
    fun signature(): String = output?.let { "${it.path}@${it.lastModified()}" } ?: ""

    companion object {
        private val skip = Regex("^(publish|runtimes|ref|refint)$", RegexOption.IGNORE_CASE)

        /** The first .csproj in the form's folder or a folder above it (the form's project). */
        fun projectFileOf(formDir: File): File? {
            var dir: File? = formDir
            var depth = 0
            while (dir != null && depth < 8) {
                dir.listFiles { f -> f.isFile && f.name.endsWith(".csproj", ignoreCase = true) }?.minByOrNull { it.name }?.let { return it }
                dir = dir.parentFile
                depth++
            }
            return null
        }

        fun assemblyName(projectFile: File): String {
            val text = try { projectFile.readText() } catch (_: Exception) { "" }
            return Regex("<AssemblyName>\\s*([^<]+?)\\s*</AssemblyName>").find(text)?.groupValues?.get(1)
                ?.takeIf { !it.contains("$(") } ?: projectFile.name.removeSuffix(".csproj")
        }

        /** The newest bin/<Configuration>/<tfm>[/<rid>]/<AssemblyName>.dll that has its .deps.json beside it. */
        fun findOutput(projectFile: File): File? {
            val name = assemblyName(projectFile).lowercase() + ".dll"
            val found = ArrayList<File>()
            fun visit(dir: File, depth: Int) {
                for (f in dir.listFiles() ?: return) {
                    if (f.isDirectory) { if (depth < 3 && !skip.matches(f.name)) visit(f, depth + 1) }
                    else if (f.name.lowercase() == name && File(f.parentFile, f.name.dropLast(4) + ".deps.json").isFile) found.add(f)
                }
            }
            visit(File(projectFile.parentFile, "bin"), 0)
            return found.maxByOrNull { it.lastModified() }
        }

        fun of(formDir: File): ProjectLibraries {
            val project = projectFileOf(formDir)
            return ProjectLibraries(project, project?.let(::findOutput))
        }
    }
}

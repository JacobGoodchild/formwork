package dev.formwork

import dev.formwork.project.ProjectConverter
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.io.File

/** Writes every sample, converted, to build/converted-samples (CI hands them to the Windows job). FORMWORK_EXPORT_CONVERTED=1. */
@EnabledIfEnvironmentVariable(named = "FORMWORK_EXPORT_CONVERTED", matches = "1")
class ExportConvertedSamplesTest {
    @Test
    fun `export converted samples`() {
        val out = File(TestSupport.repo, "build/converted-samples").apply { deleteRecursively(); mkdirs() }
        val samples = File(TestSupport.repo, "samples").listFiles()!!.filter { it.isDirectory }
        for (s in samples) {
            val target = File(out, s.name)
            s.copyRecursively(target)
            target.walkTopDown().filter { it.isDirectory && (it.name == "bin" || it.name == "obj") }.toList().forEach { it.deleteRecursively() }
            val csproj = target.walkTopDown().first { it.name.endsWith(".csproj") }
            val r = ProjectConverter.convert(csproj.readText())
            assertTrue(r is ProjectConverter.Result.Converted, "${s.name}: $r")
            csproj.writeText((r as ProjectConverter.Result.Converted).text)
        }
    }
}

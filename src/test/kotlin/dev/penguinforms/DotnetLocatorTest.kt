package dev.penguinforms

import dev.penguinforms.host.DotnetLocator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

class DotnetLocatorTest {
    private fun fakeDotnet(dir: File): File {
        dir.mkdirs()
        return File(dir, "dotnet").apply { writeText("#!/bin/sh\n"); setExecutable(true) }
    }

    @Test
    fun `candidates come in order - setting, DOTNET_ROOT, PATH, home, well-known - without duplicates or relative folders`() {
        val c = DotnetLocator.candidates(
            "/my/dotnet",
            mapOf("DOTNET_ROOT" to "/root-dir", "PATH" to "/usr/bin:relative:/root-dir:/usr/bin"),
            "/home/me",
        ).map { it.path }
        assertEquals(listOf("/my/dotnet", "/root-dir/dotnet", "/usr/bin/dotnet", "/home/me/.dotnet/dotnet"), c.take(4))
        assertTrue(c.contains("/usr/share/dotnet/dotnet"))
        assertEquals(c.size, c.toSet().size)
    }

    @Test
    fun `the first dotnet with NET 10 wins, not the first dotnet found`() {
        val tmp = Files.createTempDirectory("pf-dotnet").toFile()
        try {
            val old = fakeDotnet(File(tmp, "usr-bin"))      // a distribution package with .NET 8 only
            val missing = File(tmp, "nowhere/dotnet")
            val new = fakeDotnet(File(tmp, "home/.dotnet")) // Microsoft's install script, with .NET 10
            val runtimes = mapOf(old.canonicalPath to listOf("8.0.11"), new.canonicalPath to listOf("8.0.11", "10.0.3"))
            val s = DotnetLocator.choose(listOf(missing, old, new)) { runtimes[it.canonicalPath] ?: emptyList() }
            assertEquals(new.canonicalPath, s.found?.path)
            val text = s.describe()
            assertTrue(text.contains("nowhere/dotnet (not there)"), text)
            assertTrue(text.contains("usr-bin/dotnet (has .NET 8.0.11)"), text)
        } finally { tmp.deleteRecursively() }
    }

    @Test
    fun `none with NET 10 gives no program but says where it looked`() {
        val tmp = Files.createTempDirectory("pf-dotnet").toFile()
        try {
            val old = fakeDotnet(File(tmp, "a"))
            val s = DotnetLocator.choose(listOf(old, File(tmp, "b/dotnet"))) { listOf("9.0.1") }
            assertNull(s.found)
            assertEquals(2, s.checked.size)
            assertTrue(DotnetLocator.isNet10OrNewer("11.0.0") && !DotnetLocator.isNet10OrNewer("9.0.1"))
        } finally { tmp.deleteRecursively() }
    }
}

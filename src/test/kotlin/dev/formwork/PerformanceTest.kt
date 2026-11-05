package dev.formwork

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * The brief's performance targets, measured at the host (where the work of each gesture happens):
 * a form opens in under 3 s (host start included); a drag step (move + repaint) is fast enough for 30 redraws a
 * second; a property edit reaches the canvas in under 200 ms. Results are printed and written to build/perf.txt.
 */
class PerformanceTest {
    private fun ms(start: Long) = (System.nanoTime() - start) / 1_000_000.0

    @Test
    fun `performance targets`() {
        val dir = TestSupport.copySample("TabsDemo") // the biggest sample: 18 controls in nested containers
        val designer = dir.walkTopDown().first { it.name == "Form1.Designer.cs" }
        val out = StringBuilder()
        val t0 = System.nanoTime()
        TestSupport.startHost().use { host ->
            host.call("formwork.hello")
            host.call("open", JsonObject().apply { addProperty("path", designer.path) })
            val open = ms(t0)
            out.append("open (cold host start + read + paint): %.0f ms (target < 3000)\n".format(open))

            fun move(x: Int) = JsonObject().apply { add("ops", JsonArray().apply { add(JsonObject().apply {
                addProperty("op", "setBounds"); addProperty("id", "groupBox1"); addProperty("x", x); addProperty("y", 6) }) }) }
            repeat(3) { host.call("apply", move(10 + it)) } // warm up
            val times = (0 until 30).map { i -> val s = System.nanoTime(); host.call("apply", move(20 + i)); ms(s) }.sorted()
            val median = times[times.size / 2]
            out.append("committed move (on mouse release: move + rewrite Designer.cs + repaint): median %.1f ms, worst %.1f ms (%.0f per second). While dragging, the page moves the outline itself every frame without asking the host.\n".format(median, times.last(), 1000 / median))

            val propTimes = (0 until 10).map { i ->
                val s = System.nanoTime()
                host.call("apply", JsonObject().apply { add("ops", JsonArray().apply { add(JsonObject().apply {
                    addProperty("op", "setProp"); addProperty("id", "groupBox1"); addProperty("prop", "Text"); addProperty("value", "Options $i") }) }) })
                ms(s)
            }.sorted()
            val prop = propTimes[propTimes.size / 2]
            out.append("property edit → new canvas picture: median %.1f ms, worst %.1f ms (target < 200)\n".format(prop, propTimes.last()))
            print(out)
            File(TestSupport.repo, "build").mkdirs(); File(TestSupport.repo, "build/perf.txt").writeText(out.toString())
            assertTrue(open < 3000, out.toString())
            assertTrue(median < 100, out.toString())
            assertTrue(prop < 200, out.toString())
        }
    }
}

package com.willfp.eco.internal.config

import com.willfp.eco.core.config.ConfigType
import com.willfp.eco.core.config.interfaces.Config
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Random
import java.util.concurrent.atomic.AtomicInteger

class EcoConfigReadTest {
    private fun values(config: EcoConfig): MutableMap<String, Any?> {
        val field = EcoConfig::class.java.getDeclaredField("values").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        return field.get(config) as MutableMap<String, Any?>
    }

    // Original published lookup, retained as an independent behavior reference.
    private fun original(config: EcoConfig, path: String): Any? {
        val first = path.split(".")[0]
        if (path.contains(".")) {
            val rest = path.removePrefix("$first.")
            if (rest.isEmpty()) return null
            val found = original(config, first)
            return if (found is EcoConfig && found.javaClass == EcoConfig::class.java) original(found, rest)
            else if (found is Config) found.get(rest) else null
        }
        return values(config)[first]
    }

    @Test
    fun `reads preserve path edge cases updates and child dispatch`() {
        val root = EcoConfig(ConfigType.YAML)
        val section = EcoConfig(ConfigType.YAML)
        values(root).putAll(mapOf("" to "empty", "enabled" to true, "section" to section, "中文" to "值", "scalar" to 7))
        values(section).putAll(mapOf("" to "nested-empty", "value" to "original"))
        val calls = AtomicInteger()
        val child = object : EcoConfig(ConfigType.YAML) {
            override fun get(path: String): Any { calls.incrementAndGet(); return "override:$path" }
        }
        values(root)["delegate"] = child
        val random = Random(20261110)
        val parts = listOf("", "section", "value", "enabled", "missing", "scalar", "中文")
        val paths = mutableListOf("", ".", "..", "section.", ".section", "section..value", "delegate.a.b")
        repeat(10000) { paths.add((0..random.nextInt(6)).joinToString(".") { parts[random.nextInt(parts.size)] }) }
        paths.forEach { assertEquals(original(root, it), root.get(it), it) }
        repeat(100) { values(section)["value"] = it; assertEquals(it, root.get("section.value")) }
        assertEquals("override:a.b", root.get("delegate.a.b"))
        assertEquals(3, calls.get())
    }
}

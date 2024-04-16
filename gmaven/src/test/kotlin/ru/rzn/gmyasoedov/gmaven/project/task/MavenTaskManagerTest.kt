package ru.rzn.gmyasoedov.gmaven.project.task

import org.junit.Assert.assertEquals
import org.junit.Test

class MavenTaskManagerTest {

    @Test
    fun testOrder() {
        val taskManager = MavenTaskManager()
        var tasks = taskManager.prepareTaskOrder(listOf("clean", "package"))
        assertEquals(listOf("clean", "package"), tasks)

        tasks = taskManager.prepareTaskOrder(listOf("package", "clean"))
        assertEquals(listOf("clean", "package"), tasks)

        tasks = taskManager.prepareTaskOrder(listOf("package", "verify", "clean"))
        assertEquals(listOf("clean", "package", "verify"), tasks)

        tasks = taskManager.prepareTaskOrder(listOf("package", "verify", "clean", "install"))
        assertEquals(listOf("clean", "package", "verify", "install"), tasks)

        tasks = taskManager.prepareTaskOrder(listOf("deploy", "verify", "clean", "install"))
        assertEquals(listOf("clean", "verify", "install", "deploy"), tasks)

        tasks = taskManager.prepareTaskOrder(
            listOf("help:effective-pom", "-f", "/path/project/pom.xml", "initialize", "site", "clean")
        )
        assertEquals(listOf("clean", "initialize", "site", "help:effective-pom", "-f", "/path/project/pom.xml"), tasks)
    }
}
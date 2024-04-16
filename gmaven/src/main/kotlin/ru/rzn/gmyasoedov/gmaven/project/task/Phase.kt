package ru.rzn.gmyasoedov.gmaven.project.task

enum class Phase(val phaseName:String, val lifecycle: Lifecycle) {
    PRE_CLEAN("pre-clean", Lifecycle.CLEAN),
    CLEAN("clean", Lifecycle.CLEAN),
    POST_CLEAN("post-clean", Lifecycle.CLEAN),

    VALIDATE("validate", Lifecycle.DEFAULT),
    INITIALIZE("initialize", Lifecycle.DEFAULT),
    GENERATE_SOURCES("generate-sources", Lifecycle.DEFAULT),
    PROCESS_SOURCES("process-sources", Lifecycle.DEFAULT),
    GENERATE_RESOURCES("generate-resources", Lifecycle.DEFAULT),
    PROCESS_RESOURCES("process-resources", Lifecycle.DEFAULT),
    COMPILE("compile", Lifecycle.DEFAULT),
    PROCESS_CLASSES("process-classes", Lifecycle.DEFAULT),
    GENERATE_TEST_SOURCES("generate-test-sources", Lifecycle.DEFAULT),
    PROCESS_TEST_SOURCES("process-test-sources", Lifecycle.DEFAULT),
    GENERATE_TEST_RESOURCES("generate-test-resources", Lifecycle.DEFAULT),
    PROCESS_TEST_RESOURCES("process-test-resources", Lifecycle.DEFAULT),
    TEST_COMPILE("test-compile", Lifecycle.DEFAULT),
    PROCESS_TEST_CLASSES("process-test-classes", Lifecycle.DEFAULT),
    TEST("test", Lifecycle.DEFAULT),
    PREPARE_PACKAGE("prepare-package", Lifecycle.DEFAULT),
    PACKAGE("package", Lifecycle.DEFAULT),
    PRE_INTEGRATION_TEST("pre-integration-test", Lifecycle.DEFAULT),
    INTEGRATION_TEST("integration-test", Lifecycle.DEFAULT),
    POST_INTEGRATION_TEST("post-integration-test", Lifecycle.DEFAULT),
    VERIFY("verify", Lifecycle.DEFAULT),
    INSTALL("install", Lifecycle.DEFAULT),
    DEPLOY("deploy", Lifecycle.DEFAULT),

    PRE_SITE("pre-site", Lifecycle.SITE),
    SITE("site", Lifecycle.SITE),
    POST_SITE("post-site", Lifecycle.SITE),
    SITE_DEPLOY("site-deploy", Lifecycle.SITE);

    companion object {
        fun find(taskName: String) = Phase.values().find { it.phaseName == taskName }
    }
}
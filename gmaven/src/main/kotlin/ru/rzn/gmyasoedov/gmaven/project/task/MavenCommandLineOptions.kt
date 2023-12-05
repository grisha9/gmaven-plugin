package ru.rzn.gmyasoedov.gmaven.project.task

object MavenCommandLineOptions {
    val allOptions: Set<Option> = setOf(
        Option("-am", "--also-make", "If project list is specified, also build projects required by the list"),
        Option("-amd", "--also-make-dependents", "If project list is specified, also build projects that depend on projects on the list"),
        Option("-B", "--batch-mode", "Run in non-interactive (batch) mode (disables output color)"),
        Option("-b", "--builder", "The id of the build strategy to use"),
        Option("-C", "--strict-checksums", "Fail the build if checksums don't match"),
        Option("-c", "--lax-checksums", "Warn if checksums don't match"),
        Option("-cpu", "--check-plugin-updates", "Ineffective, only kept for backward compatibility"),
        Option("-D", "--define", "Define a system property"),
        Option("-e", "--errors", "Produce execution error messages"),
        Option("-emp", "--encrypt-master-password", "Encrypt master security password"),
        Option("-ep", "--encrypt-password", "Encrypt server password"),
        Option("-f", "--file", "Force the use of an alternate POM file (or directory with pom.xml)"),
        Option("-fae", "--fail-at-end", "Only fail the build afterwards; allow all non-impacted builds to continue"),
        Option("-ff", "--fail-fast", "Stop at first failure in reactorized builds"),
        Option("-fn", "--fail-never", "NEVER fail the build, regardless of project result"),
        Option("-gs", "--global-settings", "Alternate path for the global settings file"),
        Option("-gt", "--global-toolchains", "Alternate path for the global toolchains file"),
        Option("-h", "--help", "Display help information"),
        Option("-l", "--log-file", "Log file where all build output will go (disables output color)"),
        Option("-llr", "--legacy-local-repository", "Use Maven 2 Legacy Local Repository behaviour, ie no use of _remote.repositories. Can also be activated by using -Dmaven.legacyLocalRepo=true"),
        Option("-N", "--non-recursive", "Do not recurse into sub-projects"),
        Option("-npr", "--no-plugin-registry", "Ineffective, only kept for backward compatibility"),
        Option("-npu", "--no-plugin-updates", "Ineffective, only kept for backward compatibility"),
        Option("-nsu", "--no-snapshot-updates", "Suppress SNAPSHOT updates"),
        Option("-o", "--offline", "Work offline"),
        Option("-P", "--activate-profiles", "Comma-delimited list of profiles to activate"),
        Option("-pl", "--projects", "Comma-delimited list of specified reactor projects to build instead of all projects. A project can be specified by [groupId]:artifactId or by its relative path"),
        Option("-q", "--quiet", "Quiet output - only show errors"),
        Option("-rf", "--resume-from", "Resume reactor from specified project"),
        Option("-s", "--settings", "Alternate path for the user settings file"),
        Option("-t", "--toolchains", "Alternate path for the user toolchains file"),
        Option("-T", "--threads", "Thread count, for instance 2.0C where C is core multiplied"),
        Option("-U", "--update-snapshots", "Forces a check for missing releases and updated snapshots on remote repositories"),
        Option("-up", "--update-plugins", "Ineffective, only kept for backward compatibility"),
        Option("-v", "--version", "Display version information"),
        Option("-V", "--show-version", "Display version information WITHOUT stopping build"),
        Option("-X", "--debug", "Produce execution debug output"),
    )
}

data class Option(val name: String, val longName: String, val description: String)
# GMaven Changelog

## [Unreleased]

## [233.19] - 2023-12-17
### Added
- Navigation from dependency/artifactId tag to module or local repository pom file
### Fixed
- key="gmaven.subtask.args" for sub additional task arguments
- fixed '--javaagent param' for task execution

## [233.18] - 2023-12-02
### Fixed
- optimized modules cache data
- tool window actions visibility

## [232.17] - 2023-11-18
### Added
- Navigate to config file from GMaven-tool window/Module (F4 or Right click/Open External Config)
### Fixed
- IgnoreMavenProjectAction unexpected exceptions

## [232.16] - 2023-11-12
### Added
- Quick access to maven snapshot setting [wiki](https://github.com/grisha9/gmaven-plugin/wiki/GMaven-project-settings#update-snapshot-mode).
- clearer error messages in build/run windows
### Fixed
- do not use Kotlin deprecated versions

## [232.15] - 2023-11-01
### Added
- Optimize external project model structure
- Completions for groupId/artifactId/version [Wiki](https://github.com/grisha9/gmaven-plugin/wiki/Completions). Via perform HTTP request to https://search.maven.org/. Requires disabling bundled maven plugin that logic not intersected (Otherwise, the completions from the bundled maven plugin will work).
- Ignore/unignore maven projects with submodules (Right click on pom.xnl file or module directory).

### Fixed
- https://github.com/grisha9/gmaven-plugin/issues/2
- NPE for maven 4.0.0-alpha8

## [232.14] - 2023-10-26
### Added
- Quick fix for Maven distribution
- Gutter icons for configuration files. [Wiki](https://github.com/grisha9/gmaven-plugin/wiki/GMaven-registry-keys#gmavengutterannotation)   
- Icon for Maven project configuration files

## [232.13] - 2023-10-16

### Fixed
- Maven wrapper: changing distributionUrl in maven-wrapper.properties is not picked up during import
- Removed -Xmx and -Xms prediction and optimize firstRun

## [232.12] - 2023-10-08

### Added
- Mvnd task execution (https://github.com/grisha9/gmaven-plugin/wiki/GMaven-project-settings#delegate-tasks-execution-to-maven-daemon)
- Custom lifecycles (https://github.com/grisha9/gmaven-plugin/wiki/GMaven-registry-keys#gmavenlifecycles)

## [232.11] - 2023-10-02

### Fixed
- https://github.com/grisha9/gmaven-plugin/issues/4 (support relative path)

## [232.10] - 2023-09-27

### Added
- Download sources
- Improvement maven snapshot setting

### Fixed
- Task execution - use project context (get parent build file correctly)
- Sub-module creation (redundant gitignore & groupId from parent)

## [232.8] - 2023-09-18

### Added
- use ExternalEntityData for custom model data

### Fixed
- https://github.com/grisha9/gmaven-plugin/issues/3 (experimental key="gmaven.settings.linked.modules")
- create new module with artifactId != directory
- set project jdk after first import

## [232.7] - 2023-08-29

### Added
- Kotlin JVM support


## [232.6] - 2023-08-23

### Added
- Cleanup README
- Added setting - showPluginNodes for optimizing large projects

## [232.5] - 2023-08-22

### Added
- Groovy support (maven-compiler-plugin/groovy-maven-plugin/gmavenplus-plugin)
- Eclipse/Groovy-Eclipse Java Compiler support

### Fixed
- Default compiler settings from maven.properties
- Change jdk from GMaven settings
# GMaven Changelog

## [Unreleased]

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
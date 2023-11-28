GMaven
==================

The lightweight Maven plugin that gets the project model through maven task execution.
This provides greater simplicity and original build tool behavior in obtaining project data.

### IntelliJ Plugin - https://plugins.jetbrains.com/plugin/22370-gmaven
### Plugin wiki page - https://github.com/grisha9/gmaven-plugin/wiki 

#### Articles about GMaven: 
- [dev.to](https://dev.to/grisha9/my-intellij-idea-plugin-for-maven-support-gmaven-cn9);
- [habr.com](https://habr.com/ru/articles/753828/) (Russian);


### Maven Plugin for IntelliJ IDEA

The plugin adds support for the Maven for Java language projects:
 - Import project structure to IDE
 - Original Maven behavior for getting project model
 - Execution Maven tasks 
 - Support separate modules for production and test roots
 - Groovy support
 - Kotlin JVM support


### Prerequisites

1. IntelliJ IDEA 2021.3+
2. JDK 11+
3. Maven 3.3.1+


### General information

- For open existing project structure - select GMaven plugin on the dialog box after selecting the project
- For creating new project select GMaven on build system tab on New Project Wizard


### Setup

- For manual build run the command: gradlew clean build
- And then get distribution from: gmaven-plugin/gmaven/build/distributions


### Issues
If you found a bug, please report it on https://github.com/grisha9/gmaven-plugin/issues

Wiki about issues: https://github.com/grisha9/gmaven-plugin/wiki/Issues




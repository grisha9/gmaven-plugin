package ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin;

import com.intellij.pom.java.LanguageLevel;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;

import static java.util.Collections.emptyList;

public class CompilerData {
    private final LanguageLevel sourceLevel;
    private final LanguageLevel targetLevel;
    private final LanguageLevel testSourceLevel;
    private final LanguageLevel testTargetLevel;
    private final Collection<String> arguments;
    private final Collection<String> pluginSpecificArguments;

    public CompilerData(@NotNull LanguageLevel sourceLevel,
                        @NotNull LanguageLevel targetLevel,
                        @NotNull LanguageLevel testSourceLevel,
                        @NotNull LanguageLevel testTargetLevel,
                        @NotNull Collection<String> arguments,
                        @NotNull Collection<String> pluginSpecificArguments) {
        this.sourceLevel = Objects.requireNonNull(sourceLevel);
        this.targetLevel = Objects.requireNonNull(targetLevel);
        this.testSourceLevel = Objects.requireNonNull(testSourceLevel);
        this.testTargetLevel = Objects.requireNonNull(testTargetLevel);
        this.arguments = Objects.requireNonNull(arguments);
        this.pluginSpecificArguments = Objects.requireNonNull(pluginSpecificArguments);
    }

    public CompilerData(@NotNull LanguageLevel sourceLevel,
                        @NotNull LanguageLevel targetLevel,
                        @NotNull LanguageLevel testSourceLevel,
                        @NotNull LanguageLevel testTargetLevel,
                        @NotNull Collection<String> arguments) {
        this(sourceLevel, targetLevel, testSourceLevel, testTargetLevel,arguments, emptyList());
    }

    public CompilerData(@NotNull LanguageLevel defaultLevel,
                        @NotNull Collection<String> arguments,
                        @NotNull Collection<String> pluginSpecificArguments) {
        this(defaultLevel, defaultLevel, defaultLevel, defaultLevel, arguments, pluginSpecificArguments);
    }

    @NotNull
    public LanguageLevel getSourceLevel() {
        return sourceLevel;
    }

    @NotNull
    public LanguageLevel getTargetLevel() {
        return targetLevel;
    }

    @NotNull
    public LanguageLevel getTestSourceLevel() {
        return testSourceLevel;
    }

    @NotNull
    public LanguageLevel getTestTargetLevel() {
        return testTargetLevel;
    }

    @NotNull
    public Collection<String> getArguments() {
        return arguments;
    }

    public Collection<String> getPluginSpecificArguments() {
        return pluginSpecificArguments;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CompilerData that = (CompilerData) o;

        if (sourceLevel != that.sourceLevel) return false;
        if (targetLevel != that.targetLevel) return false;
        if (testSourceLevel != that.testSourceLevel) return false;
        if (testTargetLevel != that.testTargetLevel) return false;
        return Objects.equals(arguments, that.arguments);
    }

    @Override
    public int hashCode() {
        int result = sourceLevel != null ? sourceLevel.hashCode() : 0;
        result = 31 * result + (targetLevel != null ? targetLevel.hashCode() : 0);
        result = 31 * result + (testSourceLevel != null ? testSourceLevel.hashCode() : 0);
        result = 31 * result + (testTargetLevel != null ? testTargetLevel.hashCode() : 0);
        result = 31 * result + (arguments != null ? arguments.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "CompilerData{" +
                "sourceLevel=" + sourceLevel +
                ", targetLevel=" + targetLevel +
                ", arguments=" + arguments +
                '}';
    }
}

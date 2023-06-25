package ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin;

import com.intellij.pom.java.LanguageLevel;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Objects;

public class CompilerData {
    private final LanguageLevel sourceLevel;
    private final LanguageLevel targetLevel;
    private final LanguageLevel testSourceLevel;
    private final LanguageLevel testTargetLevel;
    private final Collection<String> annotationProcessorPaths;
    private final Collection<String> arguments;

    public CompilerData(@Nonnull LanguageLevel sourceLevel,
                        @Nonnull LanguageLevel targetLevel,
                        @Nonnull LanguageLevel testSourceLevel,
                        @Nonnull LanguageLevel testTargetLevel,
                        @Nonnull Collection<String> annotationProcessorPaths,
                        @Nonnull Collection<String> arguments) {
        this.sourceLevel = Objects.requireNonNull(sourceLevel);
        this.targetLevel = Objects.requireNonNull(targetLevel);
        this.testSourceLevel = Objects.requireNonNull(testSourceLevel);
        this.testTargetLevel = Objects.requireNonNull(testTargetLevel);
        this.annotationProcessorPaths = Objects.requireNonNull(annotationProcessorPaths);
        this.arguments = Objects.requireNonNull(arguments);
    }

    public CompilerData(@Nonnull LanguageLevel defaultLevel,
                        @Nonnull Collection<String> annotationProcessorPaths,
                        @Nonnull Collection<String> arguments) {
        this(defaultLevel, defaultLevel, defaultLevel, defaultLevel, annotationProcessorPaths, arguments);
    }

    @Nonnull
    public LanguageLevel getSourceLevel() {
        return sourceLevel;
    }

    @Nonnull
    public LanguageLevel getTargetLevel() {
        return targetLevel;
    }

    @Nonnull
    public LanguageLevel getTestSourceLevel() {
        return testSourceLevel;
    }

    @Nonnull
    public LanguageLevel getTestTargetLevel() {
        return testTargetLevel;
    }

    @Nonnull
    public Collection<String> getAnnotationProcessorPaths() {
        return annotationProcessorPaths;
    }

    @Nonnull
    public Collection<String> getArguments() {
        return arguments;
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
        if (!Objects.equals(annotationProcessorPaths, that.annotationProcessorPaths))
            return false;
        return Objects.equals(arguments, that.arguments);
    }

    @Override
    public int hashCode() {
        int result = sourceLevel != null ? sourceLevel.hashCode() : 0;
        result = 31 * result + (targetLevel != null ? targetLevel.hashCode() : 0);
        result = 31 * result + (testSourceLevel != null ? testSourceLevel.hashCode() : 0);
        result = 31 * result + (testTargetLevel != null ? testTargetLevel.hashCode() : 0);
        result = 31 * result + (annotationProcessorPaths != null ? annotationProcessorPaths.hashCode() : 0);
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

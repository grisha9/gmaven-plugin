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
    private final Collection<String> arguments;//todo

    public CompilerData(@Nonnull LanguageLevel sourceLevel,
                        @Nonnull LanguageLevel targetLevel,
                        @Nonnull LanguageLevel testSourceLevel,
                        @Nonnull LanguageLevel testTargetLevel,
                        @Nonnull Collection<String> arguments) {
        this.sourceLevel = Objects.requireNonNull(sourceLevel);
        this.targetLevel = Objects.requireNonNull(targetLevel);
        this.testSourceLevel = Objects.requireNonNull(testSourceLevel);
        this.testTargetLevel = Objects.requireNonNull(testTargetLevel);
        this.arguments = Objects.requireNonNull(arguments);
    }

    public CompilerData(@Nonnull LanguageLevel defaultLevel,
                        @Nonnull Collection<String> arguments) {
        this.sourceLevel = Objects.requireNonNull(defaultLevel);
        this.targetLevel = Objects.requireNonNull(defaultLevel);
        this.testSourceLevel = Objects.requireNonNull(defaultLevel);
        this.testTargetLevel = Objects.requireNonNull(defaultLevel);
        this.arguments = Objects.requireNonNull(arguments);
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
        return Objects.equals(arguments, that.arguments);
    }

    @Override
    public int hashCode() {
        int result = sourceLevel.hashCode();
        result = 31 * result + targetLevel.hashCode();
        result = 31 * result + arguments.hashCode();
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

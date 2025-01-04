package ru.rzn.gmyasoedov.gmaven.dom;

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.codeInsight.navigation.NavigationGutterIconRenderer;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.NotNullFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;

public class MyNavigationGutterIconBuilder<T> extends NavigationGutterIconBuilder<T> {

    public MyNavigationGutterIconBuilder(@NotNull Icon icon) {
        this(
                icon,
                (NotNullFunction<? super T, ? extends Collection<? extends PsiElement>>) DEFAULT_PSI_CONVERTOR,
                (NotNullFunction<? super T, ? extends Collection<? extends GotoRelatedItem>>) PSI_GOTO_RELATED_ITEM_PROVIDER
        );
    }

    protected MyNavigationGutterIconBuilder(
            @NotNull Icon icon,
            @NotNull NotNullFunction<? super T, ? extends Collection<? extends PsiElement>> converter
    ) {
        super(icon, converter);
    }

    protected MyNavigationGutterIconBuilder(
            @NotNull Icon icon,
            @NotNull NotNullFunction<? super T, ? extends Collection<? extends PsiElement>> converter,
            @Nullable final NotNullFunction<? super T, ? extends Collection<? extends GotoRelatedItem>> provider
    ) {
        super(icon, converter, provider);
    }

    @Override
    public @NotNull NavigationGutterIconRenderer createGutterIconRenderer(
            @NotNull Project project,
            @Nullable GutterIconNavigationHandler<PsiElement> navigationHandler
    ) {
        return super.createGutterIconRenderer(project, navigationHandler);
    }
}

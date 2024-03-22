package ru.rzn.gmyasoedov.gmaven.settings;

import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil;
import com.intellij.openapi.externalSystem.service.settings.AbstractExternalProjectSettingsControl;
import com.intellij.openapi.externalSystem.util.ExternalSystemUiUtil;
import com.intellij.openapi.externalSystem.util.PaintAwarePanel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable;
import com.intellij.openapi.roots.ui.configuration.SdkComboBox;
import com.intellij.openapi.roots.ui.configuration.SdkListItem;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel;
import com.intellij.openapi.roots.ui.util.CompositeAppearance;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.SystemInfoRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.GridBag;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rzn.gmyasoedov.gmaven.GMavenConstants;
import ru.rzn.gmyasoedov.gmaven.project.wrapper.MvnDotProperties;
import ru.rzn.gmyasoedov.gmaven.utils.MavenLog;
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.stream.Stream;

import static com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil.*;
import static com.intellij.openapi.externalSystem.util.ExternalSystemUiUtil.getLabelConstraints;
import static com.intellij.openapi.fileChooser.FileChooserDescriptorFactory.createSingleFolderDescriptor;
import static com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil.createUniqueSdkName;
import static com.intellij.openapi.roots.ui.configuration.SdkComboBoxModel.createJdkComboBoxModel;
import static com.intellij.openapi.ui.TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT;
import static java.util.Objects.requireNonNullElse;
import static ru.rzn.gmyasoedov.gmaven.bundle.GBundle.message;
import static ru.rzn.gmyasoedov.gmaven.settings.DistributionType.CUSTOM;

public class ProjectSettingsControlBuilder extends AbstractExternalProjectSettingsControl<MavenProjectSettings> {
    @NotNull
    private final MavenProjectSettings projectSettings;

    @Nullable
    private JBCheckBox nonRecursiveCheckBox;
    @Nullable
    private JBCheckBox useWholeProjectContextCheckBox;
    @Nullable
    private JBCheckBox resolveModulePerSourceSetCheckBox;
    @Nullable
    private JBCheckBox showPluginNodesCheckBox;
    @Nullable
    private JBCheckBox useMvndForTasksCheckBox;
    @Nullable
    private JTextField threadCountField;
    @Nullable
    private JTextField vmOptionsField;
    @Nullable
    private JTextField argumentsField;
    @Nullable
    private JTextField argumentsImportField;
    @Nullable
    private ComboBox<SnapshotUpdateComboBoxItem> snapshotUpdateComboBox;
    @Nullable
    private ComboBox<OutputLevelComboBoxItem> outPutLevelCombobox;
    @Nullable
    private ComboBox<DistributionSettingsComboBoxItem> mavenHomeCombobox;
    @Nullable
    private TextFieldWithBrowseButton mavenCustomPathField;
    @Nullable
    private JBLabel wrapperHintLabel;
    private SdkComboBox jdkComboBox;
    private JPanel jdkComboBoxWrapper;

    public ProjectSettingsControlBuilder(@NotNull MavenProjectSettings initialSettings) {
        super(initialSettings);
        projectSettings = initialSettings;
    }

    @Override
    public boolean validate(MavenProjectSettings settings) {
        return false;
    }

    @Override
    public void applyExtraSettings(MavenProjectSettings settings) {
        if (nonRecursiveCheckBox != null) {
            settings.setNonRecursive(nonRecursiveCheckBox.isSelected());
        }
        if (useWholeProjectContextCheckBox != null) {
            settings.setUseWholeProjectContext(useWholeProjectContextCheckBox.isSelected());
        }
        if (resolveModulePerSourceSetCheckBox != null) {
            settings.setResolveModulePerSourceSet(resolveModulePerSourceSetCheckBox.isSelected());
        }
        if (showPluginNodesCheckBox != null) {
            settings.setShowPluginNodes(showPluginNodesCheckBox.isSelected());
        }
        if (useMvndForTasksCheckBox != null) {
            settings.setUseMvndForTasks(useMvndForTasksCheckBox.isSelected());
        }
        if (threadCountField != null) {
            settings.setThreadCount(threadCountField.getText());
        }
        if (vmOptionsField != null) {
            settings.setVmOptions(vmOptionsField.getText());
        }
        if (argumentsField != null) {
            settings.setArguments(argumentsField.getText());
        }
        if (argumentsImportField != null) {
            settings.setArgumentsImport(argumentsImportField.getText());
        }
        if (snapshotUpdateComboBox != null && snapshotUpdateComboBox.getItem() != null) {
            settings.setSnapshotUpdateType(snapshotUpdateComboBox.getItem().value);
        }
        if (outPutLevelCombobox != null && outPutLevelCombobox.getItem() != null) {
            settings.setOutputLevel(outPutLevelCombobox.getItem().value);
        }
        if (jdkComboBox != null && jdkComboBox.getSelectedItem() != null) {
            settings.setJdkName(getJdkName(jdkComboBox.getSelectedItem()));
        }
        if (mavenHomeCombobox != null && mavenCustomPathField != null && mavenHomeCombobox.getItem() != null) {
            var distributionSettings = mavenHomeCombobox.getItem().value;
            if (distributionSettings.getType() == CUSTOM) {
                distributionSettings = new DistributionSettings(CUSTOM, Path.of(mavenCustomPathField.getText()), null);
            }
            settings.setDistributionSettings(distributionSettings);
        }
    }

    @Override
    public boolean isExtraSettingModified() {
        if (nonRecursiveCheckBox != null
                && nonRecursiveCheckBox.isSelected() != projectSettings.getNonRecursive()) {
            return true;
        }
        if (useWholeProjectContextCheckBox != null
                && useWholeProjectContextCheckBox.isSelected() != projectSettings.getUseWholeProjectContext()) {
            return true;
        }
        if (resolveModulePerSourceSetCheckBox != null
                && resolveModulePerSourceSetCheckBox.isSelected() != projectSettings.getResolveModulePerSourceSet()) {
            return true;
        }
        if (showPluginNodesCheckBox != null
                && showPluginNodesCheckBox.isSelected() != projectSettings.getShowPluginNodes()) {
            return true;
        }
        if (useMvndForTasksCheckBox != null
                && useMvndForTasksCheckBox.isSelected() != projectSettings.getUseMvndForTasks()) {
            return true;
        }
        if (threadCountField != null && !Objects.equals(
                threadCountField.getText(), requireNonNullElse(projectSettings.getThreadCount(), ""))
        ) {
            return true;
        }
        if (vmOptionsField != null && !Objects.equals(
                vmOptionsField.getText(), requireNonNullElse(projectSettings.getVmOptions(), ""))
        ) {
            return true;
        }
        if (argumentsField != null && !Objects.equals(
                argumentsField.getText(), requireNonNullElse(projectSettings.getArguments(), ""))
        ) {
            return true;
        }
        if (argumentsImportField != null && !Objects.equals(
                argumentsImportField.getText(), requireNonNullElse(projectSettings.getArgumentsImport(), ""))
        ) {
            return true;
        }
        if (snapshotUpdateComboBox != null
                && !Objects.equals(snapshotUpdateComboBox.getItem().value, projectSettings.getSnapshotUpdateType())) {
            return true;
        }
        if (outPutLevelCombobox != null
                && !Objects.equals(outPutLevelCombobox.getItem().value, projectSettings.getOutputLevel())) {
            return true;
        }
        if (jdkComboBox != null
                && !Objects.equals(getJdkName(jdkComboBox.getSelectedItem()), projectSettings.getJdkName())) {
            return true;
        }
        if (mavenHomeCombobox != null && mavenCustomPathField != null && mavenHomeCombobox.getItem() != null) {
            var distributionSettings = mavenHomeCombobox.getItem().value;
            DistributionSettings current = projectSettings.getDistributionSettings();
            if (distributionSettings.getType() == CUSTOM && current.getType() == CUSTOM
                    && !Objects.equals(Path.of(mavenCustomPathField.getText()), current.getPath())) {
                return true;
            } else if (!Objects.equals(distributionSettings.getType(), current.getType())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public @Nullable String getHelpId() {
        return "GMaven_Project_Settings";
    }

    @Override
    protected void resetExtraSettings(boolean isDefaultModuleCreation) {
    }

    @Override
    public void resetExtraSettings(boolean isDefaultModuleCreation, @Nullable WizardContext wizardContext) {
        if (nonRecursiveCheckBox != null) {
            nonRecursiveCheckBox.setSelected(projectSettings.getNonRecursive());
        }
        if (useWholeProjectContextCheckBox != null) {
            useWholeProjectContextCheckBox.setSelected(projectSettings.getUseWholeProjectContext());
        }
        if (resolveModulePerSourceSetCheckBox != null) {
            resolveModulePerSourceSetCheckBox.setSelected(projectSettings.getResolveModulePerSourceSet());
        }
        if (showPluginNodesCheckBox != null) {
            showPluginNodesCheckBox.setSelected(projectSettings.getShowPluginNodes());
        }
        if (useMvndForTasksCheckBox != null) {
            useMvndForTasksCheckBox.setSelected(projectSettings.getUseMvndForTasks());
        }
        if (threadCountField != null) {
            threadCountField.setText(projectSettings.getThreadCount());
        }
        if (vmOptionsField != null) {
            vmOptionsField.setText(projectSettings.getVmOptions());
        }
        if (argumentsField != null) {
            argumentsField.setText(projectSettings.getArguments());
        }
        if (argumentsImportField != null) {
            argumentsImportField.setText(projectSettings.getArgumentsImport());
        }
        if (snapshotUpdateComboBox != null) {
            snapshotUpdateComboBox.setItem(new SnapshotUpdateComboBoxItem(projectSettings.getSnapshotUpdateType()));
        }
        if (outPutLevelCombobox != null) {
            outPutLevelCombobox.setItem(new OutputLevelComboBoxItem(projectSettings.getOutputLevel()));
        }

        Project project = getProject();
        if (project == null) return;
        if (mavenCustomPathField != null) {
            mavenCustomPathField.addBrowseFolderListener(
                    message("gmaven.settings.project.maven.custom.path.title"), null, project,
                    createSingleFolderDescriptor(), TEXT_FIELD_WHOLE_TEXT
            );
        }
        if (mavenHomeCombobox != null) {
            mavenHomeCombobox.removeAllItems();
            mavenHomeCombobox.addItem(
                    new DistributionSettingsComboBoxItem(DistributionSettings.getBundled())
            );
            String externalProjectPath = projectSettings.getExternalProjectPath();
            var distributionUrl = MvnDotProperties.getDistributionUrl(project, externalProjectPath);
            if (!distributionUrl.isEmpty()) {
                mavenHomeCombobox.addItem(
                        new DistributionSettingsComboBoxItem(DistributionSettings.getWrapper(distributionUrl))
                );
            }

            DistributionSettings current = projectSettings.getDistributionSettings();
            var mavenHome = MavenUtils.resolveMavenHome();
            if (mavenHome != null) {
                mavenHomeCombobox.addItem(
                        new DistributionSettingsComboBoxItem(DistributionSettings.getLocal(mavenHome.toPath()))
                );
            }

            var customDistribution = current.getType() == CUSTOM
                    ? current : new DistributionSettings(CUSTOM, null, null);
            mavenHomeCombobox.addItem(new DistributionSettingsComboBoxItem(customDistribution));
            mavenHomeCombobox.setItem(new DistributionSettingsComboBoxItem(current));
            setupMavenHomeHintAndCustom(current);
        }
        if (jdkComboBoxWrapper != null) {
            Sdk projectSdk = wizardContext != null ? wizardContext.getProjectJdk() : null;

            ProjectStructureConfigurable structureConfigurable = ProjectStructureConfigurable.getInstance(project);
            ProjectSdksModel sdksModel = structureConfigurable.getProjectJdksModel();

            setupProjectSdksModel(sdksModel, project, projectSdk);
            recreateJdkComboBox(project, sdksModel);
            setSelectedJdk(jdkComboBox, projectSettings.getJdkName());
        }
        setPreferredSize();
    }

    private void setSelectedJdk(@Nullable SdkComboBox jdkComboBox, @Nullable String jdkName) {
        if (jdkComboBox == null) return;
        if (Objects.equals(jdkName, USE_PROJECT_JDK)) {
            jdkComboBox.setSelectedItem(jdkComboBox.showProjectSdkItem());
        } else if (Objects.equals(jdkName, USE_JAVA_HOME)) {
            jdkComboBox.setSelectedItem(jdkComboBox.showProjectSdkItem());
        } else if (jdkName == null) {
            jdkComboBox.setSelectedItem(jdkComboBox.showNoneSdkItem());
        } else {
            jdkComboBox.setSelectedSdk(jdkName);
        }
    }

    @Override
    public void fillExtraControls(PaintAwarePanel content, int indentLevel) {
        nonRecursiveCheckBox = new JBCheckBox(message("gmaven.settings.project.recursive"));
        content.add(nonRecursiveCheckBox, ExternalSystemUiUtil.getFillLineConstraints(indentLevel));

        useWholeProjectContextCheckBox = new JBCheckBox(message("gmaven.settings.project.task.context"));
        useWholeProjectContextCheckBox.setToolTipText(message("gmaven.settings.project.task.context.tooltip"));
        content.add(useWholeProjectContextCheckBox, ExternalSystemUiUtil.getFillLineConstraints(indentLevel));

        resolveModulePerSourceSetCheckBox = new JBCheckBox(message("gmaven.settings.project.module.per.source.set"));
        content.add(resolveModulePerSourceSetCheckBox, ExternalSystemUiUtil.getFillLineConstraints(indentLevel));

        useMvndForTasksCheckBox = new JBCheckBox(message("gmaven.settings.project.mvnd"));
        useMvndForTasksCheckBox.setToolTipText(message("gmaven.settings.project.mvnd.tooltip"));
        content.add(useMvndForTasksCheckBox, ExternalSystemUiUtil.getFillLineConstraints(indentLevel));

        showPluginNodesCheckBox = new JBCheckBox(message("gmaven.settings.project.plugins"));
        showPluginNodesCheckBox.setToolTipText(message("gmaven.settings.project.plugins.tooltip"));
        content.add(showPluginNodesCheckBox, ExternalSystemUiUtil.getFillLineConstraints(indentLevel));

        snapshotUpdateComboBox = setupSnapshotUpdateComboBox();
        JBLabel snapshotUpdateLabel = new JBLabel(message("gmaven.settings.project.snapshot.update"));
        content.add(snapshotUpdateLabel, getLabelConstraints(indentLevel));
        content.add(snapshotUpdateComboBox, getLabelConstraints(0));
        content.add(Box.createGlue(), ExternalSystemUiUtil.getFillLineConstraints(indentLevel));
        snapshotUpdateLabel.setLabelFor(snapshotUpdateComboBox);

        outPutLevelCombobox = setupOutputLevelComboBox();
        JBLabel outputLevelLabel = new JBLabel(message("gmaven.settings.project.output.level"));
        content.add(outputLevelLabel, getLabelConstraints(indentLevel));
        content.add(outPutLevelCombobox, getLabelConstraints(0));
        content.add(Box.createGlue(), ExternalSystemUiUtil.getFillLineConstraints(indentLevel));
        outputLevelLabel.setLabelFor(outPutLevelCombobox);


        JBLabel threadCountLabel = new JBLabel(message("gmaven.settings.project.thread.count"));
        threadCountField = new JTextField();
        content.add(threadCountLabel, getLabelConstraints(indentLevel));
        content.add(threadCountField, getLabelConstraints(0));
        content.add(Box.createGlue(), ExternalSystemUiUtil.getFillLineConstraints(indentLevel));
        threadCountLabel.setLabelFor(threadCountField);


        JBLabel vmOptionsLabel = new JBLabel(message("gmaven.settings.project.vm.options"));
        vmOptionsField = new JTextField();
        content.add(vmOptionsLabel, getLabelConstraints(indentLevel));
        content.add(vmOptionsField, getLabelConstraints(0));
        content.add(Box.createGlue(), ExternalSystemUiUtil.getFillLineConstraints(indentLevel));
        vmOptionsLabel.setLabelFor(vmOptionsField);

        JBLabel argumentsLabel = new JBLabel(message("gmaven.settings.project.arguments"));
        argumentsLabel.setToolTipText(message("gmaven.settings.project.arguments.tooltip"));
        argumentsField = new JTextField();
        argumentsField.setToolTipText(message("gmaven.settings.project.arguments.tooltip"));
        content.add(argumentsLabel, getLabelConstraints(indentLevel));
        content.add(argumentsField, getLabelConstraints(0));
        content.add(Box.createGlue(), ExternalSystemUiUtil.getFillLineConstraints(indentLevel));
        argumentsLabel.setLabelFor(argumentsField);

        JBLabel argumentsImportLabel = new JBLabel(message("gmaven.settings.project.arguments.import"));
        argumentsImportLabel.setToolTipText(message("gmaven.settings.project.arguments.import.tooltip"));
        argumentsImportField = new JTextField();
        argumentsImportField.setToolTipText(message("gmaven.settings.project.arguments.import.tooltip"));
        content.add(argumentsImportLabel, getLabelConstraints(indentLevel));
        content.add(argumentsImportField, getLabelConstraints(0));
        content.add(Box.createGlue(), ExternalSystemUiUtil.getFillLineConstraints(indentLevel));
        argumentsImportLabel.setLabelFor(argumentsImportField);


        JBLabel jdkLabel = new JBLabel(message("gmaven.settings.project.jvm"));
        jdkComboBoxWrapper = new JPanel(new BorderLayout());
        jdkLabel.setLabelFor(jdkComboBoxWrapper);
        content.add(jdkLabel, getLabelConstraints(indentLevel));
        content.add(jdkComboBoxWrapper, getLabelConstraints(0));
        content.add(Box.createGlue(), ExternalSystemUiUtil.getFillLineConstraints(indentLevel));


        JBLabel mavenHomeLabel = new JBLabel(message("gmaven.settings.project.maven.home"));
        mavenHomeCombobox = setupMavenHomeComboBox();
        content.add(mavenHomeLabel, getLabelConstraints(indentLevel));
        content.add(mavenHomeCombobox, getLabelConstraints(0));
        content.add(Box.createGlue(), ExternalSystemUiUtil.getFillLineConstraints(indentLevel));
        mavenHomeLabel.setLabelFor(mavenHomeCombobox);

        JBLabel hintLabel = new JBLabel("", UIUtil.ComponentStyle.MINI);
        wrapperHintLabel = new JBLabel("", UIUtil.ComponentStyle.MINI);
        GridBag constraints = getLabelConstraints(indentLevel);
        constraints.insets.top = 0;
        content.add(hintLabel, constraints);
        constraints = getLabelConstraints(0);
        constraints.insets.top = 0;
        content.add(wrapperHintLabel, constraints);
        content.add(Box.createGlue(), ExternalSystemUiUtil.getFillLineConstraints(indentLevel));
        hintLabel.setLabelFor(wrapperHintLabel);

        JBLabel mavenCustomPathLabel = new JBLabel();
        mavenCustomPathField = new TextFieldWithBrowseButton();
        content.add(mavenCustomPathLabel, getLabelConstraints(indentLevel));
        content.add(mavenCustomPathField, getLabelConstraints(0));
        content.add(Box.createGlue(), ExternalSystemUiUtil.getFillLineConstraints(indentLevel));
        mavenCustomPathLabel.setLabelFor(mavenCustomPathField);
    }

    private void setPreferredSize() {
        List<? extends JComponent> components = Arrays
                .asList(
                        vmOptionsField, argumentsField, argumentsImportField, threadCountField, mavenCustomPathField,
                        outPutLevelCombobox, mavenHomeCombobox, jdkComboBox, snapshotUpdateComboBox
                );
        JComponent maxWidthComponent = components.stream()
                .filter(Objects::nonNull)
                .max(Comparator.comparing(v1 -> v1.getPreferredSize().width))
                .orElse(null);
        if (maxWidthComponent == null) return;
        components.stream()
                .filter(Objects::nonNull)
                .forEach(c -> c.setPreferredSize(maxWidthComponent.getPreferredSize()));
    }

    private void recreateJdkComboBox(@NotNull Project project, @NotNull ProjectSdksModel sdksModel) {
        if (jdkComboBox != null) {
            jdkComboBoxWrapper.remove(jdkComboBox);
        }
        jdkComboBox = new SdkComboBox(createJdkComboBoxModel(project, sdksModel));
        jdkComboBoxWrapper.add(jdkComboBox, BorderLayout.CENTER);
    }

    @NotNull
    private ComboBox<OutputLevelComboBoxItem> setupOutputLevelComboBox() {
        OutputLevelComboBoxItem[] levels = Arrays.stream(OutputLevelType.values())
                .map(OutputLevelComboBoxItem::new)
                .toArray(OutputLevelComboBoxItem[]::new);
        var combobox = new ComboBox<>(levels);
        combobox.setRenderer(new MyItemCellRenderer<>());
        return combobox;
    }

    @NotNull
    private ComboBox<SnapshotUpdateComboBoxItem> setupSnapshotUpdateComboBox() {
        SnapshotUpdateComboBoxItem[] levels = Arrays.stream(SnapshotUpdateType.values())
                .map(SnapshotUpdateComboBoxItem::new)
                .toArray(SnapshotUpdateComboBoxItem[]::new);
        var combobox = new ComboBox<>(levels);
        combobox.setRenderer(new MyItemCellRenderer<>());
        return combobox;
    }

    @NotNull
    private ComboBox<DistributionSettingsComboBoxItem> setupMavenHomeComboBox() {
        DistributionSettingsComboBoxItem[] items = Stream.of(
                        DistributionSettings.getBundled()
                )
                .map(DistributionSettingsComboBoxItem::new)
                .toArray(DistributionSettingsComboBoxItem[]::new);

        var combobox = new ComboBox<>(items);
        combobox.setRenderer(new MyItemCellRenderer<>());
        combobox.addItemListener(e -> {
            DistributionSettingsComboBoxItem item = (DistributionSettingsComboBoxItem) e.getItem();
            DistributionSettings value = item.value;
            setupMavenHomeHintAndCustom(value);
        });
        combobox.setSelectedItem(null);
        return combobox;
    }

    private void setupMavenHomeHintAndCustom(DistributionSettings value) {
        if (mavenCustomPathField != null && value != null) {
            mavenCustomPathField.setVisible(value.getType() == CUSTOM);
            if (value.getType() == CUSTOM && value.getPath() != null) {
                mavenCustomPathField.setText(value.getPath().toString());
            } else {
                mavenCustomPathField.setText(null);
            }
        }
        if (wrapperHintLabel != null && value != null) {
            wrapperHintLabel.setVisible(value.getType() != CUSTOM);
            if (value.getType() == DistributionType.MVN) {
                wrapperHintLabel.setText(value.getPath().toString());
            } else {
                wrapperHintLabel.setText(value.getUrl());
            }
        }
    }

    private static void setupProjectSdksModel(@NotNull ProjectSdksModel sdksModel, @NotNull Project project, @Nullable Sdk projectSdk) {
        sdksModel.reset(project);
        deduplicateSdkNames(sdksModel);
        if (projectSdk == null) {
            projectSdk = sdksModel.getProjectSdk();
            projectSdk = sdksModel.findSdk(projectSdk);
        }
        if (projectSdk != null) {
            projectSdk = ExternalSystemJdkUtil.resolveDependentJdk(projectSdk);
            projectSdk = sdksModel.findSdk(projectSdk.getName());
        }
        sdksModel.setProjectSdk(projectSdk);
    }

    private static void deduplicateSdkNames(@NotNull ProjectSdksModel projectSdksModel) {
        Set<String> processedNames = new HashSet<>();
        Collection<Sdk> editableSdks = projectSdksModel.getProjectSdks().values();
        for (Sdk sdk : editableSdks) {
            if (processedNames.contains(sdk.getName())) {
                SdkModificator sdkModificator = sdk.getSdkModificator();
                String name = createUniqueSdkName(sdk.getName(), editableSdks);
                sdkModificator.setName(name);
                sdkModificator.commitChanges();
            }
            processedNames.add(sdk.getName());
        }
    }

    private static String getJdkName(@Nullable SdkListItem item) {
        if (item instanceof SdkListItem.ProjectSdkItem) {
            return USE_PROJECT_JDK;
        } else if (item instanceof SdkListItem.SdkItem) {
            return ((SdkListItem.SdkItem) item).sdk.getName();
        } else if (item instanceof SdkListItem.InvalidSdkItem) {
            return ((SdkListItem.InvalidSdkItem) item).sdkName;
        } else if (item instanceof SdkListItem.SdkReferenceItem) {
            return getJdkReferenceName((SdkListItem.SdkReferenceItem) item);
        } else {
            return null;
        }
    }

    private static String getJdkReferenceName(SdkListItem.SdkReferenceItem item) {
        if (JAVA_HOME.equals(item.name)) {
            return JAVA_HOME;
        } else {
            return item.name;
        }
    }


    public enum OutputLevelType {
        DEFAULT, QUITE, DEBUG
    }

    public enum SnapshotUpdateType {
        DEFAULT, NEVER, FORCE
    }

    private final class OutputLevelComboBoxItem extends MyItem<OutputLevelType> {

        private OutputLevelComboBoxItem(@NotNull OutputLevelType value) {
            super(Objects.requireNonNull(value));
        }

        @Override
        protected String getText() {
            return StringUtil.capitalize(value.name().toLowerCase());
        }

        @Override
        protected String getComment() {
            return null;
        }
    }

    private final class SnapshotUpdateComboBoxItem extends MyItem<SnapshotUpdateType> {

        private SnapshotUpdateComboBoxItem(@NotNull SnapshotUpdateType value) {
            super(Objects.requireNonNull(value));
        }

        @Override
        protected String getText() {
            return StringUtil.capitalize(value.name().toLowerCase());
        }

        @Override
        protected String getComment() {
            return null;
        }
    }

    private final class DistributionSettingsComboBoxItem extends MyItem<DistributionSettings> {

        private DistributionSettingsComboBoxItem(@NotNull DistributionSettings value) {
            super(Objects.requireNonNull(value));
        }

        @Override
        protected String getText() {
            return getText(value);
        }

        @Override
        protected String getComment() {
            return null;
        }

        @NotNull
        private String getText(DistributionSettings settings) {
            String text = StringUtil.capitalize(settings.getType().name().toLowerCase());
            if (settings.getType() == DistributionType.BUNDLED) {
                text += "(maven version: " + GMavenConstants.BUNDLED_MAVEN_VERSION + ")";
            }
            if (settings.getType() == DistributionType.MVN) {
                text = "Maven home(mvn)";
                try {
                    String mavenVersion = MavenUtils.getMavenVersion(settings.getPath().toFile());
                    if (mavenVersion != null) {
                        text += ": " + mavenVersion;
                    }
                } catch (Exception e) {
                    MavenLog.LOG.warn(e);
                }
            }
            if (settings.getType() == DistributionType.WRAPPER) {
                text = "Use Maven wrapper";
            }
            return text;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DistributionSettingsComboBoxItem)) return false;
            DistributionSettingsComboBoxItem item = (DistributionSettingsComboBoxItem) o;
            return Objects.equals(value.getType(), item.value.getType());
        }

        @Override
        public int hashCode() {
            return Objects.hash(value.getType());
        }
    }

    private static abstract class MyItem<T> {
        @NotNull
        protected final T value;

        private MyItem(@NotNull T value) {
            this.value = value;
        }

        @NlsContexts.ListItem
        protected abstract String getText();

        @NlsContexts.ListItem
        protected abstract String getComment();

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MyItem)) return false;
            MyItem item = (MyItem) o;
            return Objects.equals(value, item.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    private static class MyItemCellRenderer<T> extends ColoredListCellRenderer<MyItem<T>> {

        @Override
        protected void customizeCellRenderer(@NotNull JList<? extends MyItem<T>> list,
                                             MyItem<T> value,
                                             int index,
                                             boolean selected,
                                             boolean hasFocus) {
            if (value == null) return;
            CompositeAppearance.DequeEnd ending = new CompositeAppearance().getEnding();
            ending.addText(value.getText(), getTextAttributes(selected));
            if (value.getComment() != null) {
                SimpleTextAttributes commentAttributes = getCommentAttributes(selected);
                ending.addComment(value.getComment(), commentAttributes);
            }
            ending.getAppearance().customize(this);
        }

        @NotNull
        private static SimpleTextAttributes getTextAttributes(boolean selected) {
            return selected && !(SystemInfoRt.isWindows && UIManager.getLookAndFeel().getName().contains("Windows"))
                    ? SimpleTextAttributes.SELECTED_SIMPLE_CELL_ATTRIBUTES
                    : SimpleTextAttributes.SIMPLE_CELL_ATTRIBUTES;
        }

        @NotNull
        private static SimpleTextAttributes getCommentAttributes(boolean selected) {
            return SystemInfo.isMac && selected
                    ? new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.WHITE)
                    : SimpleTextAttributes.GRAY_ATTRIBUTES;
        }
    }
}

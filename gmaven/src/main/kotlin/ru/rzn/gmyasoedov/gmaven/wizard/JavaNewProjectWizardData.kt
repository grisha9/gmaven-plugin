package ru.rzn.gmyasoedov.gmaven.wizard

import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.util.Key

interface JavaNewProjectWizardData : MavenNewProjectWizardData {

  val addSampleCodeProperty: GraphProperty<Boolean>

  var addSampleCode: Boolean

  val generateOnboardingTipsProperty: GraphProperty<Boolean>

  var generateOnboardingTips: Boolean

  companion object {

    val KEY = Key.create<JavaNewProjectWizardData>(JavaNewProjectWizardData::class.java.name)

    @JvmStatic
    val NewProjectWizardStep.javaMavenData: JavaNewProjectWizardData?
      get() = data.getUserData(KEY)
  }
}
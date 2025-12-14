package com.wooongyee.jvmbaekjoon.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class JvmBaekjoonConfigurable : Configurable {

    private var jdkPathField: TextFieldWithBrowseButton? = null
    private var kotlinCompilerPathField: TextFieldWithBrowseButton? = null

    override fun getDisplayName(): String = "JvmBaekjoon"

    override fun createComponent(): JComponent {
        val settings = JvmBaekjoonSettings.getInstance()

        jdkPathField = TextFieldWithBrowseButton().apply {
            text = settings.state.jdkPath
            addActionListener(
                TextBrowseFolderListener(
                    FileChooserDescriptorFactory.createSingleFolderDescriptor().apply {
                        title = "Select JDK Home"
                        description = "JDK 홈 디렉토리를 선택하세요 (예: /usr/lib/jvm/java-17-openjdk)"
                    },
                    null
                )
            )
        }

        kotlinCompilerPathField = TextFieldWithBrowseButton().apply {
            text = settings.state.kotlinCompilerPath
            addActionListener(
                TextBrowseFolderListener(
                    FileChooserDescriptor(true, false, false, false, false, false).apply {
                        title = "Select Kotlin Compiler"
                        description = "kotlinc 실행 파일을 선택하세요"
                    },
                    null
                )
            )
        }

        return FormBuilder.createFormBuilder()
            .addLabeledComponent(
                JBLabel("JDK Home (비어있으면 프로젝트 SDK 사용):"),
                jdkPathField!!,
                1,
                false
            )
            .addLabeledComponent(
                JBLabel("Kotlin Compiler (kotlinc):"),
                kotlinCompilerPathField!!,
                1,
                false
            )
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    override fun isModified(): Boolean {
        val settings = JvmBaekjoonSettings.getInstance()
        return jdkPathField?.text != settings.state.jdkPath ||
                kotlinCompilerPathField?.text != settings.state.kotlinCompilerPath
    }

    override fun apply() {
        val settings = JvmBaekjoonSettings.getInstance()
        settings.state.jdkPath = jdkPathField?.text ?: ""
        settings.state.kotlinCompilerPath = kotlinCompilerPathField?.text ?: ""
    }

    override fun reset() {
        val settings = JvmBaekjoonSettings.getInstance()
        jdkPathField?.text = settings.state.jdkPath
        kotlinCompilerPathField?.text = settings.state.kotlinCompilerPath
    }
}

package com.wooongyee.jvmbaekjoon.toolWindow.components

import com.intellij.ui.JBColor
import com.wooongyee.jvmbaekjoon.model.TestCase
import java.awt.Component
import java.awt.Dimension
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel

object TestCasePanel {

    fun create(testCase: TestCase, onCopy: (String, JButton) -> Unit): JPanel {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            background = JBColor.background()
            alignmentY = Component.TOP_ALIGNMENT

            // 입력 박스
            add(IOBoxPanel.create("입력", testCase.input, onCopy).apply {
                alignmentY = Component.TOP_ALIGNMENT
            })

            // 간격
            add(Box.createRigidArea(Dimension(10, 0)))

            // 출력 박스
            add(IOBoxPanel.create("출력", testCase.output, onCopy).apply {
                alignmentY = Component.TOP_ALIGNMENT
            })
        }
    }
}

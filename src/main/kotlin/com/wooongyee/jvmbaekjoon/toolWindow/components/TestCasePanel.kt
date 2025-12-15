package com.wooongyee.jvmbaekjoon.toolWindow.components

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.wooongyee.jvmbaekjoon.model.TestCase
import java.awt.*
import javax.swing.*

object TestCasePanel {

    fun create(testCase: TestCase, onCopy: (String, JButton) -> Unit): JPanel {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = JBColor.background()
            alignmentX = Component.LEFT_ALIGNMENT

            // 입력/출력 박스 (가로 배치)
            add(JPanel().apply {
                layout = BoxLayout(this, BoxLayout.X_AXIS)
                background = JBColor.background()
                alignmentX = Component.LEFT_ALIGNMENT

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
            })

            // 예제 설명 (있는 경우) - HTML 렌더링
            testCase.explanation?.let { explanation ->
                add(Box.createRigidArea(Dimension(0, 10)))
                add(createExplanationHtmlPanel(explanation))
            }
        }
    }

    /**
     * 예제 설명 패널 생성 (HTML 렌더링)
     */
    private fun createExplanationHtmlPanel(explanationHtml: String): JPanel {
        return JPanel().apply {
            layout = BorderLayout()
            background = JBColor.background()
            alignmentX = Component.LEFT_ALIGNMENT

            add(JBLabel("📝 설명:").apply {
                font = Font("Dialog", Font.BOLD, 12)
                border = BorderFactory.createEmptyBorder(0, 0, 5, 0)
            }, BorderLayout.NORTH)

            val editorPane = JEditorPane("text/html", explanationHtml).apply {
                isEditable = false
                background = JBColor.background()
                putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
                font = Font("Dialog", Font.PLAIN, 12)
            }

            // 스크롤 없이 그대로 추가
            add(editorPane, BorderLayout.CENTER)
        }
    }

}

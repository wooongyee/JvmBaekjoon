package com.wooongyee.jvmbaekjoon.toolWindow.components

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.ui.JBColor
import com.wooongyee.jvmbaekjoon.model.BojProblem
import java.awt.*
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

object ProblemHeaderPanel {

    fun create(problem: BojProblem): JPanel {
        return JPanel(BorderLayout()).apply {
            background = JBColor.background()

            // 제목
            val titleLabel = JLabel("${problem.number}. ${problem.title}").apply {
                font = font.deriveFont(Font.BOLD, 16f)
            }
            add(titleLabel, BorderLayout.WEST)

            // 링크 버튼
            val linkButton = createLinkButton(problem.number)
            val buttonWrapper = JPanel(FlowLayout(FlowLayout.RIGHT, 0, 0)).apply {
                background = JBColor.background()
                add(linkButton)
            }
            add(buttonWrapper, BorderLayout.EAST)
        }
    }

    private fun createLinkButton(problemNumber: String): JButton {
        return JButton(AllIcons.Actions.Forward).apply {
            toolTipText = "BOJ 사이트에서 보기"
            isBorderPainted = false
            isContentAreaFilled = false
            isFocusPainted = false
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            preferredSize = Dimension(24, 24)
            maximumSize = Dimension(24, 24)
            minimumSize = Dimension(24, 24)
            addActionListener {
                BrowserUtil.browse("https://www.acmicpc.net/problem/$problemNumber")
            }
        }
    }
}

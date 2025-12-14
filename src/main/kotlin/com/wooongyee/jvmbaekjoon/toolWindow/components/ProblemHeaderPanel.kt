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

    fun create(problem: BojProblem, onSubmit: () -> Unit): JPanel {
        return JPanel(BorderLayout()).apply {
            background = JBColor.background()

            // 제목
            val titleLabel = JLabel("${problem.number}. ${problem.title}").apply {
                font = font.deriveFont(Font.BOLD, 16f)
            }
            add(titleLabel, BorderLayout.WEST)

            // 버튼들 (제출, 링크)
            val buttonWrapper = JPanel(FlowLayout(FlowLayout.RIGHT, 5, 0)).apply {
                background = JBColor.background()
                add(createSubmitButton(onSubmit))
                add(createLinkButton(problem.number))
            }
            add(buttonWrapper, BorderLayout.EAST)
        }
    }

    private fun createSubmitButton(onSubmit: () -> Unit): JButton {
        return JButton(AllIcons.Actions.Upload).apply {
            toolTipText = "BOJ에 제출하기"
            isBorderPainted = false
            isContentAreaFilled = false
            isFocusPainted = false
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            preferredSize = Dimension(24, 24)
            maximumSize = Dimension(24, 24)
            minimumSize = Dimension(24, 24)
            addActionListener { onSubmit() }
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

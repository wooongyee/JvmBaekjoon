package com.wooongyee.jvmbaekjoon.toolWindow.components

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.wooongyee.jvmbaekjoon.model.TestResult
import java.awt.*
import javax.swing.*

object TestResultPanel {

    private val PASS_COLOR = JBColor(Color(0, 150, 0), Color(100, 200, 100))
    private val FAIL_COLOR = JBColor(Color(200, 0, 0), Color(255, 100, 100))

    /**
     * 테스트 결과 패널 생성
     */
    fun create(results: List<TestResult>): JPanel {
        val panel = JPanel(BorderLayout()).apply {
            background = JBColor.background()
            border = JBUI.Borders.empty(10)
        }

        // 헤더 - 요약
        val passedCount = results.count { it.passed }
        val totalCount = results.size
        val allPassed = passedCount == totalCount

        val headerPanel = JPanel(BorderLayout()).apply {
            background = JBColor.background()
            border = JBUI.Borders.empty(0, 0, 10, 0)

            val statusLabel = JLabel(if (allPassed) "All Passed!" else "Failed").apply {
                font = font.deriveFont(Font.BOLD, 16f)
                foreground = if (allPassed) PASS_COLOR else FAIL_COLOR
            }

            val countLabel = JLabel("$passedCount / $totalCount 통과").apply {
                font = font.deriveFont(Font.PLAIN, 14f)
            }

            add(statusLabel, BorderLayout.WEST)
            add(countLabel, BorderLayout.EAST)
        }
        panel.add(headerPanel, BorderLayout.NORTH)

        // 결과 리스트
        val resultsPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = JBColor.background()
        }

        results.forEach { result ->
            resultsPanel.add(createResultItem(result))
            resultsPanel.add(Box.createRigidArea(Dimension(0, 8)))
        }

        val scrollPane = JBScrollPane(resultsPanel).apply {
            border = BorderFactory.createEmptyBorder()
            verticalScrollBar.unitIncrement = 16
        }
        panel.add(scrollPane, BorderLayout.CENTER)

        return panel
    }

    /**
     * 개별 테스트 결과 아이템
     */
    private fun createResultItem(result: TestResult): JPanel {
        val panel = JPanel(BorderLayout()).apply {
            background = JBColor.background()
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(JBColor.border(), 1),
                JBUI.Borders.empty(8)
            )
            maximumSize = Dimension(Int.MAX_VALUE, 200)
        }

        // 헤더 (테스트 번호 + 상태 + 시간)
        val headerPanel = JPanel(BorderLayout()).apply {
            isOpaque = false

            val statusIcon = when {
                result.timedOut -> "⏱️"
                result.passed -> "✅"
                else -> "❌"
            }
            val statusText = when {
                result.timedOut -> "시간 초과"
                result.passed -> "정답"
                else -> "오답"
            }

            val titleLabel = JLabel("$statusIcon 예제 ${result.testCaseIndex + 1} - $statusText").apply {
                font = font.deriveFont(Font.BOLD, 13f)
                foreground = if (result.passed) PASS_COLOR else FAIL_COLOR
            }

            val timeLabel = JLabel("${result.executionTimeMs}ms").apply {
                font = font.deriveFont(Font.PLAIN, 12f)
                foreground = JBColor.GRAY
            }

            add(titleLabel, BorderLayout.WEST)
            add(timeLabel, BorderLayout.EAST)
        }
        panel.add(headerPanel, BorderLayout.NORTH)

        // 실패한 경우 상세 내용 표시
        if ((!result.passed && !result.timedOut) || result.error != null) {
            val detailPanel = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = false
                border = JBUI.Borders.empty(8, 0, 0, 0)
            }

            if (!result.passed && !result.timedOut) {
                // 기대 출력
                detailPanel.add(createOutputSection("기대 출력:", result.expectedOutput))
                detailPanel.add(Box.createRigidArea(Dimension(0, 4)))

                // 실제 출력
                detailPanel.add(createOutputSection("실제 출력:", result.actualOutput))
            }

            // 에러 메시지 (시간 초과 제외)
            if (!result.timedOut) {
                result.error?.let { error ->
                    detailPanel.add(Box.createRigidArea(Dimension(0, 4)))
                    detailPanel.add(createOutputSection("에러:", error, FAIL_COLOR))
                }
            }

            panel.add(detailPanel, BorderLayout.CENTER)
        }

        return panel
    }

    /**
     * 출력 섹션 생성
     */
    private fun createOutputSection(
        label: String,
        content: String,
        labelColor: Color = JBColor.GRAY
    ): JPanel {
        return JPanel(BorderLayout()).apply {
            isOpaque = false

            val labelComp = JLabel(label).apply {
                font = font.deriveFont(Font.BOLD, 11f)
                foreground = labelColor
            }

            val textArea = JTextArea(content).apply {
                isEditable = false
                font = Font("Monospaced", Font.PLAIN, 12)
                background = JBColor(Color(245, 245, 245), Color(50, 50, 50))
                border = JBUI.Borders.empty(4)
                rows = minOf(content.lines().size, 5)
            }

            add(labelComp, BorderLayout.NORTH)
            add(textArea, BorderLayout.CENTER)
        }
    }
}

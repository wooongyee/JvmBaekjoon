package com.wooongyee.jvmbaekjoon.toolWindow.components

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import com.wooongyee.jvmbaekjoon.model.ProblemStats
import java.awt.*
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

object ProblemStatsPanel {

    fun create(stats: ProblemStats): JPanel {
        val panel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createLineBorder(JBColor.border(), 1)
            background = JBColor.background()
        }

        val columnNames = arrayOf("시간 제한", "메모리 제한", "제출", "정답", "맞힌 사람", "정답 비율")
        val data = arrayOf(
            arrayOf<Any>(
                stats.timeLimit,
                stats.memoryLimit,
                stats.submitCount,
                stats.correctCount,
                stats.acceptedUserCount,
                stats.correctRate
            )
        )

        val model = object : DefaultTableModel(data, columnNames) {
            override fun isCellEditable(row: Int, column: Int) = false
        }

        val table = JBTable(model).apply {
            setShowGrid(true)
            gridColor = JBColor.border()
            rowHeight = 28
            tableHeader.reorderingAllowed = false
            tableHeader.resizingAllowed = true
            autoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS

            tableHeader.defaultRenderer = object : DefaultTableCellRenderer() {
                init {
                    horizontalAlignment = SwingConstants.CENTER
                    font = font.deriveFont(Font.BOLD)
                }
            }

            setDefaultRenderer(Any::class.java, object : DefaultTableCellRenderer() {
                init {
                    horizontalAlignment = SwingConstants.CENTER
                }
            })
        }

        panel.add(JBScrollPane(table).apply {
            border = JBUI.Borders.empty()
            preferredSize = Dimension(0, 60)
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        }, BorderLayout.CENTER)

        return panel
    }
}

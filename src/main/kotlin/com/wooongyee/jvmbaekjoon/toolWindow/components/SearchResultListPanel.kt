package com.wooongyee.jvmbaekjoon.toolWindow.components

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.wooongyee.jvmbaekjoon.model.ProblemSearchResult
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

object SearchResultListPanel {

    /**
     * 검색 결과 리스트 패널 생성
     * @param results 검색 결과 목록
     * @param onSelect 항목 선택 시 콜백 (문제 번호 전달)
     */
    fun create(results: List<ProblemSearchResult>, onSelect: (String) -> Unit): JPanel {
        val panel = JPanel(BorderLayout()).apply {
            background = JBColor.background()
            border = JBUI.Borders.empty(10)
        }

        // 헤더
        val headerLabel = JLabel("검색 결과: ${results.size}개 (최대 50개)").apply {
            font = font.deriveFont(Font.BOLD, 14f)
            border = JBUI.Borders.empty(0, 0, 10, 0)
        }
        panel.add(headerLabel, BorderLayout.NORTH)

        // 결과 리스트
        val listModel = DefaultListModel<ProblemSearchResult>().apply {
            results.forEach { addElement(it) }
        }

        val resultList = JBList(listModel).apply {
            cellRenderer = SearchResultCellRenderer()
            selectionMode = ListSelectionModel.SINGLE_SELECTION

            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (e.clickCount == 2) {
                        val selected = selectedValue
                        if (selected != null) {
                            onSelect(selected.number)
                        }
                    }
                }
            })
        }

        val listScrollPane = JBScrollPane(resultList).apply {
            border = BorderFactory.createLineBorder(JBColor.border(), 1)
        }
        panel.add(listScrollPane, BorderLayout.CENTER)

        // 안내 메시지
        val hintLabel = JLabel("더블클릭하여 문제 열기").apply {
            foreground = JBColor.GRAY
            font = font.deriveFont(Font.ITALIC, 12f)
            border = JBUI.Borders.empty(10, 0, 0, 0)
        }
        panel.add(hintLabel, BorderLayout.SOUTH)

        return panel
    }

    /**
     * 검색 결과 셀 렌더러
     */
    private class SearchResultCellRenderer : ListCellRenderer<ProblemSearchResult> {
        private val panel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(8, 10)
        }
        private val numberLabel = JLabel().apply {
            foreground = JBColor.GRAY
            font = font.deriveFont(Font.BOLD, 12f)
        }
        private val titleLabel = JLabel().apply {
            font = font.deriveFont(Font.PLAIN, 13f)
        }

        override fun getListCellRendererComponent(
            list: JList<out ProblemSearchResult>,
            value: ProblemSearchResult,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            numberLabel.text = "${value.number}."
            titleLabel.text = value.title

            panel.removeAll()

            val leftPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0)).apply {
                isOpaque = false
                add(numberLabel)
                add(titleLabel)
            }
            panel.add(leftPanel, BorderLayout.WEST)

            if (isSelected) {
                panel.background = JBColor(Color(0, 120, 215), Color(75, 110, 175))
                numberLabel.foreground = Color.WHITE
                titleLabel.foreground = Color.WHITE
            } else {
                panel.background = JBColor.background()
                numberLabel.foreground = JBColor.GRAY
                titleLabel.foreground = JBColor.foreground()
            }

            return panel
        }
    }
}

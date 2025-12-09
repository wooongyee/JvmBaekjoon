package com.wooongyee.jvmbaekjoon.toolWindow.components

import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.datatransfer.StringSelection
import javax.swing.*

object IOBoxPanel {

    fun create(label: String, text: String, onCopy: (String, JButton) -> Unit): JPanel {
        val outerPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createLineBorder(JBColor.border(), 1)
            background = JBColor(Color(248, 249, 250), Color(45, 47, 49))
            alignmentY = Component.TOP_ALIGNMENT
        }

        // 헤더
        val header = JPanel(BorderLayout()).apply {
            background = JBColor(Color(248, 249, 250), Color(45, 47, 49))
            border = JBUI.Borders.empty(8, 8, 4, 8)
        }

        val labelComp = JLabel(label).apply {
            font = font.deriveFont(Font.BOLD, 12f)
        }
        header.add(labelComp, BorderLayout.WEST)

        // 복사 버튼
        val copyButton = JButton(AllIcons.Actions.Copy).apply {
            toolTipText = "복사"
            isBorderPainted = false
            isContentAreaFilled = false
            isFocusPainted = false
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            preferredSize = Dimension(20, 20)
            maximumSize = Dimension(20, 20)
            minimumSize = Dimension(20, 20)

            val self = this
            addActionListener {
                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                clipboard.setContents(StringSelection(text), null)
                onCopy(label, self)
            }
        }
        header.add(copyButton, BorderLayout.EAST)

        outerPanel.add(header, BorderLayout.NORTH)

        // 텍스트 영역
        val textArea = JTextArea(text).apply {
            isEditable = false
            lineWrap = false
            wrapStyleWord = false
            background = JBColor(Color(248, 249, 250), Color(45, 47, 49))
            font = Font("Monospaced", Font.PLAIN, 13)
            border = JBUI.Borders.empty(8)
        }

        val lineCount = text.lines().size
        val lineHeight = textArea.getFontMetrics(textArea.font).height
        val totalHeight = lineCount * lineHeight + 16

        textArea.preferredSize = Dimension(0, totalHeight)

        val scrollPane = JBScrollPane(textArea).apply {
            border = JBUI.Borders.empty()
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_NEVER
        }

        outerPanel.add(scrollPane, BorderLayout.CENTER)

        val headerHeight = 40
        outerPanel.preferredSize = Dimension(300, totalHeight + headerHeight)
        outerPanel.maximumSize = Dimension(Int.MAX_VALUE, totalHeight + headerHeight)

        return outerPanel
    }
}

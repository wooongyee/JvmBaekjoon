package com.wooongyee.jvmbaekjoon.toolWindow

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import com.wooongyee.jvmbaekjoon.services.BojCrawlerService
import com.wooongyee.jvmbaekjoon.services.BojProblem
import com.wooongyee.jvmbaekjoon.services.TestCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.awt.*
import java.awt.datatransfer.StringSelection
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.text.html.HTMLEditorKit

class BojToolWindowContent(private val project: Project) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var problemNumberField: JBTextField
    private var currentProblem: BojProblem? = null
    private lateinit var mainPanel: JPanel
    private lateinit var contentScrollPane: JBScrollPane

    fun getContent(): JComponent {
        val scrollPane = JBScrollPane().apply {
            border = JBUI.Borders.empty()
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        }
        contentScrollPane = scrollPane

        mainPanel = panel {
            row {
                label("문제 번호:")

                textField()
                    .columns(10)
                    .applyToComponent {
                        problemNumberField = this
                        addActionListener { loadProblem() }
                    }

                cell(JButton("로드").apply {
                    addActionListener { loadProblem() }
                })
            }

            row {
                cell(scrollPane)
                    .align(Align.FILL)
            }.resizableRow()
        }.apply {
            border = JBUI.Borders.empty(10)
        }

        showWelcomeMessage()

        return mainPanel
    }
    private fun showWelcomeMessage() {
        val welcomePanel = JPanel(GridBagLayout()).apply {
            background = JBColor.background()
        }

        val label = JLabel("문제 번호를 입력하고 '로드' 버튼을 눌러주세요").apply {
            foreground = JBColor.GRAY
            font = font.deriveFont(Font.PLAIN, 15f)  // 크기 증가
            horizontalAlignment = SwingConstants.CENTER
        }

        welcomePanel.add(label)  // GridBagLayout은 기본적으로 중앙 배치

        contentScrollPane.setViewportView(welcomePanel)
    }

    private fun loadProblem() {
        val number = problemNumberField.text.trim()

        if (number.isEmpty()) {
            JOptionPane.showMessageDialog(
                mainPanel,
                "문제 번호를 입력해주세요",
                "입력 오류",
                JOptionPane.WARNING_MESSAGE
            )
            return
        }

        showLoadingMessage()

        scope.launch {
            val result = BojCrawlerService.fetchProblem(number)

            result.onSuccess { problem ->
                currentProblem = problem
                displayProblem(problem)
            }.onFailure { error ->
                showErrorMessage(error.message ?: "알 수 없는 오류")
            }
        }
    }
    private fun showLoadingMessage() {
        val loadingPanel = JPanel(GridBagLayout()).apply {
            background = JBColor.background()
        }

        val gbc = GridBagConstraints().apply {
            gridx = 0
            gridy = GridBagConstraints.RELATIVE
            insets = Insets(5, 0, 10, 0)
            anchor = GridBagConstraints.CENTER
        }

        // 작고 회색 진행바
        val progressBar = JProgressBar().apply {
            isIndeterminate = true
            preferredSize = Dimension(120, 6)
            border = null
        }
        loadingPanel.add(progressBar, gbc)

        // 로딩 메시지
        val label = JLabel("문제를 불러오는 중...").apply {
            foreground = JBColor.GRAY
            font = font.deriveFont(Font.PLAIN, 14f)
            horizontalAlignment = SwingConstants.CENTER
        }
        gbc.insets = Insets(10, 0, 0, 0)
        loadingPanel.add(label, gbc)

        contentScrollPane.setViewportView(loadingPanel)
    }

    private fun showErrorMessage(message: String) {
        val errorPanel = panel {
            row {
                label("오류: $message")
                    .applyToComponent {
                        foreground = JBColor.RED
                        horizontalAlignment = SwingConstants.CENTER
                    }
            }.resizableRow()
        }
        contentScrollPane.setViewportView(errorPanel)
    }

    private fun displayProblem(problem: BojProblem) {
        val wrapper = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(10)
        }

        val problemPanel = panel {
            // 문제 헤더
            row {
                cell(createProblemHeader(problem))
                    .align(Align.FILL)
            }

            // 통계 테이블
            row {
                cell(createStatsTable(problem.stats))
                    .align(Align.FILL)
            }.topGap(TopGap.SMALL)

            // 문제
            row {
                label("문제")
                    .bold()
                    .applyToComponent {
                        font = font.deriveFont(Font.BOLD, 14f)
                    }
            }.topGap(TopGap.MEDIUM)

            row {
                panel { separator() }
            }

            row {
                cell(createFormattedTextPane(problem.description))
                    .align(Align.FILL)
            }.topGap(TopGap.SMALL)

            // 입력
            row {
                label("입력")
                    .bold()
                    .applyToComponent {
                        font = font.deriveFont(Font.BOLD, 14f)
                    }
            }.topGap(TopGap.MEDIUM)

            row {
                panel { separator() }
            }

            row {
                cell(createFormattedTextPane(problem.input))
                    .align(Align.FILL)
            }.topGap(TopGap.SMALL)

            // 출력
            row {
                label("출력")
                    .bold()
                    .applyToComponent {
                        font = font.deriveFont(Font.BOLD, 14f)
                    }
            }.topGap(TopGap.MEDIUM)

            row {
                panel { separator() }
            }

            row {
                cell(createFormattedTextPane(problem.output))
                    .align(Align.FILL)
            }.topGap(TopGap.SMALL)

            // 테스트 케이스
            if (problem.testCases.isNotEmpty()) {
                row {
                    label("테스트 케이스")
                        .bold()
                        .applyToComponent {
                            font = font.deriveFont(Font.BOLD, 14f)
                        }
                }.topGap(TopGap.MEDIUM)

                // 테스트 케이스 섹션 구분선
                row {
                    panel { separator() }
                }

                problem.testCases.forEachIndexed { index, testCase ->
                    // 헤더 (예제 번호 + 실행 버튼)
                    row {
                        cell(createTestCaseHeader(index + 1) {
                            onRunTestCase(index + 1, testCase)
                        })
                    }.topGap(TopGap.SMALL)

                    // 입출력 박스 (예제 번호 아래 살짝 공백)
                    row {
                        cell(createTestCasePanel(testCase))
                            .align(Align.FILL)
                    }.topGap(TopGap.SMALL)

                    // 예제 끝에 구분선 (마지막 예제 제외)
                    if (index < problem.testCases.size - 1) {
                        row {
                            panel { separator() }
                        }.topGap(TopGap.SMALL)
                    }
                }
            }
        }

        wrapper.add(problemPanel, BorderLayout.NORTH)
        contentScrollPane.setViewportView(wrapper)
    }

    private fun createProblemHeader(problem: BojProblem): JPanel {
        return JPanel(BorderLayout()).apply {
            background = JBColor.background()

            val titleLabel = JLabel("${problem.number}. ${problem.title}").apply {
                font = font.deriveFont(Font.BOLD, 16f)
            }
            add(titleLabel, BorderLayout.WEST)

            val linkButton = createIconButton(AllIcons.Actions.Forward, "BOJ 사이트에서 보기") { _ ->
                BrowserUtil.browse("https://www.acmicpc.net/problem/${problem.number}")
            }.apply {
                preferredSize = Dimension(24, 24)
                maximumSize = Dimension(24, 24)
                minimumSize = Dimension(24, 24)
            }

            val buttonWrapper = JPanel(FlowLayout(FlowLayout.RIGHT, 0, 0)).apply {
                background = JBColor.background()
                add(linkButton)
            }

            add(buttonWrapper, BorderLayout.EAST)
        }
    }

    /**
     * 테스트 케이스 헤더 (예제 번호 + 실행 버튼 바로 옆)
     */
    private fun createTestCaseHeader(number: Int, onRun: () -> Unit): JPanel {
        return JPanel(FlowLayout(FlowLayout.LEFT, 5, 0)).apply {
            background = JBColor.background()

            // 예제 번호
            add(JLabel("예제 $number"))

            // 실행 버튼 (바로 옆)
            add(createIconButton(AllIcons.Actions.Execute, "실행") { _ ->
                onRun()
            }.apply {
                preferredSize = Dimension(20, 20)
                maximumSize = Dimension(20, 20)
                minimumSize = Dimension(20, 20)
            })
        }
    }

    /**
     * HTML 스타일 텍스트 패널 (자동 줄바꿈, 가독성 좋음)
     */
    /**
     * 포맷된 텍스트 영역 (줄바꿈 보존)
     */
    private fun createFormattedTextPane(text: String): JTextArea {
        return JTextArea(text).apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = false  // 단어 단위가 아닌 글자 단위로 줄바꿈
            background = JBColor.background()
            border = JBUI.Borders.empty(5, 0, 5, 0)
            isOpaque = false
            font = Font(Font.SANS_SERIF, Font.PLAIN, 13)
        }
    }

    /**
     * 텍스트를 HTML로 변환 (줄바꿈, 리스트 형식 보존)
     */
    private fun convertTextToHtml(text: String): String {
        val lines = text.lines()
        val html = StringBuilder("<html><body style='font-family: sans-serif; font-size: 13px; line-height: 1.6;'>")

        var inList = false

        for (line in lines) {
            val trimmed = line.trim()

            when {
                // 빈 줄
                trimmed.isEmpty() -> {
                    if (inList) {
                        html.append("</ul>")
                        inList = false
                    }
                    html.append("<br>")
                }
                // 리스트 항목 (•, -, *, 숫자. 등)
                trimmed.startsWith("•") || trimmed.startsWith("-") || trimmed.startsWith("*") ||
                        trimmed.matches(Regex("^\\d+\\..*")) -> {
                    if (!inList) {
                        html.append("<ul style='margin: 5px 0; padding-left: 20px;'>")
                        inList = true
                    }
                    val content = trimmed.replaceFirst(Regex("^[•\\-*]\\s*|^\\d+\\.\\s*"), "")
                    html.append("<li>").append(escapeHtml(content)).append("</li>")
                }
                // 일반 텍스트
                else -> {
                    if (inList) {
                        html.append("</ul>")
                        inList = false
                    }
                    html.append("<p style='margin: 5px 0;'>").append(escapeHtml(line)).append("</p>")
                }
            }
        }

        if (inList) {
            html.append("</ul>")
        }

        html.append("</body></html>")
        return html.toString()
    }

    /**
     * HTML 이스케이프
     */
    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    private fun createIconButton(icon: Icon, tooltip: String, action: (JButton) -> Unit): JButton {
        return JButton(icon).apply {
            toolTipText = tooltip
            isBorderPainted = false
            isContentAreaFilled = false
            isFocusPainted = false
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            preferredSize = Dimension(24, 24)

            val self = this
            addActionListener { action(self) }
        }
    }

    private fun createStatsTable(stats: com.wooongyee.jvmbaekjoon.services.ProblemStats): JPanel {
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

    /**
     * 테스트 케이스 패널 (각 박스 독립적 높이)
     */
    private fun createTestCasePanel(testCase: TestCase): JPanel {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            background = JBColor.background()
            alignmentY = Component.TOP_ALIGNMENT  // 전체 패널 위쪽 정렬

            // 입력 박스 (위쪽 정렬)
            add(createIOBox("입력", testCase.input).apply {
                alignmentY = Component.TOP_ALIGNMENT
            })

            // 간격
            add(Box.createRigidArea(Dimension(10, 0)))

            // 출력 박스 (위쪽 정렬)
            add(createIOBox("출력", testCase.output).apply {
                alignmentY = Component.TOP_ALIGNMENT
            })
        }
    }
    /**
     * 입출력 박스 (내용에 맞는 높이)
     */
    private fun createIOBox(label: String, text: String): JPanel {
        val outerPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createLineBorder(JBColor.border(), 1)
            background = JBColor(Color(248, 249, 250), Color(45, 47, 49))
            alignmentY = Component.TOP_ALIGNMENT  // 위쪽 정렬
        }

        val header = JPanel(BorderLayout()).apply {
            background = JBColor(Color(248, 249, 250), Color(45, 47, 49))
            border = JBUI.Borders.empty(8, 8, 4, 8)
        }

        val labelComp = JLabel(label).apply {
            font = font.deriveFont(Font.BOLD, 12f)
        }
        header.add(labelComp, BorderLayout.WEST)

        val copyButton = createIconButton(AllIcons.Actions.Copy, "복사") { button ->
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(StringSelection(text), null)
            showCopyNotification(label, button)
        }.apply {
            preferredSize = Dimension(20, 20)
            maximumSize = Dimension(20, 20)
            minimumSize = Dimension(20, 20)
        }
        header.add(copyButton, BorderLayout.EAST)

        outerPanel.add(header, BorderLayout.NORTH)

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

        // preferredSize만 설정하고 maximumSize는 높이 제한 없이
        val headerHeight = 40
        outerPanel.preferredSize = Dimension(300, totalHeight + headerHeight)
        outerPanel.maximumSize = Dimension(Int.MAX_VALUE, totalHeight + headerHeight)  // 높이도 고정

        return outerPanel
    }


    private fun showCopyNotification(label: String, button: JComponent) {
        JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(
                "$label 복사됨",
                MessageType.INFO,
                null
            )
            .setFadeoutTime(1500)
            .createBalloon()
            .show(
                RelativePoint.getCenterOf(button),
                Balloon.Position.above
            )
    }

    private fun onRunTestCase(number: Int, testCase: TestCase) {
        JOptionPane.showMessageDialog(
            mainPanel,
            "테스트 케이스 $number 실행\n(다음 단계에서 구현)",
            "실행",
            JOptionPane.INFORMATION_MESSAGE
        )
    }
}

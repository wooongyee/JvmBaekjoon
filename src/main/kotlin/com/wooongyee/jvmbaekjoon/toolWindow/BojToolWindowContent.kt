package com.wooongyee.jvmbaekjoon.toolWindow

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.util.ui.JBUI
import com.wooongyee.jvmbaekjoon.services.BojCrawlerService
import com.wooongyee.jvmbaekjoon.services.BojProblem
import com.wooongyee.jvmbaekjoon.services.TestCase
import com.wooongyee.jvmbaekjoon.toolWindow.components.ProblemHeaderPanel
import com.wooongyee.jvmbaekjoon.toolWindow.components.ProblemStatsPanel
import com.wooongyee.jvmbaekjoon.toolWindow.components.TestCasePanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.awt.*
import javax.swing.*

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
            font = font.deriveFont(Font.PLAIN, 15f)
            horizontalAlignment = SwingConstants.CENTER
        }

        welcomePanel.add(label)

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

        val progressBar = JProgressBar().apply {
            isIndeterminate = true
            preferredSize = Dimension(120, 6)
            border = null
        }
        loadingPanel.add(progressBar, gbc)

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
            // 문제 헤더 (컴포넌트 사용)
            row {
                cell(ProblemHeaderPanel.create(problem))
                    .align(Align.FILL)
            }

            // 통계 테이블 (컴포넌트 사용)
            row {
                cell(ProblemStatsPanel.create(problem.stats))
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

                    // 입출력 박스 (컴포넌트 사용)
                    row {
                        cell(TestCasePanel.create(testCase) { label, button ->
                            showCopyNotification(label, button)
                        })
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

    private fun createTestCaseHeader(number: Int, onRun: () -> Unit): JPanel {
        return JPanel(FlowLayout(FlowLayout.LEFT, 5, 0)).apply {
            background = JBColor.background()

            add(JLabel("예제 $number"))

            add(JButton(AllIcons.Actions.Execute).apply {
                toolTipText = "실행"
                isBorderPainted = false
                isContentAreaFilled = false
                isFocusPainted = false
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                preferredSize = Dimension(20, 20)
                maximumSize = Dimension(20, 20)
                minimumSize = Dimension(20, 20)
                addActionListener { onRun() }
            })
        }
    }

    private fun createFormattedTextPane(text: String): JTextArea {
        return JTextArea(text).apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = false
            background = JBColor.background()
            border = JBUI.Borders.empty(5, 0, 5, 0)
            isOpaque = false
            font = Font(Font.SANS_SERIF, Font.PLAIN, 13)
        }
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

package com.wooongyee.jvmbaekjoon.toolWindow

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import java.awt.datatransfer.StringSelection
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.util.ui.JBUI
import com.wooongyee.jvmbaekjoon.model.BojProblem
import com.wooongyee.jvmbaekjoon.model.ProblemSearchResult
import com.wooongyee.jvmbaekjoon.model.TestCase
import com.wooongyee.jvmbaekjoon.services.BojProblemService
import com.wooongyee.jvmbaekjoon.services.BojTestService
import com.wooongyee.jvmbaekjoon.toolWindow.components.ProblemHeaderPanel
import com.wooongyee.jvmbaekjoon.toolWindow.components.ProblemStatsPanel
import com.wooongyee.jvmbaekjoon.toolWindow.components.SearchResultListPanel
import com.wooongyee.jvmbaekjoon.toolWindow.components.TestCasePanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.awt.*
import javax.swing.*

class BojToolWindowContent(private val project: Project) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val problemService = BojProblemService()

    private lateinit var inputField: JBTextField
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
                label("문제 번호/제목:")

                textField()
                    .columns(15)
                    .applyToComponent {
                        inputField = this
                        addActionListener { loadOrSearchProblem() }
                    }

                cell(JButton("검색").apply {
                    addActionListener { loadOrSearchProblem() }
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

        val label = JLabel("문제 번호 또는 제목을 입력하세요").apply {
            foreground = JBColor.GRAY
            font = font.deriveFont(Font.PLAIN, 15f)
            horizontalAlignment = SwingConstants.CENTER
        }

        welcomePanel.add(label)

        contentScrollPane.setViewportView(welcomePanel)
    }

    private fun loadOrSearchProblem() {
        val input = inputField.text.trim()

        if (input.isEmpty()) {
            JOptionPane.showMessageDialog(
                mainPanel,
                "문제 번호 또는 제목을 입력해주세요",
                "입력 오류",
                JOptionPane.WARNING_MESSAGE
            )
            return
        }

        // 이미 같은 문제가 표시되어 있으면 중복 검색 방지
        if (currentProblem != null && input == currentProblem!!.number) {
            return
        }

        showLoadingMessage()

        scope.launch {
            when (val result = problemService.loadOrSearch(input)) {
                is BojProblemService.SearchResult.SingleProblem -> {
                    currentProblem = result.problem
                    displayProblem(result.problem)
                }
                is BojProblemService.SearchResult.MultipleResults -> {
                    currentProblem = null
                    displaySearchResults(result.results)
                }
                is BojProblemService.SearchResult.NotFound -> {
                    currentProblem = null
                    showNotFoundMessage(result.message)
                }
                is BojProblemService.SearchResult.Error -> {
                    currentProblem = null
                    showErrorMessage(result.message)
                }
            }
        }
    }

    /**
     * 특정 문제 번호로 직접 로드
     */
    private fun loadProblemByNumber(number: String) {
        showLoadingMessage()

        scope.launch {
            when (val result = problemService.loadOrSearch(number)) {
                is BojProblemService.SearchResult.SingleProblem -> {
                    currentProblem = result.problem
                    displayProblem(result.problem)
                }
                is BojProblemService.SearchResult.NotFound -> {
                    showNotFoundMessage(result.message)
                }
                is BojProblemService.SearchResult.Error -> {
                    showErrorMessage(result.message)
                }
                else -> {
                    showErrorMessage("문제를 불러올 수 없습니다")
                }
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

        val label = JLabel("검색 중...").apply {
            foreground = JBColor.GRAY
            font = font.deriveFont(Font.PLAIN, 14f)
            horizontalAlignment = SwingConstants.CENTER
        }
        gbc.insets = Insets(10, 0, 0, 0)
        loadingPanel.add(label, gbc)

        contentScrollPane.setViewportView(loadingPanel)
    }

    private fun showErrorMessage(message: String) {
        val errorPanel = JPanel(GridBagLayout()).apply {
            background = JBColor.background()
        }

        val label = JLabel("오류: $message").apply {
            foreground = JBColor.RED
            font = font.deriveFont(Font.PLAIN, 14f)
            horizontalAlignment = SwingConstants.CENTER
        }

        errorPanel.add(label)
        contentScrollPane.setViewportView(errorPanel)
    }

    private fun showNotFoundMessage(message: String) {
        val notFoundPanel = JPanel(GridBagLayout()).apply {
            background = JBColor.background()
        }

        val label = JLabel(message).apply {
            foreground = JBColor.GRAY
            font = font.deriveFont(Font.PLAIN, 14f)
            horizontalAlignment = SwingConstants.CENTER
        }

        notFoundPanel.add(label)
        contentScrollPane.setViewportView(notFoundPanel)
    }

    /**
     * 검색 결과 목록 표시 (컴포넌트 사용)
     */
    private fun displaySearchResults(results: List<ProblemSearchResult>) {
        val panel = SearchResultListPanel.create(results) { number ->
            loadProblemByNumber(number)
        }
        contentScrollPane.setViewportView(panel)
    }

    private fun displayProblem(problem: BojProblem) {
        // BojTestService에 현재 테스트케이스 저장
        BojTestService.setCurrentTestCases(problem.testCases)

        val wrapper = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(10)
        }

        val problemPanel = panel {
            // 문제 헤더 (컴포넌트 사용)
            row {
                cell(ProblemHeaderPanel.create(problem) {
                    onSubmitProblem(problem)
                })
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
                cell(createHtmlPane(problem.description))
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
                cell(createHtmlPane(problem.input))
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
                cell(createHtmlPane(problem.output))
                    .align(Align.FILL)
            }.topGap(TopGap.SMALL)

            // 제한 (있는 경우에만)
            problem.limit?.let { limitHtml ->
                row {
                    label("제한")
                        .bold()
                        .applyToComponent {
                            font = font.deriveFont(Font.BOLD, 14f)
                        }
                }.topGap(TopGap.MEDIUM)

                row {
                    panel { separator() }
                }

                row {
                    cell(createHtmlPane(limitHtml))
                        .align(Align.FILL)
                }.topGap(TopGap.SMALL)
            }

            // 테스트 케이스
            if (problem.testCases.isNotEmpty()) {
                row {
                    cell(createTestCaseHeader("테스트 케이스", isTitle = true) {
                        onRunAllTestCases(problem.testCases)
                    })
                }.topGap(TopGap.MEDIUM)

                row {
                    panel { separator() }
                }

                problem.testCases.forEachIndexed { index, testCase ->
                    // 헤더 (예제 번호 + 실행 버튼)
                    row {
                        cell(createTestCaseHeader("예제 ${index + 1}") {
                            onRunTestCase(index, testCase)
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

    private fun createTestCaseHeader(text: String, isTitle: Boolean = false, onRun: () -> Unit): JPanel {
        return JPanel(FlowLayout(FlowLayout.LEFT, 5, 0)).apply {
            background = JBColor.background()

            add(JLabel(text).apply {
                if (isTitle) {
                    font = font.deriveFont(Font.BOLD, 14f)
                }
            })

            add(JButton(AllIcons.Actions.Execute).apply {
                toolTipText = if (isTitle) "모든 테스트케이스 실행" else "실행"
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

    /**
     * HTML 렌더링 패널 생성 (이미지 포함)
     */
    private fun createHtmlPane(html: String): JComponent {
        val editorPane = JEditorPane("text/html", html).apply {
            isEditable = false
            background = JBColor.background()
            isOpaque = false

            // 하이퍼링크 비활성화
            putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)

            // 폰트 설정
            font = Font(Font.SANS_SERIF, Font.PLAIN, 13)
        }

        // JScrollPane으로 감싸서 가로 스크롤 방지
        return JBScrollPane(editorPane).apply {
            border = JBUI.Borders.empty()  // 공백 제거
            horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_NEVER

            // 크기 제약 제거 - 내용에 맞춰 자동으로 늘어나도록
            minimumSize = Dimension(0, 0)
            maximumSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)
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

    private fun onRunTestCase(index: Int, testCase: TestCase) {
        // 현재 열린 파일 가져오기
        val file = FileEditorManager.getInstance(project).selectedFiles.firstOrNull()

        if (file == null) {
            Messages.showMessageDialog(
                project,
                "열린 파일이 없습니다.\nKotlin 또는 Java 파일을 열어주세요.",
                "Run BOJ Test",
                Messages.getWarningIcon()
            )
            return
        }

        if (!BojTestService.isSupportedFile(file)) {
            Messages.showMessageDialog(
                project,
                "Kotlin 또는 Java 파일만 실행할 수 있습니다.",
                "Run BOJ Test",
                Messages.getWarningIcon()
            )
            return
        }

        // 파일 저장 (수정 중인 내용 반영)
        FileDocumentManager.getInstance().saveAllDocuments()

        // 개별 테스트케이스 실행
        BojTestService.runSingleTestCase(project, file, testCase, index)
    }

    private fun onRunAllTestCases(testCases: List<TestCase>) {
        // 현재 열린 파일 가져오기
        val file = FileEditorManager.getInstance(project).selectedFiles.firstOrNull()

        if (file == null) {
            Messages.showMessageDialog(
                project,
                "열린 파일이 없습니다.\nKotlin 또는 Java 파일을 열어주세요.",
                "Run BOJ Test",
                Messages.getWarningIcon()
            )
            return
        }

        if (!BojTestService.isSupportedFile(file)) {
            Messages.showMessageDialog(
                project,
                "Kotlin 또는 Java 파일만 실행할 수 있습니다.",
                "Run BOJ Test",
                Messages.getWarningIcon()
            )
            return
        }

        // 파일 저장 (수정 중인 내용 반영)
        FileDocumentManager.getInstance().saveAllDocuments()

        // 전체 테스트케이스 실행
        BojTestService.runTestWithCases(project, file, testCases)
    }

    private fun onSubmitProblem(problem: BojProblem) {
        // 현재 열린 파일 가져오기
        val file = FileEditorManager.getInstance(project).selectedFiles.firstOrNull()

        if (file == null) {
            Messages.showMessageDialog(
                project,
                "열린 파일이 없습니다.\n제출할 코드를 먼저 열어주세요.",
                "BOJ 제출",
                Messages.getWarningIcon()
            )
            return
        }

        try {
            // 파일 저장 (수정 중인 내용 반영)
            FileDocumentManager.getInstance().saveAllDocuments()

            // 파일 내용 읽기
            val code = String(file.contentsToByteArray(), file.charset)

            // 클립보드에 복사
            CopyPasteManager.getInstance().setContents(StringSelection(code))

            // 제출 페이지 열기
            BrowserUtil.browse("https://www.acmicpc.net/submit/${problem.number}")

            // 성공 메시지
            JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(
                    "코드가 클립보드에 복사되었습니다.<br>제출 페이지로 이동합니다.",
                    MessageType.INFO,
                    null
                )
                .setFadeoutTime(2000)
                .createBalloon()
                .show(
                    RelativePoint.getCenterOf(mainPanel),
                    Balloon.Position.above
                )
        } catch (e: Exception) {
            Messages.showMessageDialog(
                project,
                "코드를 읽는 중 오류가 발생했습니다.\n${e.message}",
                "BOJ 제출",
                Messages.getErrorIcon()
            )
        }
    }
}

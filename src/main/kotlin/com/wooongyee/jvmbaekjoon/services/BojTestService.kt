package com.wooongyee.jvmbaekjoon.services

import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import com.wooongyee.jvmbaekjoon.model.TestCase
import com.wooongyee.jvmbaekjoon.model.TestResult
import java.util.concurrent.CompletableFuture

/**
 * BOJ 테스트 실행 서비스
 */
object BojTestService {

    private var consoleView: ConsoleView? = null

    /**
     * BOJ 테스트 실행 (모든 테스트케이스)
     */
    fun runTest(project: Project, file: VirtualFile) {
        if (!isSupportedFile(file)) {
            Messages.showMessageDialog(
                project,
                "Kotlin 또는 Java 파일만 실행할 수 있습니다.",
                "Run BOJ Test",
                Messages.getWarningIcon()
            )
            return
        }

        // Tool Window 열기
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("JvmBaekjoon")
        toolWindow?.show()

        // 현재 로드된 테스트케이스가 없으면 안내
        val testCases = getCurrentTestCases()
        if (testCases.isEmpty()) {
            Messages.showMessageDialog(
                project,
                "테스트케이스가 없습니다.\n먼저 BOJ 문제를 검색해주세요.",
                "Run BOJ Test",
                Messages.getWarningIcon()
            )
            return
        }

        runTestWithCases(project, file, testCases)
    }

    /**
     * BOJ 테스트 실행 (테스트케이스 포함)
     */
    fun runTestWithCases(
        project: Project,
        file: VirtualFile,
        testCases: List<TestCase>
    ) {
        if (!isSupportedFile(file)) {
            Messages.showMessageDialog(
                project,
                "Kotlin 또는 Java 파일만 실행할 수 있습니다.",
                "Run BOJ Test",
                Messages.getWarningIcon()
            )
            return
        }

        // Run Tool Window 콘솔 생성
        val console = createConsole(project)
        console.clear()
        console.print("🔨 컴파일 중: ${file.name}\n", ConsoleViewContentType.SYSTEM_OUTPUT)

        CodeExecutorService.compile(project, file).thenAccept { compileResult ->
            ApplicationManager.getApplication().invokeLater {
                if (!compileResult.success) {
                    console.print("❌ 컴파일 실패\n", ConsoleViewContentType.ERROR_OUTPUT)
                    console.print("${compileResult.error}\n", ConsoleViewContentType.ERROR_OUTPUT)
                    return@invokeLater
                }

                console.print("✅ 컴파일 성공\n\n", ConsoleViewContentType.SYSTEM_OUTPUT)

                // 테스트케이스를 백그라운드에서 순차 실행
                CompletableFuture.runAsync {
                    val results = mutableListOf<TestResult>()

                    testCases.forEachIndexed { index, testCase ->
                        val result = CodeExecutorService.execute(
                            project = project,
                            outputPath = compileResult.outputPath!!,
                            className = compileResult.className!!,
                            input = testCase.input
                        )

                        val actualOutput = result.output.trim()
                        val expectedOutput = testCase.output.trim()
                        val passed = actualOutput == expectedOutput && !result.timedOut

                        ApplicationManager.getApplication().invokeLater {
                            console.print("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n", ConsoleViewContentType.NORMAL_OUTPUT)
                            console.print("예제 ${index + 1}\n", ConsoleViewContentType.NORMAL_OUTPUT)

                            // 결과 출력
                            when {
                                result.timedOut -> {
                                    console.print("⏱️ 시간 초과", ConsoleViewContentType.ERROR_OUTPUT)
                                    console.print(" (${result.executionTimeMs}ms)\n", ConsoleViewContentType.NORMAL_OUTPUT)
                                }
                                passed -> {
                                    console.print("✅ 정답", ConsoleViewContentType.SYSTEM_OUTPUT)
                                    console.print(" (${result.executionTimeMs}ms)\n", ConsoleViewContentType.NORMAL_OUTPUT)
                                }
                                else -> {
                                    console.print("❌ 오답", ConsoleViewContentType.ERROR_OUTPUT)
                                    console.print(" (${result.executionTimeMs}ms)\n", ConsoleViewContentType.NORMAL_OUTPUT)
                                }
                            }

                            console.print("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n", ConsoleViewContentType.NORMAL_OUTPUT)

                            // 오답이면 실제 출력 표시
                            if (!passed && !result.timedOut) {
                                console.print("$actualOutput\n\n", ConsoleViewContentType.ERROR_OUTPUT)
                            }

                            if (result.error.isNotEmpty()) {
                                console.print("⚠️ 에러:\n", ConsoleViewContentType.ERROR_OUTPUT)
                                console.print("${result.error}\n\n", ConsoleViewContentType.ERROR_OUTPUT)
                            }
                        }

                        val testResult = TestResult(
                            testCaseIndex = index,
                            passed = passed,
                            expectedOutput = expectedOutput,
                            actualOutput = actualOutput,
                            executionTimeMs = result.executionTimeMs,
                            timedOut = result.timedOut,
                            error = if (result.error.isNotEmpty()) result.error else null
                        )

                        results.add(testResult)
                    }

                    // 완료 후 UI 스레드에서 요약 표시
                    ApplicationManager.getApplication().invokeLater {
                        displaySummary(console, results)
                    }
                }
            }
        }
    }

    /**
     * 개별 테스트케이스 실행
     */
    fun runSingleTestCase(project: Project, file: VirtualFile, testCase: TestCase, index: Int) {
        if (!isSupportedFile(file)) {
            Messages.showMessageDialog(
                project,
                "Kotlin 또는 Java 파일만 실행할 수 있습니다.",
                "Run BOJ Test",
                Messages.getWarningIcon()
            )
            return
        }

        val console = createConsole(project)
        console.clear()

        CodeExecutorService.compile(project, file).thenAccept { compileResult ->
            ApplicationManager.getApplication().invokeLater {
                if (!compileResult.success) {
                    console.print("❌ 컴파일 실패\n", ConsoleViewContentType.ERROR_OUTPUT)
                    console.print("${compileResult.error}\n", ConsoleViewContentType.ERROR_OUTPUT)
                    return@invokeLater
                }

                // 단일 테스트케이스 실행
                val result = CodeExecutorService.execute(
                    project = project,
                    outputPath = compileResult.outputPath!!,
                    className = compileResult.className!!,
                    input = testCase.input
                )

                val actualOutput = result.output.trim()
                val expectedOutput = testCase.output.trim()
                val passed = actualOutput == expectedOutput && !result.timedOut

                // 결과 및 실행 시간 먼저 표시
                console.print("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n", ConsoleViewContentType.NORMAL_OUTPUT)
                console.print("예제 ${index + 1}\n", ConsoleViewContentType.NORMAL_OUTPUT)

                when {
                    result.timedOut -> {
                        console.print("⏱️ 시간 초과", ConsoleViewContentType.ERROR_OUTPUT)
                        console.print(" (${result.executionTimeMs}ms)\n", ConsoleViewContentType.NORMAL_OUTPUT)
                    }
                    passed -> {
                        console.print("✅ 정답", ConsoleViewContentType.SYSTEM_OUTPUT)
                        console.print(" (${result.executionTimeMs}ms)\n", ConsoleViewContentType.NORMAL_OUTPUT)
                    }
                    else -> {
                        console.print("❌ 오답", ConsoleViewContentType.ERROR_OUTPUT)
                        console.print(" (${result.executionTimeMs}ms)\n", ConsoleViewContentType.NORMAL_OUTPUT)
                    }
                }

                console.print("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n", ConsoleViewContentType.NORMAL_OUTPUT)

                // 오답이면 실제 출력 표시
                if (!passed && !result.timedOut) {
                    console.print("$actualOutput\n\n", ConsoleViewContentType.ERROR_OUTPUT)
                }

                // 에러가 있으면 표시
                if (result.error.isNotEmpty()) {
                    console.print("⚠️ 에러:\n", ConsoleViewContentType.ERROR_OUTPUT)
                    console.print("${result.error}\n\n", ConsoleViewContentType.ERROR_OUTPUT)
                }
            }
        }
    }

    /**
     * 결과 요약 표시
     */
    private fun displaySummary(console: ConsoleView, results: List<TestResult>) {
        val passedCount = results.count { it.passed }
        val totalCount = results.size
        val totalTime = results.sumOf { it.executionTimeMs }

        console.print("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n", ConsoleViewContentType.NORMAL_OUTPUT)
        console.print("📊 결과 요약\n", ConsoleViewContentType.NORMAL_OUTPUT)

        if (passedCount == totalCount) {
            console.print("🎉 All Passed! ", ConsoleViewContentType.SYSTEM_OUTPUT)
            console.print("($passedCount / $totalCount)\n", ConsoleViewContentType.NORMAL_OUTPUT)
        } else {
            console.print("$passedCount / $totalCount 통과\n", ConsoleViewContentType.ERROR_OUTPUT)
        }

        console.print("총 실행 시간: ${totalTime}ms\n", ConsoleViewContentType.NORMAL_OUTPUT)
        console.print("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n", ConsoleViewContentType.NORMAL_OUTPUT)
    }

    /**
     * Run Tool Window 콘솔 생성
     */
    private fun createConsole(project: Project): ConsoleView {
        val console = TextConsoleBuilderFactory.getInstance()
            .createBuilder(project)
            .console

        val descriptor = RunContentDescriptor(
            console,
            null,
            console.component,
            "BOJ Test"
        )

        RunContentManager.getInstance(project).showRunContent(
            DefaultRunExecutor.getRunExecutorInstance(),
            descriptor
        )

        consoleView = console
        return console
    }

    /**
     * 파일이 지원되는지 확인
     */
    fun isSupportedFile(file: VirtualFile?): Boolean {
        return file?.extension in listOf("kt", "java")
    }

    /**
     * 현재 로드된 테스트케이스
     */
    private var currentTestCases: List<TestCase> = emptyList()

    fun setCurrentTestCases(testCases: List<TestCase>) {
        currentTestCases = testCases
    }

    fun getCurrentTestCases(): List<TestCase> = currentTestCases
}

package com.wooongyee.jvmbaekjoon.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.compiler.CompileContext
import com.intellij.openapi.compiler.CompileStatusNotification
import com.intellij.openapi.compiler.CompilerManager
import com.intellij.openapi.compiler.CompilerMessageCategory
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.wooongyee.jvmbaekjoon.model.CompileResult
import com.wooongyee.jvmbaekjoon.model.ExecutionResult
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

object CodeExecutorService {

    private const val DEFAULT_TIMEOUT_MS = 2000L
    private const val MAX_OUTPUT_LENGTH = 10000 // 최대 출력 길이

    /**
     * 파일 컴파일 (IntelliJ 내부 API 사용)
     */
    fun compile(project: Project, file: VirtualFile): CompletableFuture<CompileResult> {
        val future = CompletableFuture<CompileResult>()

        ApplicationManager.getApplication().invokeLater {
            val module = ModuleUtil.findModuleForFile(file, project)
            if (module == null) {
                future.complete(CompileResult(false, null, null, "모듈을 찾을 수 없습니다."))
                return@invokeLater
            }

            val compilerManager = CompilerManager.getInstance(project)

            compilerManager.compile(arrayOf(file), object : CompileStatusNotification {
                override fun finished(aborted: Boolean, errors: Int, warnings: Int, context: CompileContext) {
                    if (aborted) {
                        future.complete(CompileResult(false, null, null, "컴파일이 취소되었습니다."))
                        return
                    }

                    if (errors > 0) {
                        val errorMessages = context.getMessages(CompilerMessageCategory.ERROR)
                            .joinToString("\n") { it.message }
                        future.complete(CompileResult(false, null, null, errorMessages))
                        return
                    }

                    // 컴파일 성공 - 출력 경로 찾기
                    val compilerExtension = CompilerModuleExtension.getInstance(module)
                    val outputPath = compilerExtension?.compilerOutputPath?.path

                    if (outputPath == null) {
                        future.complete(CompileResult(false, null, null, "출력 경로를 찾을 수 없습니다."))
                        return
                    }

                    // 클래스 이름 추출
                    val className = extractClassName(project, file)

                    future.complete(CompileResult(true, outputPath, className, null))
                }
            })
        }

        return future
    }

    /**
     * 컴파일된 클래스 실행
     */
    fun execute(
        outputPath: String,
        className: String,
        input: String,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS
    ): ExecutionResult {
        val startTime = System.currentTimeMillis()

        try {
            val processBuilder = ProcessBuilder("java", "-cp", outputPath, className)
                .redirectErrorStream(false)

            val process = processBuilder.start()

            // stdin에 입력 전달
            process.outputStream.bufferedWriter().use { writer ->
                writer.write(input)
                writer.flush()
            }

            // 타임아웃 대기
            val completed = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            val executionTime = System.currentTimeMillis() - startTime

            if (!completed) {
                process.destroyForcibly()
                return ExecutionResult(
                    output = "",
                    error = "시간 초과 (${timeoutMs}ms)",
                    executionTimeMs = executionTime,
                    exitCode = -1,
                    timedOut = true
                )
            }

            var output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()

            // 출력 길이 제한
            val outputTruncated = output.length > MAX_OUTPUT_LENGTH
            if (outputTruncated) {
                output = output.take(MAX_OUTPUT_LENGTH) + "\n... (출력이 너무 길어 잘렸습니다)"
            }

            return ExecutionResult(
                output = output,
                error = error,
                executionTimeMs = executionTime,
                exitCode = process.exitValue(),
                timedOut = false
            )
        } catch (e: Exception) {
            val executionTime = System.currentTimeMillis() - startTime
            return ExecutionResult(
                output = "",
                error = "실행 오류: ${e.message}",
                executionTimeMs = executionTime,
                exitCode = -1,
                timedOut = false
            )
        }
    }

    /**
     * 파일에서 클래스 이름 추출
     */
    private fun extractClassName(project: Project, file: VirtualFile): String {
        val psiFile = PsiManager.getInstance(project).findFile(file)
        val fileName = file.nameWithoutExtension

        return when (file.extension) {
            "java" -> {
                // Java: 파일명과 동일한 public class
                fileName
            }
            "kt" -> {
                // Kotlin: MainKt 형태 (main 함수가 top-level인 경우)
                // 또는 클래스 내부에 있으면 클래스명
                "${fileName}Kt"
            }
            else -> fileName
        }
    }

    /**
     * 지원되는 파일인지 확인
     */
    fun isSupportedFile(file: VirtualFile?): Boolean {
        return file?.extension in listOf("kt", "java")
    }
}

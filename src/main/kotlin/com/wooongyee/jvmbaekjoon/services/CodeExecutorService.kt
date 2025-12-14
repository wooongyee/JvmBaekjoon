package com.wooongyee.jvmbaekjoon.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.compiler.CompileContext
import com.intellij.openapi.compiler.CompileStatusNotification
import com.intellij.openapi.compiler.CompilerManager
import com.intellij.openapi.compiler.CompilerMessageCategory
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.wooongyee.jvmbaekjoon.model.CompileResult
import com.wooongyee.jvmbaekjoon.model.ExecutionResult
import com.wooongyee.jvmbaekjoon.settings.JvmBaekjoonSettings
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

object CodeExecutorService {

    private const val DEFAULT_TIMEOUT_MS = 2000L
    private const val MAX_OUTPUT_LENGTH = 10000 // 최대 출력 길이

    /**
     * 파일 컴파일 (설정된 경로가 있으면 직접 컴파일러 호출, 없으면 IntelliJ 내부 API 사용)
     */
    fun compile(project: Project, file: VirtualFile): CompletableFuture<CompileResult> {
        val settings = JvmBaekjoonSettings.getInstance()
        val jdkPath = settings.state.jdkPath
        val kotlinCompilerPath = settings.state.kotlinCompilerPath

        // 설정된 경로가 있으면 직접 컴파일러 호출
        if (jdkPath.isNotEmpty() || kotlinCompilerPath.isNotEmpty()) {
            return compileDirectly(project, file, jdkPath, kotlinCompilerPath)
        }

        // 설정이 없으면 기존 IntelliJ CompilerManager 사용
        return compileWithIntelliJ(project, file)
    }

    /**
     * 직접 컴파일러 호출 (javac/kotlinc)
     */
    private fun compileDirectly(
        project: Project,
        file: VirtualFile,
        jdkPath: String,
        kotlinCompilerPath: String
    ): CompletableFuture<CompileResult> {
        val future = CompletableFuture<CompileResult>()

        Thread {
            try {
                val sourceFile = File(file.path)
                val className = extractClassName(project, file)

                // 임시 출력 디렉토리 생성
                val tempDir = Files.createTempDirectory("jvmbaekjoon-compile").toFile()
                tempDir.deleteOnExit()

                val compileResult = when (file.extension) {
                    "java" -> compileJava(sourceFile, tempDir, jdkPath)
                    "kt" -> compileKotlin(sourceFile, tempDir, kotlinCompilerPath)
                    else -> {
                        future.complete(CompileResult(false, null, null, "지원하지 않는 파일 형식입니다."))
                        return@Thread
                    }
                }

                if (compileResult.success) {
                    future.complete(CompileResult(true, tempDir.absolutePath, className, null))
                } else {
                    future.complete(compileResult)
                }
            } catch (e: Exception) {
                future.complete(CompileResult(false, null, null, "컴파일 오류: ${e.message}"))
            }
        }.start()

        return future
    }

    /**
     * Java 파일 컴파일
     */
    private fun compileJava(sourceFile: File, outputDir: File, jdkPath: String): CompileResult {
        val javacPath = if (jdkPath.isNotEmpty()) {
            Paths.get(jdkPath, "bin", "javac").toString()
        } else {
            "javac"
        }

        val processBuilder = ProcessBuilder(
            javacPath,
            "-d", outputDir.absolutePath,
            sourceFile.absolutePath
        )

        val process = processBuilder.start()
        val exitCode = process.waitFor()
        val errorOutput = process.errorStream.bufferedReader().readText()

        return if (exitCode == 0) {
            CompileResult(true, outputDir.absolutePath, null, null)
        } else {
            CompileResult(false, null, null, errorOutput)
        }
    }

    /**
     * Kotlin 파일 컴파일
     */
    private fun compileKotlin(sourceFile: File, outputDir: File, kotlinCompilerPath: String): CompileResult {
        if (kotlinCompilerPath.isEmpty()) {
            return CompileResult(false, null, null, "Kotlin 컴파일러 경로가 설정되지 않았습니다.")
        }

        val processBuilder = ProcessBuilder(
            kotlinCompilerPath,
            "-d", outputDir.absolutePath,
            sourceFile.absolutePath
        )

        val process = processBuilder.start()
        val exitCode = process.waitFor()
        val errorOutput = process.errorStream.bufferedReader().readText()

        return if (exitCode == 0) {
            CompileResult(true, outputDir.absolutePath, null, null)
        } else {
            CompileResult(false, null, null, errorOutput)
        }
    }

    /**
     * IntelliJ 내부 API를 사용한 컴파일
     */
    private fun compileWithIntelliJ(project: Project, file: VirtualFile): CompletableFuture<CompileResult> {
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
        project: Project,
        outputPath: String,
        className: String,
        input: String,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS
    ): ExecutionResult {
        val startTime = System.currentTimeMillis()

        try {
            val settings = JvmBaekjoonSettings.getInstance()
            val jdkPath = settings.state.jdkPath

            // JDK 경로 결정: 설정 > 프로젝트 SDK > 시스템 java 순서
            val javaCommand = when {
                jdkPath.isNotEmpty() -> Paths.get(jdkPath, "bin", "java").toString()
                else -> {
                    // 프로젝트 SDK 사용
                    val projectSdk = ProjectRootManager.getInstance(project).projectSdk
                    if (projectSdk != null) {
                        val sdkHome = projectSdk.homePath
                        if (sdkHome != null) {
                            Paths.get(sdkHome, "bin", "java").toString()
                        } else {
                            "java"
                        }
                    } else {
                        "java"
                    }
                }
            }

            val processBuilder = ProcessBuilder(javaCommand, "-cp", outputPath, className)
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

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
import com.intellij.openapi.roots.JdkOrderEntry
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.wooongyee.jvmbaekjoon.messages.ErrorMessages
import com.wooongyee.jvmbaekjoon.model.CompileResult
import com.wooongyee.jvmbaekjoon.model.ExecutionResult
import com.wooongyee.jvmbaekjoon.settings.JvmBaekjoonSettings
import org.jetbrains.kotlin.psi.KtFile
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

object CodeExecutorService {

    private const val DEFAULT_TIMEOUT_MS = 10000L
    private const val MAX_OUTPUT_LENGTH = 10000

    /**
     * 파일 컴파일
     */
    fun compile(project: Project, file: VirtualFile): CompletableFuture<CompileResult> {
        val settings = JvmBaekjoonSettings.getInstance()
        val jdkPath = settings.state.jdkPath
        val kotlinCompilerPath = settings.state.kotlinCompilerPath

        if (jdkPath.isNotEmpty() || kotlinCompilerPath.isNotEmpty()) {
            return compileDirectly(project, file, jdkPath, kotlinCompilerPath)
        }

        return compileWithIntelliJ(project, file)
    }

    /**
     * 직접 컴파일러 호출
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
                    val actualClassName = findCompiledClass(tempDir, className, file.nameWithoutExtension)
                    future.complete(CompileResult(true, tempDir.absolutePath, actualClassName ?: className, null))
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
            "-encoding", "UTF-8",
            "-d", outputDir.absolutePath,
            sourceFile.absolutePath
        )

        val process = processBuilder.start()
        val exitCode = process.waitFor()

        val output = process.inputStream.bufferedReader().readText()
        val errorOutput = process.errorStream.bufferedReader().readText()
        val fullOutput = (output + "\n" + errorOutput).trim()

        return if (exitCode == 0) {
            CompileResult(true, outputDir.absolutePath, null, null)
        } else {
            val errorMsg = fullOutput.ifEmpty { ErrorMessages.compileFailed(exitCode) }
            CompileResult(false, null, null, errorMsg)
        }
    }

    /**
     * Kotlin 파일 컴파일
     */
    private fun compileKotlin(sourceFile: File, outputDir: File, kotlinCompilerPath: String): CompileResult {
        if (kotlinCompilerPath.isEmpty()) {
            return CompileResult(false, null, null, ErrorMessages.kotlinCompilerNotSet())
        }

        val processBuilder = ProcessBuilder(
            kotlinCompilerPath,
            "-d", outputDir.absolutePath,
            "-include-runtime",
            sourceFile.absolutePath
        )

        val process = processBuilder.start()
        val exitCode = process.waitFor()

        val output = process.inputStream.bufferedReader().readText()
        val errorOutput = process.errorStream.bufferedReader().readText()
        val fullOutput = (output + "\n" + errorOutput).trim()

        return if (exitCode == 0) {
            CompileResult(true, outputDir.absolutePath, null, null)
        } else {
            val errorMsg = fullOutput.ifEmpty { ErrorMessages.compileFailed(exitCode) }
            CompileResult(false, null, null, errorMsg)
        }
    }

    /**
     * IntelliJ CompilerManager를 사용한 컴파일
     */
    private fun compileWithIntelliJ(project: Project, file: VirtualFile): CompletableFuture<CompileResult> {
        val future = CompletableFuture<CompileResult>()

        ApplicationManager.getApplication().invokeLater {
            val module = ModuleUtil.findModuleForFile(file, project)
            if (module == null) {
                future.complete(CompileResult(false, null, null, ErrorMessages.moduleNotFound()))
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
                        future.complete(CompileResult(false, null, null, ErrorMessages.compileError(errorMessages)))
                        return
                    }

                    val compilerExtension = CompilerModuleExtension.getInstance(module)
                    val outputPath = compilerExtension?.compilerOutputPath?.path

                    if (outputPath == null) {
                        future.complete(CompileResult(false, null, null, ErrorMessages.outputPathNotFound()))
                        return
                    }

                    Thread.sleep(1000)

                    val expectedClassName = extractClassName(project, file)
                    val actualClassName = findCompiledClass(
                        File(outputPath),
                        expectedClassName,
                        file.nameWithoutExtension
                    )

                    if (actualClassName != null) {
                        future.complete(CompileResult(true, outputPath, actualClassName, null))
                    } else {
                        future.complete(CompileResult(false, null, null, ErrorMessages.classFileNotFound(file.name, outputPath)))
                    }
                }
            })
        }

        return future
    }
    /**
     * Java 실행 명령어 경로 결정
     * 우선순위: 설정 > 프로젝트 SDK > 시스템 java
     */
    private fun getJavaCommand(project: Project): String {
        val settings = JvmBaekjoonSettings.getInstance()
        val jdkPath = settings.state.jdkPath

        return when {
            jdkPath.isNotEmpty() -> Paths.get(jdkPath, "bin", "java").toString()
            else -> {
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
    }

    /**
     * Classpath 구성: Kotlin 런타임 JAR + 사용자 코드 출력 경로
     */
    private fun buildClasspath(project: Project, outputPath: String): String {
        val kotlinRuntimeJars = findKotlinRuntimeJars(project)
        val cleanedJars = kotlinRuntimeJars.map { it.removeSuffix("!/") }
        val libraryPath = cleanedJars.joinToString(File.pathSeparator)

        return if (libraryPath.isNotEmpty()) {
            "$libraryPath${File.pathSeparator}$outputPath"
        } else {
            outputPath
        }
    }

    /**
     * 프로세스 실행 및 결과 수집
     */
    private fun runProcess(
        process: Process,
        input: String,
        timeoutMs: Long,
        startTime: Long
    ): ExecutionResult {
        process.outputStream.bufferedWriter().use { writer ->
            writer.write(input)
            writer.flush()
        }

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

        if (output.length > MAX_OUTPUT_LENGTH) {
            output = output.take(MAX_OUTPUT_LENGTH) + "\n... (출력이 너무 길어 잘렸습니다)"
        }

        return ExecutionResult(
            output = output,
            error = error,
            executionTimeMs = executionTime,
            exitCode = process.exitValue(),
            timedOut = false
        )
    }

    /**
     * Kotlin 런타임 JAR 경로 찾기 (kotlin-stdlib, annotations)
     */
    private fun findKotlinRuntimeJars(project: Project): List<String> {
        val paths = mutableSetOf<String>()

        ApplicationManager.getApplication().runReadAction {
            val module = ProjectRootManager.getInstance(project).contentRoots.firstOrNull()?.let { projectRoot ->
                ProjectRootManager.getInstance(project).fileIndex.getModuleForFile(projectRoot)
            }

            if (module == null) {
                return@runReadAction
            }

            val orderEntries = ModuleRootManager.getInstance(module).orderEntries

            for (orderEntry in orderEntries) {
                if (orderEntry is LibraryOrderEntry || orderEntry is JdkOrderEntry) {
                    val libraryFiles = orderEntry.getFiles(OrderRootType.CLASSES)

                    for (file in libraryFiles) {
                        val name = file.name.lowercase()
                        if ((name.startsWith("kotlin-stdlib") || name.startsWith("annotations")) &&
                            file.extension?.lowercase() == "jar") {
                            paths.add(file.path)
                        }
                    }
                }
            }
        }

        return paths.toList()
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
            val javaCommand = getJavaCommand(project)
            val fullClasspath = buildClasspath(project, outputPath)

            val process = ProcessBuilder(javaCommand, "-cp", fullClasspath, className)
                .redirectErrorStream(false)
                .start()

            return runProcess(process, input, timeoutMs, startTime)
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
     * 컴파일된 클래스 파일 찾기
     */
    private fun findCompiledClass(outputDir: File, expectedClassName: String, fileNameWithoutExt: String): String? {
        val packagePath = if (expectedClassName.contains('.')) {
            expectedClassName.substringBeforeLast('.').replace('.', File.separatorChar)
        } else {
            ""
        }

        val baseFileName = fileNameWithoutExt.split(" ", "(", ")").first()

        val searchDir = if (packagePath.isNotEmpty()) {
            File(outputDir, packagePath)
        } else {
            outputDir
        }

        if (!searchDir.exists()) {
            return null
        }

        val classFiles = searchDir.walkTopDown()
            .filter { file ->
                file.extension == "class" &&
                (file.nameWithoutExtension.startsWith(baseFileName) ||
                 file.nameWithoutExtension.contains(baseFileName))
            }
            .toList()

        if (classFiles.isEmpty()) {
            return null
        }

        val mainClassFile = classFiles.firstOrNull {
            it.nameWithoutExtension.endsWith("Kt")
        } ?: classFiles.firstOrNull {
            it.nameWithoutExtension == fileNameWithoutExt
        } ?: classFiles.first()

        val relativePath = mainClassFile.relativeTo(outputDir).path
        val className = relativePath
            .replace(File.separator, ".")
            .removeSuffix(".class")

        return className
    }

    /**
     * 클래스 이름 추출 (Full Qualified Name)
     */
    private fun extractClassName(project: Project, file: VirtualFile): String {
        val psiFile = PsiManager.getInstance(project).findFile(file) ?: return file.nameWithoutExtension

        return ApplicationManager.getApplication().runReadAction<String> {
            val fileName = file.nameWithoutExtension

            when (file.extension) {
                "java" -> {
                    if (psiFile is PsiJavaFile) {
                        val packageName = psiFile.packageName
                        if (packageName.isNotEmpty()) {
                            "$packageName.$fileName"
                        } else {
                            fileName
                        }
                    } else {
                        fileName
                    }
                }

                "kt" -> {
                    if (psiFile is KtFile) {
                        val packageName = psiFile.packageFqName.asString()
                        val className = "${fileName}Kt"
                        if (packageName.isNotEmpty()) {
                            "$packageName.$className"
                        } else {
                            className
                        }
                    } else {
                        "${fileName}Kt"
                    }
                }

                else -> fileName
            }
        }
    }

    /**
     * 지원되는 파일인지 확인
     */
    fun isSupportedFile(file: VirtualFile?): Boolean {
        return file?.extension in listOf("kt", "java")
    }
}

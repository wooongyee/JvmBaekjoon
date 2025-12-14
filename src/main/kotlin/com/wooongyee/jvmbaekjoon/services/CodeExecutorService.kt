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
import com.intellij.openapi.roots.OrderRootType // ⭐️ 새로 추가된 Import
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
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
                    // 실제 생성된 클래스 파일 확인 (디버깅용)
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
            "-encoding", "UTF-8",  // UTF-8 인코딩 명시
            "-d", outputDir.absolutePath,
            sourceFile.absolutePath
        )

        val process = processBuilder.start()
        val exitCode = process.waitFor()

        // 표준 출력과 에러 출력 모두 수집
        val output = process.inputStream.bufferedReader().readText()
        val errorOutput = process.errorStream.bufferedReader().readText()
        val fullOutput = (output + "\n" + errorOutput).trim()

        return if (exitCode == 0) {
            CompileResult(true, outputDir.absolutePath, null, null)
        } else {
            val errorMsg = if (fullOutput.isNotEmpty()) {
                fullOutput
            } else {
                "컴파일 실패 (종료 코드: $exitCode)"
            }
            CompileResult(false, null, null, errorMsg)
        }
    }

    /**
     * Kotlin 파일 컴파일
     */
    private fun compileKotlin(sourceFile: File, outputDir: File, kotlinCompilerPath: String): CompileResult {
        if (kotlinCompilerPath.isEmpty()) {
            val errorMsg = """
                ❌ Kotlin 컴파일러 경로가 설정되지 않았습니다

                🔧 해결 방법:
                1️⃣ 터미널에서 'which kotlinc' 실행하여 kotlinc 경로 확인
                2️⃣ IntelliJ: Preferences (Settings) → Tools → JvmBaekjoon
                3️⃣ 'Kotlin Compiler Path'에 위에서 확인한 경로 입력
                   예시: /usr/local/bin/kotlinc 또는 /opt/homebrew/bin/kotlinc

                💡 kotlinc가 설치되어 있지 않다면:
                • Homebrew 사용: brew install kotlin
                • 또는 https://kotlinlang.org/docs/command-line.html 참고
            """.trimIndent()
            return CompileResult(false, null, null, errorMsg)
        }

        val processBuilder = ProcessBuilder(
            kotlinCompilerPath,
            "-d", outputDir.absolutePath,
            "-include-runtime",  // Kotlin 런타임 포함
            sourceFile.absolutePath
        )

        val process = processBuilder.start()
        val exitCode = process.waitFor()

        // 표준 출력과 에러 출력 모두 수집
        val output = process.inputStream.bufferedReader().readText()
        val errorOutput = process.errorStream.bufferedReader().readText()
        val fullOutput = (output + "\n" + errorOutput).trim()

        return if (exitCode == 0) {
            CompileResult(true, outputDir.absolutePath, null, null)
        } else {
            val errorMsg = if (fullOutput.isNotEmpty()) {
                fullOutput
            } else {
                "컴파일 실패 (종료 코드: $exitCode)"
            }
            CompileResult(false, null, null, errorMsg)
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
                val errorMsg = """
                    ❌ 컴파일 실패

                    이 파일이 어떤 IntelliJ 모듈에도 속하지 않습니다.

                    🔧 해결 방법:
                    1️⃣ 파일이 있는 폴더를 우클릭 → 'Mark Directory as' → 'Sources Root' 선택
                    2️⃣ 또는 JvmBaekjoon 설정에서 'JDK Path'와 'Kotlin Compiler Path'를 지정하여 직접 컴파일 방식을 사용하세요
                """.trimIndent()
                future.complete(CompileResult(false, null, null, errorMsg))
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
                        future.complete(CompileResult(false, null, null, "❌ 컴파일 에러:\n\n$errorMessages"))
                        return
                    }

                    // 컴파일 성공 - 출력 경로 찾기
                    val compilerExtension = CompilerModuleExtension.getInstance(module)
                    val outputPath = compilerExtension?.compilerOutputPath?.path

                    if (outputPath == null) {
                        val errorMsg = """
                            ❌ 컴파일 실패

                            IntelliJ 프로젝트의 출력 경로(Output Path)를 찾을 수 없습니다.

                            🔧 해결 방법:
                            1️⃣ File → Project Structure → Project → Project compiler output 경로 확인
                            2️⃣ JvmBaekjoon 설정에서 'JDK Path'와 'Kotlin Compiler Path'를 지정 (권장)
                        """.trimIndent()
                        future.complete(CompileResult(false, null, null, errorMsg))
                        return
                    }

                    // 1초 대기 후 파일 존재 여부 확인 (파일 시스템 쓰기 지연 방지)
                    Thread.sleep(1000)

                    // PSI를 통해 예상 클래스명을 추출
                    val expectedClassName = extractClassName(project, file)

                    // 실제 생성된 클래스 파일을 찾기 시도
                    val actualClassName = findCompiledClass(
                        File(outputPath),
                        expectedClassName,
                        file.nameWithoutExtension
                    )

                    if (actualClassName != null) {
                        future.complete(CompileResult(true, outputPath, actualClassName, null))
                    } else {
                        // 실제로 클래스 파일을 찾지 못한 경우
                        val errorMsg = """
                            ❌ 컴파일 실패

                            IntelliJ 기본 컴파일러를 사용했으나, 컴파일된 클래스 파일을 찾을 수 없습니다.

                            📋 현재 상황:
                            • 파일: ${file.name}
                            • 출력 경로: $outputPath
                            • IntelliJ가 이 파일을 제대로 컴파일하지 못했거나, 예상하지 못한 위치에 저장했습니다.

                            🔧 해결 방법 (순서대로 시도해보세요):

                            1️⃣ Source Root 설정 확인
                               • IntelliJ에서 파일이 있는 폴더를 우클릭
                               • 'Mark Directory as' → 'Sources Root' 선택
                               • 'Build' → 'Rebuild Project' 실행 후 다시 테스트

                            2️⃣ 직접 컴파일 방식 사용 (⭐ 가장 확실한 방법)
                               • IntelliJ 상단 메뉴: Preferences (Settings) → Tools → JvmBaekjoon
                               • 'JDK Path' 입력 예시:
                                 /Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
                               • 'Kotlin Compiler Path' 입력:
                                 터미널에서 'which kotlinc' 실행 후 나온 경로 입력
                               • 설정 후 다시 테스트 실행

                            💡 왜 이런 문제가 발생하나요?
                            • IntelliJ의 기본 컴파일러는 프로젝트 구조에 민감합니다
                            • 파일명에 공백, 특수문자, 한글이 있으면 문제가 생길 수 있습니다
                            • 파일이 Source Root 밖에 있으면 컴파일되지 않습니다
                            • 직접 컴파일 방식(2번)을 사용하면 이런 문제를 피할 수 있습니다

                            ⚠️ 위 방법으로도 해결되지 않는다면:
                            • 파일명을 영문으로 단순화해보세요 (예: Main.kt, Solution.kt)
                            • 또는 현재 프로젝트 구조가 플러그인과 호환되지 않을 수 있습니다
                        """.trimIndent()
                        future.complete(CompileResult(false, null, null, errorMsg))
                    }
                }
            })
        }

        return future
    }
    /**
     * 프로젝트 모듈 설정에서 모든 필수 Kotlin 런타임 JAR 경로를 찾습니다.
     *
     * 이 함수는 모듈 및 프로젝트 파일 인덱스를 읽기 때문에, IntelliJ Platform의 쓰레딩 규칙에 따라
     * 모든 모델 접근을 Read Action 내부에서 수행하도록 수정되었습니다.
     *
     * @return kotlin-stdlib 및 annotations 관련 JAR 파일 경로 리스트
     */
    private fun findKotlinRuntimeJars(project: Project): List<String> {
        val paths = mutableSetOf<String>()

        // 모듈을 찾고, OrderEntry를 순회하는 모든 로직을 Read Action 내부에 둡니다.
        // Read Action은 IntelliJ 모델의 일관된 읽기를 보장합니다.
        ApplicationManager.getApplication().runReadAction {

            // 1. 현재 프로젝트 루트에 해당하는 모듈을 찾습니다.
            val module = ProjectRootManager.getInstance(project).contentRoots.firstOrNull()?.let { projectRoot ->
                // projectRoot를 기반으로 모듈을 찾습니다. (모델 읽기)
                ProjectRootManager.getInstance(project).fileIndex.getModuleForFile(projectRoot)
            }

            if (module == null) {
                // 모듈을 찾지 못하면 Read Action을 종료하고 함수를 빠져나갑니다.
                return@runReadAction
            }

            // 2. 모듈의 모든 종속성(OrderEntry) 확인
            val orderEntries = ModuleRootManager.getInstance(module).orderEntries // (모델 읽기)

            for (orderEntry in orderEntries) {
                // 3. 라이브러리 또는 SDK 종속성인 경우에만 확인
                if (orderEntry is LibraryOrderEntry || orderEntry is JdkOrderEntry) {
                    // Classpath에 포함되는 모든 라이브러리 파일 경로를 가져옴 (모델 읽기)
                    val libraryFiles = orderEntry.getFiles(OrderRootType.CLASSES)

                    for (file in libraryFiles) {
                        val name = file.name.lowercase()

                        // kotlin-stdlib 또는 annotations으로 시작하고 .jar로 끝나는 파일만 포함
                        if (name.startsWith("kotlin-stdlib") || name.startsWith("annotations")) {
                            // .jar 파일만 Classpath에 추가 (안정적인 확장자 검사 유지)
                            if (file.extension?.lowercase() == "jar") {
                                // Classpath에 포함될 경로를 저장
                                paths.add(file.path)
                            }
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

            // ⭐️ 수정 부분 1: Kotlin 런타임 JAR 경로를 찾습니다. ⭐️
            // (이 로직은 findKotlinRuntimeJars 함수가 ModuleRootManager 등을 사용하여 경로를 찾도록 수정되어 있다고 가정합니다.)
            val kotlinRuntimeJars = findKotlinRuntimeJars(project)

            // ⭐️ 수정 부분 2: Classpath 구성: (모든 JAR 파일 경로) + (구분자) + (사용자 코드 출력 경로) ⭐️

            // 1. IntelliJ VFS 경로 포맷(!/) 제거 및 경로 정리
            val cleanedJars = kotlinRuntimeJars.map {
                it.removeSuffix("!/")
            }

            // 2. Classpath 문자열 조합
            val libraryPath = cleanedJars.joinToString(File.pathSeparator)

            val fullClasspath = if (libraryPath.isNotEmpty()) {
                // 예: /path/to/stdlib.jar:/path/to/annotations.jar:/path/to/output
                "$libraryPath${File.pathSeparator}$outputPath"
            } else {
                // 라이브러리를 찾지 못하면 출력 경로만 사용 (NoClassDefFoundError 발생 위험)
                outputPath
            }

            // ⭐️ [DEBUG] 실행 경로 진단 정보 추가 ⭐️
            // className을 Classpath와 결합하여 JVM이 찾을 것으로 예상되는 전체 경로를 출력합니다.
            val expectedClassFileRelativePath = className.replace('.', File.separatorChar) + ".class"
            val expectedClassFilePath = Paths.get(outputPath, expectedClassFileRelativePath)

            println("🚀 [DEBUG] Classpath: $fullClasspath") // fullClasspath 출력
            println("🚀 [DEBUG] FQCN: $className")
            println("🚀 [DEBUG] Expected Class Path: $expectedClassFilePath")

            // ⭐️ 수정 부분 3: ProcessBuilder에 fullClasspath 적용 ⭐️
            val processBuilder = ProcessBuilder(javaCommand, "-cp", fullClasspath, className)
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
     * 컴파일된 디렉토리에서 실제 생성된 클래스 파일 찾기
     *
     * Kotlin 컴파일러는 파일명의 공백/특수문자를 처리할 때:
     * - "BJ_1541_잃어버린 괄호.kt" → "BJ_1541_잃어버린Kt.class" (공백 이후 무시)
     * - 또는 완전히 다른 규칙을 적용할 수 있음
     */
    private fun findCompiledClass(outputDir: File, expectedClassName: String, fileNameWithoutExt: String): String? {
        // 패키지명 추출 (있는 경우)
        val packagePath = if (expectedClassName.contains('.')) {
            expectedClassName.substringBeforeLast('.').replace('.', File.separatorChar)
        } else {
            ""
        }

        // 파일명의 첫 부분 추출 (공백이나 특수문자 전까지)
        // 예: "BJ_1541_잃어버린 괄호" → "BJ_1541_잃어버린"
        val baseFileName = fileNameWithoutExt.split(" ", "(", ")").first()

        // 검색할 디렉토리 결정
        val searchDir = if (packagePath.isNotEmpty()) {
            File(outputDir, packagePath)
        } else {
            outputDir
        }

        if (!searchDir.exists()) {
            return null
        }

        // .class 파일들을 찾기 (재귀적으로)
        val classFiles = searchDir.walkTopDown()
            .filter { file ->
                file.extension == "class" &&
                (file.nameWithoutExtension.startsWith(baseFileName) ||  // 파일명 시작 부분 일치
                 file.nameWithoutExtension.contains(baseFileName))       // 또는 포함
            }
            .toList()

        if (classFiles.isEmpty()) {
            return null
        }

        // 가장 적합한 클래스 파일 선택
        val mainClassFile = classFiles.firstOrNull {
            // 1순위: *Kt 형태 (Kotlin top-level main)
            it.nameWithoutExtension.endsWith("Kt")
        } ?: classFiles.firstOrNull {
            // 2순위: 정확한 파일명 일치
            it.nameWithoutExtension == fileNameWithoutExt
        } ?: classFiles.first() // 3순위: 첫 번째 파일

        // 상대 경로를 통해 Full Qualified Name 생성
        val relativePath = mainClassFile.relativeTo(outputDir).path
        val className = relativePath
            .replace(File.separator, ".")
            .removeSuffix(".class")

        return className
    }

    /**
     * 파일에서 클래스 이름 추출 (패키지명 포함 Full Qualified Name)
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
                    // Kotlin: package.FileNameKt 형태 (top-level main 함수인 경우)
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

package com.wooongyee.jvmbaekjoon.messages

/**
 * 플러그인에서 사용하는 에러 메시지 모음
 */
object ErrorMessages {

    /**
     * Kotlin 컴파일러 경로가 설정되지 않았을 때
     */
    fun kotlinCompilerNotSet(): String = """
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

    /**
     * IntelliJ에서 모듈을 찾을 수 없을 때
     */
    fun moduleNotFound(): String = """
        ❌ 컴파일 실패

        이 파일이 어떤 IntelliJ 모듈에도 속하지 않습니다.

        🔧 해결 방법:
        1️⃣ 파일이 있는 폴더를 우클릭 → 'Mark Directory as' → 'Sources Root' 선택
        2️⃣ 또는 JvmBaekjoon 설정에서 'JDK Path'와 'Kotlin Compiler Path'를 지정하여 직접 컴파일 방식을 사용하세요
    """.trimIndent()

    /**
     * IntelliJ에서 출력 경로를 찾을 수 없을 때
     */
    fun outputPathNotFound(): String = """
        ❌ 컴파일 실패

        IntelliJ 프로젝트의 출력 경로(Output Path)를 찾을 수 없습니다.

        🔧 해결 방법:
        1️⃣ File → Project Structure → Project → Project compiler output 경로 확인
        2️⃣ JvmBaekjoon 설정에서 'JDK Path'와 'Kotlin Compiler Path'를 지정 (권장)
    """.trimIndent()

    /**
     * 컴파일은 성공했으나 클래스 파일을 찾을 수 없을 때
     */
    fun classFileNotFound(fileName: String, outputPath: String): String = """
        ❌ 컴파일 실패

        IntelliJ 기본 컴파일러를 사용했으나, 컴파일된 클래스 파일을 찾을 수 없습니다.

        📋 현재 상황:
        • 파일: $fileName
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

    /**
     * 컴파일 에러 메시지 포맷
     */
    fun compileError(errorMessages: String): String =
        "❌ 컴파일 에러:\n\n$errorMessages"

    /**
     * 컴파일 실패 (종료 코드)
     */
    fun compileFailed(exitCode: Int): String =
        "컴파일 실패 (종료 코드: $exitCode)"
}

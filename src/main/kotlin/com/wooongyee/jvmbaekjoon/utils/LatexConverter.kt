package com.wooongyee.jvmbaekjoon.utils

/**
 * LaTeX 수식을 유니코드로 변환하는 유틸리티
 */
object LatexConverter {

    /**
     * LaTeX 기호를 유니코드로 변환하는 맵
     */
    private val latexSymbols = mapOf(
        // 비교 연산자
        "\\le" to "≤",
        "\\leq" to "≤",
        "\\ge" to "≥",
        "\\geq" to "≥",
        "\\neq" to "≠",
        "\\ne" to "≠",
        "\\approx" to "≈",

        // 수학 연산자
        "\\times" to "×",
        "\\div" to "÷",
        "\\pm" to "±",
        "\\mp" to "∓",

        // 집합 기호
        "\\in" to "∈",
        "\\notin" to "∉",
        "\\subset" to "⊂",
        "\\supset" to "⊃",
        "\\subseteq" to "⊆",
        "\\supseteq" to "⊇",
        "\\cup" to "∪",
        "\\cap" to "∩",
        "\\emptyset" to "∅",

        // 논리 기호
        "\\land" to "∧",
        "\\lor" to "∨",
        "\\neg" to "¬",
        "\\forall" to "∀",
        "\\exists" to "∃",

        // 화살표
        "\\rightarrow" to "→",
        "\\leftarrow" to "←",
        "\\leftrightarrow" to "↔",
        "\\Rightarrow" to "⇒",
        "\\Leftarrow" to "⇐",
        "\\Leftrightarrow" to "⇔",

        // 그리스 문자
        "\\alpha" to "α",
        "\\beta" to "β",
        "\\gamma" to "γ",
        "\\delta" to "δ",
        "\\epsilon" to "ε",
        "\\theta" to "θ",
        "\\lambda" to "λ",
        "\\mu" to "μ",
        "\\pi" to "π",
        "\\sigma" to "σ",
        "\\omega" to "ω",

        // 기타
        "\\infty" to "∞",
        "\\cdot" to "·",
        "\\dots" to "…",
        "\\ldots" to "…",
        "\\cdots" to "⋯"
    )

    /**
     * 텍스트에서 LaTeX 수식 ($...$)을 찾아서 변환
     */
    fun convertLatex(text: String): String {
        var result = text

        // $...$ 패턴 찾기
        val pattern = Regex("""\$([^\$]+)\$""")

        result = pattern.replace(result) { matchResult ->
            val latexContent = matchResult.groupValues[1]
            convertLatexExpression(latexContent)
        }

        return result
    }

    /**
     * 단일 LaTeX 표현식 변환
     */
    private fun convertLatexExpression(latex: String): String {
        var result = latex.trim()

        // 1. 먼저 복잡한 명령 처리 (중괄호 포함)
        // 분수 처리: \frac{a}{b} → a/b
        result = result.replace(Regex("""\\frac\{([^}]+)\}\{([^}]+)\}"""), "$1/$2")

        // 제곱근 처리: \sqrt{x} → √x
        result = result.replace(Regex("""\\sqrt\{([^}]+)\}"""), "√$1")

        // 2. 제곱/첨자 처리 (중괄호 있는 경우)
        result = convertSuperscript(result)
        result = convertSubscript(result)

        // 3. LaTeX 심볼을 유니코드로 치환
        latexSymbols.forEach { (latexCmd, unicode) ->
            result = result.replace(latexCmd, unicode)
        }

        // 4. 남은 중괄호 제거 (예: {n} → n)
        result = result.replace(Regex("""\{([^}]+)\}"""), "$1")

        // 5. 백슬래시 제거
        result = result.replace("\\", "")

        // 6. 쉼표 처리 (\, → 제거)
        result = result.replace(",", ",")

        // 7. 공백 정리
        result = result.replace(Regex("""\s+"""), " ").trim()

        return result
    }

    /**
     * 제곱 표시 변환 (^2 → ²)
     */
    private fun convertSuperscript(text: String): String {
        val superscripts = mapOf(
            '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
            '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
            '+' to '⁺', '-' to '⁻', '=' to '⁼', '(' to '⁽', ')' to '⁾',
            'n' to 'ⁿ'
        )

        var result = text

        // ^{...} 패턴 (우선 처리)
        result = result.replace(Regex("""\^\{([^}]+)\}""")) { match ->
            val content = match.groupValues[1]
            content.map { char -> superscripts[char] ?: char }.joinToString("")
        }

        // ^x 패턴 (단일 문자)
        result = result.replace(Regex("""\^([0-9n])""")) { match ->
            val char = match.groupValues[1][0]
            superscripts[char]?.toString() ?: match.value
        }

        return result
    }

    /**
     * 아래 첨자 변환 (_2 → ₂)
     */
    private fun convertSubscript(text: String): String {
        val subscripts = mapOf(
            '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄',
            '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
            '+' to '₊', '-' to '₋', '=' to '₌', '(' to '₍', ')' to '₎'
        )

        var result = text

        // _{...} 패턴 (우선 처리)
        result = result.replace(Regex("""_\{([^}]+)\}""")) { match ->
            val content = match.groupValues[1]
            content.map { char -> subscripts[char] ?: char }.joinToString("")
        }

        // _x 패턴 (단일 문자)
        result = result.replace(Regex("""_([0-9])""")) { match ->
            val char = match.groupValues[1][0]
            subscripts[char]?.toString() ?: match.value
        }

        return result
    }
}

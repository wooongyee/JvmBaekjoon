package com.wooongyee.jvmbaekjoon.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


class
LatexConverterTest {

    @Test
    fun `LaTeX 변수 변환 테스트`() {
        val input = "첫째 줄에 월드 나라의 도시의 개수 \$n\$ (\$1 \\le n \\le 10\\,000\$)이 주어진다."
        val expected = "첫째 줄에 월드 나라의 도시의 개수 n (1 ≤ n ≤ 10,000)이 주어진다."

        val result = LatexConverter.convertLatex(input)

        assertEquals(expected, result)
    }

    @Test
    fun `복잡한 LaTeX 수식 변환 테스트`() {
        val input = """
            첫째 줄에 월드 나라의 도시의 개수 ${'$'}n${'$'} (${'$'}1 \le n \le 10\,000${'$'})이 주어지고
            둘째 줄에는 월드 나라의 도로의 개수 ${'$'}m${'$'} (${'$'}1 \le m \le 100\,000${'$'})이 주어진다.
            그리고 셋째 줄부터 ${'$'}m+2${'$'}번째 줄까지 도로의 정보를 나타내는 세 정수 ${'$'}u, v, w${'$'}가 공백으로 구분되어 주어진다.
        """.trimIndent()

        val result = LatexConverter.convertLatex(input)

        println("입력:")
        println(input)
        println("\n변환 결과:")
        println(result)

        // n, m 등 변수가 제대로 변환되었는지
        assert(result.contains("n (1 ≤ n ≤ 10,000)"))
        assert(result.contains("m (1 ≤ m ≤ 100,000)"))
        assert(result.contains("m+2"))
        assert(result.contains("u, v, w"))
    }

    @Test
    fun `제곱 표현 변환 테스트`() {
        val input = "${'$'}n^2${'$'} + ${'$'}m^3${'$'} = ${'$'}10^{10}${'$'}"
        val result = LatexConverter.convertLatex(input)

        println("입력: $input")
        println("결과: $result")

        assert(result.contains("n²"))
        assert(result.contains("m³"))
        assert(result.contains("10¹⁰"))
    }

    @Test
    fun `분수 표현 변환 테스트`() {
        val input = "확률은 ${'$'}\\frac{1}{2}${'$'}이다."
        val result = LatexConverter.convertLatex(input)

        println("입력: $input")
        println("결과: $result")

        assert(result.contains("1/2"))
    }

    @Test
    fun `제곱근 표현 변환 테스트`() {
        val input = "${'$'}\\sqrt{2}${'$'}는 무리수이다."
        val result = LatexConverter.convertLatex(input)

        println("입력: $input")
        println("결과: $result")

        assert(result.contains("√2"))
    }

    @Test
    fun `다양한 비교 연산자 테스트`() {
        val input = "${'$'}a \\le b${'$'}, ${'$'}c \\ge d${'$'}, ${'$'}e \\neq f${'$'}"
        val result = LatexConverter.convertLatex(input)

        println("입력: $input")
        println("결과: $result")

        assert(result.contains("a ≤ b"))
        assert(result.contains("c ≥ d"))
        assert(result.contains("e ≠ f"))
    }
}

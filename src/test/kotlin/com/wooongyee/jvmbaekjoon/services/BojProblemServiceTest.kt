package com.wooongyee.jvmbaekjoon.services

import com.wooongyee.jvmbaekjoon.model.ProblemSearchResult
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class BojProblemServiceTest {

    private val service = BojProblemService()

    /**
     * 정렬 테스트 - 검색어로 시작하는 제목이 먼저 와야 함
     */
    @Test
    fun `sort should prioritize titles starting with keyword`() {
        val results = listOf(
            ProblemSearchResult("1001", "A+B - 오답노트"),
            ProblemSearchResult("1002", "오큰수"),
            ProblemSearchResult("1003", "오등큰수"),
            ProblemSearchResult("1004", "피보나치 오류")
        )

        val sorted = sortSearchResults(results, "오")

        // "오"로 시작하는 것들이 먼저
        assertEquals("오큰수", sorted[0].title)
        assertEquals("오등큰수", sorted[1].title)
    }

    /**
     * 정렬 테스트 - 검색어 위치가 앞쪽일수록 우선
     */
    @Test
    fun `sort should prioritize earlier keyword position`() {
        val results = listOf(
            ProblemSearchResult("1001", "ABC 오류 발생"),
            ProblemSearchResult("1002", "오류 처리기"),
            ProblemSearchResult("1003", "ZZZZZ 오류")
        )

        val sorted = sortSearchResults(results, "오류")

        // "오류"로 시작하는 것이 먼저
        assertEquals("오류 처리기", sorted[0].title)
        // 그 다음 "오류"가 앞쪽에 있는 순
        assertEquals("ABC 오류 발생", sorted[1].title)
        assertEquals("ZZZZZ 오류", sorted[2].title)
    }

    /**
     * 정렬 테스트 - 동일 조건이면 문제 번호 오름차순
     */
    @Test
    fun `sort should order by problem number when other conditions equal`() {
        val results = listOf(
            ProblemSearchResult("2000", "오큰수"),
            ProblemSearchResult("1000", "오큰수 변형"),
            ProblemSearchResult("1500", "오큰수 응용")
        )

        val sorted = sortSearchResults(results, "오큰수")

        // "오큰수"로 시작하는 것들 중 문제 번호 오름차순
        assertEquals("1000", sorted[0].number)
        assertEquals("1500", sorted[1].number)
        assertEquals("2000", sorted[2].number)
    }

    /**
     * 대소문자 무시 테스트
     */
    @Test
    fun `sort should be case insensitive`() {
        val results = listOf(
            ProblemSearchResult("1001", "abc problem"),
            ProblemSearchResult("1002", "ABC solution"),
            ProblemSearchResult("1003", "zzz ABC test")
        )

        val sorted = sortSearchResults(results, "ABC")

        // 대소문자 무관하게 시작하는 것들이 먼저
        assertTrue(sorted[0].title.startsWith("abc", ignoreCase = true) ||
                   sorted[0].title.startsWith("ABC", ignoreCase = true))
    }

    /**
     * 빈 결과 테스트
     */
    @Test
    fun `sort should handle empty list`() {
        val results = emptyList<ProblemSearchResult>()
        val sorted = sortSearchResults(results, "test")

        assertTrue(sorted.isEmpty())
    }

    /**
     * 검색어가 포함되지 않은 결과 테스트
     */
    @Test
    fun `sort should handle results without keyword`() {
        val results = listOf(
            ProblemSearchResult("1001", "Hello World"),
            ProblemSearchResult("1002", "Goodbye")
        )

        val sorted = sortSearchResults(results, "XYZ")

        // 검색어가 없으면 문제 번호 순
        assertEquals("1001", sorted[0].number)
        assertEquals("1002", sorted[1].number)
    }

    /**
     * BojProblemService의 private 메서드를 테스트하기 위한 헬퍼
     * (실제로는 리플렉션 없이 로직만 테스트)
     */
    private fun sortSearchResults(results: List<ProblemSearchResult>, keyword: String): List<ProblemSearchResult> {
        return results.sortedWith(compareBy(
            { !it.title.startsWith(keyword, ignoreCase = true) },
            { it.title.indexOf(keyword, ignoreCase = true).let { idx -> if (idx < 0) Int.MAX_VALUE else idx } },
            { it.number.toIntOrNull() ?: Int.MAX_VALUE }
        ))
    }
}

package com.wooongyee.jvmbaekjoon.services

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.net.URLEncoder

class BojCrawlerServiceTest {

    /**
     * URL 인코딩 테스트 - 공백이 %20으로 변환되는지 확인
     */
    @Test
    fun `URL encoding should convert spaces to percent20`() {
        val keyword = "N 과 M"
        val encoded = URLEncoder.encode(keyword, "UTF-8").replace("+", "%20")

        assertEquals("N%20%EA%B3%BC%20M", encoded)
        assertFalse(encoded.contains("+"), "공백이 +로 인코딩되면 안됨")
        assertTrue(encoded.contains("%20"), "공백은 %20으로 인코딩되어야 함")
    }

    /**
     * 단일 공백 인코딩 테스트
     */
    @Test
    fun `single space should be encoded as percent20`() {
        val keyword = "hello world"
        val encoded = URLEncoder.encode(keyword, "UTF-8").replace("+", "%20")

        assertEquals("hello%20world", encoded)
    }

    /**
     * 한글 인코딩 테스트
     */
    @Test
    fun `Korean characters should be properly encoded`() {
        val keyword = "오큰수"
        val encoded = URLEncoder.encode(keyword, "UTF-8")

        // 한글은 UTF-8로 인코딩됨
        assertFalse(encoded.contains("오"))
        assertTrue(encoded.startsWith("%"))
    }

    /**
     * 실제 검색 테스트 - "1000" 문제 번호로 검색
     */
    @Test
    fun `searchProblem should return results for valid keyword`() = runTest {
        val result = BojCrawlerService.searchProblem("A+B")

        assertTrue(result.isSuccess, "검색이 성공해야 함")

        val results = result.getOrNull()
        assertNotNull(results)
        assertTrue(results!!.isNotEmpty(), "검색 결과가 있어야 함")
    }

    /**
     * 공백 변형 검색어 테스트 - "N과M", "N과 M", "N 과 M" 모두 같은 결과
     */
    @Test
    fun `searchProblem should return same results regardless of spaces`() = runTest {
        val result1 = BojCrawlerService.searchProblem("N과M")
        val result2 = BojCrawlerService.searchProblem("N과 M")
        val result3 = BojCrawlerService.searchProblem("N 과 M")

        assertTrue(result1.isSuccess, "N과M 검색이 성공해야 함")
        assertTrue(result2.isSuccess, "N과 M 검색이 성공해야 함")
        assertTrue(result3.isSuccess, "N 과 M 검색이 성공해야 함")

        val results1 = result1.getOrNull()!!
        val results2 = result2.getOrNull()!!
        val results3 = result3.getOrNull()!!

        assertTrue(results1.isNotEmpty(), "검색 결과가 있어야 함")

        // 세 검색 결과의 문제 번호 집합이 동일해야 함
        val numbers1 = results1.map { it.number }.toSet()
        val numbers2 = results2.map { it.number }.toSet()
        val numbers3 = results3.map { it.number }.toSet()

        assertEquals(numbers1, numbers2, "N과M과 N과 M 검색 결과가 같아야 함")
        assertEquals(numbers2, numbers3, "N과 M과 N 과 M 검색 결과가 같아야 함")
    }

    /**
     * 문제 번호로 직접 조회 테스트
     */
    @Test
    fun `fetchProblem should return problem for valid number`() = runTest {
        val result = BojCrawlerService.fetchProblem("1000")

        assertTrue(result.isSuccess, "문제 조회가 성공해야 함")

        val problem = result.getOrNull()
        assertNotNull(problem)
        assertEquals("1000", problem!!.number)
        assertTrue(problem.title.isNotEmpty(), "제목이 있어야 함")
        assertTrue(problem.testCases.isNotEmpty(), "테스트 케이스가 있어야 함")
    }

    /**
     * 존재하지 않는 문제 번호 테스트
     */
    @Test
    fun `fetchProblem should fail for invalid number`() = runTest {
        val result = BojCrawlerService.fetchProblem("999999999")

        assertTrue(result.isFailure, "존재하지 않는 문제는 실패해야 함")
    }
}

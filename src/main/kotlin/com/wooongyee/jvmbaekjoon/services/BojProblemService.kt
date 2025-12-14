package com.wooongyee.jvmbaekjoon.services

import com.wooongyee.jvmbaekjoon.model.BojProblem
import com.wooongyee.jvmbaekjoon.model.ProblemSearchResult

/**
 * 문제 로드/검색 비즈니스 로직
 */
class BojProblemService {

    companion object {
        private const val MAX_RESULTS = 50
        private const val CACHE_SIZE = 10
    }

    // LRU 캐시 (최대 10개 문제)
    private val problemCache = object : LinkedHashMap<String, BojProblem>(CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, BojProblem>?): Boolean {
            return size > CACHE_SIZE
        }
    }

    /**
     * 입력값에 따라 문제 번호로 직접 로드하거나 제목으로 검색
     */
    suspend fun loadOrSearch(input: String): SearchResult {
        val trimmed = input.trim()

        // 숫자만 입력된 경우 → 문제 번호로 직접 로드
        if (trimmed.all { it.isDigit() }) {
            return loadByNumber(trimmed)
        }

        // 그 외 → 제목으로 검색
        return searchByTitle(trimmed)
    }

    /**
     * 문제 번호로 직접 로드
     */
    private suspend fun loadByNumber(number: String): SearchResult {
        // 캐시 확인
        problemCache[number]?.let {
            return SearchResult.SingleProblem(it)
        }

        // 캐시에 없으면 크롤링
        val result = BojCrawlerService.fetchProblem(number)

        return result.fold(
            onSuccess = { problem ->
                // 캐시에 저장
                problemCache[number] = problem
                SearchResult.SingleProblem(problem)
            },
            onFailure = { SearchResult.NotFound("문제 $number 를 찾을 수 없습니다") }
        )
    }

    /**
     * 제목으로 검색
     */
    private suspend fun searchByTitle(keyword: String): SearchResult {
        val result = BojCrawlerService.searchProblem(keyword)

        return result.fold(
            onSuccess = { results ->
                when {
                    results.isEmpty() -> SearchResult.NotFound("'$keyword' 검색 결과가 없습니다")
                    results.size == 1 -> {
                        // 결과가 1개면 바로 문제 로드 (캐시 활용)
                        loadByNumber(results[0].number)
                    }
                    else -> {
                        // 정렬 후 최대 50개 제한
                        val sortedResults = sortSearchResults(results, keyword).take(MAX_RESULTS)
                        SearchResult.MultipleResults(sortedResults)
                    }
                }
            },
            onFailure = { SearchResult.Error(it.message ?: "검색 중 오류 발생") }
        )
    }

    /**
     * 검색 결과 정렬 (Prefix Match 우선)
     */
    private fun sortSearchResults(results: List<ProblemSearchResult>, keyword: String): List<ProblemSearchResult> {
        return results.sortedWith(compareBy(
            // 1. 제목이 검색어로 시작하는지 (우선)
            { !it.title.startsWith(keyword, ignoreCase = true) },
            // 2. 제목에 검색어가 포함된 위치 (앞쪽일수록 우선)
            { it.title.indexOf(keyword, ignoreCase = true).let { idx -> if (idx < 0) Int.MAX_VALUE else idx } },
            // 3. 문제 번호 (오름차순)
            { it.number.toIntOrNull() ?: Int.MAX_VALUE }
        ))
    }

    /**
     * 검색 결과
     */
    sealed class SearchResult {
        data class SingleProblem(val problem: BojProblem) : SearchResult()
        data class MultipleResults(val results: List<ProblemSearchResult>) : SearchResult()
        data class NotFound(val message: String) : SearchResult()
        data class Error(val message: String) : SearchResult()
    }
}

package com.wooongyee.jvmbaekjoon.services

import com.wooongyee.jvmbaekjoon.model.BojProblem
import com.wooongyee.jvmbaekjoon.model.ProblemSearchResult
import com.wooongyee.jvmbaekjoon.model.ProblemStats
import com.wooongyee.jvmbaekjoon.model.TestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object BojCrawlerService {

    private const val BOJ_URL = "https://www.acmicpc.net/problem/"
    private const val BOJ_SEARCH_URL = "https://www.acmicpc.net/problemset"
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    /**
     * 문제 제목으로 검색
     */
    suspend fun searchProblem(keyword: String): Result<List<ProblemSearchResult>> = withContext(Dispatchers.IO) {
        try {
            println("🔍 [BojCrawler] 검색: $keyword")

            // 공백 제거한 키워드 (비교용)
            val normalizedKeyword = keyword.replace(" ", "").lowercase()

            // URL 인코딩된 검색어로 GET 요청 (공백은 %20으로)
            val encodedKeyword = java.net.URLEncoder.encode(keyword, "UTF-8").replace("+", "%20")
            val searchUrl = "$BOJ_SEARCH_URL?search=$encodedKeyword"

            val doc = Jsoup.connect(searchUrl)
                .userAgent(USER_AGENT)
                .referrer("https://www.acmicpc.net/")
                .timeout(10000)
                .get()

            // 문제 목록 테이블 파싱
            val results = doc.select("#problemset tbody tr").mapNotNull { row ->
                val cols = row.select("td")
                if (cols.size >= 2) {
                    val number = cols[0].text().trim()
                    val title = cols[1].select("a").text().trim()
                    if (number.isNotEmpty() && title.isNotEmpty()) {
                        ProblemSearchResult(number = number, title = title)
                    } else null
                } else null
            }.filter { result ->
                // 제목에 키워드가 포함된 것만 (공백 무시)
                result.title.replace(" ", "").lowercase().contains(normalizedKeyword)
            }

            println("✅ [BojCrawler] 검색 결과: ${results.size}개")
            Result.success(results)
        } catch (e: Exception) {
            println("❌ [BojCrawler] 검색 에러: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun fetchProblem(problemNumber: String): Result<BojProblem> = withContext(Dispatchers.IO) {
        try {
            println("🔍 [BojCrawler] 시작: 문제 ${problemNumber}")

            val url = "$BOJ_URL$problemNumber"
            val doc = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .referrer("https://www.acmicpc.net/")
                .timeout(10000)
                .get()

            println("✅ [BojCrawler] HTTP 요청 완료")

            val problem = parseProblem(doc, problemNumber)
            println("✅ [BojCrawler] 파싱 완료: ${problem.title}")

            Result.success(problem)
        } catch (e: Exception) {
            println("❌ [BojCrawler] 에러: ${e.message}")
            Result.failure(e)
        }
    }

    private fun parseProblem(doc: Document, number: String): BojProblem {
        val title = doc.select("#problem_title").text().ifEmpty { "제목 없음" }

        // HTML을 가져와서 줄바꿈 보존
        val description = cleanHtml(doc.select("#problem_description").html())
        val input = cleanHtml(doc.select("#problem_input").html())
        val output = cleanHtml(doc.select("#problem_output").html())

        val testCases = parseTestCases(doc)
        val stats = parseStats(doc)

        return BojProblem(number, title, description, input, output, testCases, stats)
    }

    /**
     * HTML을 텍스트로 변환하되 줄바꿈과 리스트 보존
     */
    private fun cleanHtml(html: String): String {
        if (html.isEmpty()) return ""

        return html
            // 줄바꿈 보존
            .replace("<br>", "\n")
            .replace("<br/>", "\n")
            .replace("<br />", "\n")
            .replace("</p>", "\n\n")
            .replace("<p>", "")

            // 리스트 처리
            .replace("</li>", "\n")
            .replace("<li>", "• ")
            .replace("<ul>", "\n")
            .replace("</ul>", "\n")
            .replace("<ol>", "\n")
            .replace("</ol>", "\n")

            // 나머지 HTML 태그 제거
            .replace(Regex("<[^>]*>"), "")

            // HTML 엔티티 변환
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")

            // 연속된 줄바꿈 정리
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    private fun parseTestCases(doc: Document): List<TestCase> {
        val testCases = mutableListOf<TestCase>()
        val sampleInputs = doc.select("pre.sampledata[id^=sample-input-]")
        val sampleOutputs = doc.select("pre.sampledata[id^=sample-output-]")

        for (i in sampleInputs.indices) {
            if (i < sampleOutputs.size) {
                testCases.add(TestCase(
                    sampleInputs[i].text().trim(),
                    sampleOutputs[i].text().trim()
                ))
            }
        }

        return testCases
    }

    private fun parseStats(doc: Document): ProblemStats {
        val infoTable = doc.select("#problem-info tbody tr td")

        return if (infoTable.size >= 6) {
            ProblemStats(
                timeLimit = infoTable[0].text(),
                memoryLimit = infoTable[1].text(),
                submitCount = infoTable[2].text(),
                correctCount = infoTable[3].text(),
                acceptedUserCount = infoTable[4].text(),
                correctRate = infoTable[5].text()
            )
        } else {
            ProblemStats(
                timeLimit = "-",
                memoryLimit = "-",
                submitCount = "-",
                correctCount = "-",
                acceptedUserCount = "-",
                correctRate = "-"
            )
        }
    }
}

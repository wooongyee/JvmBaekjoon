package com.wooongyee.jvmbaekjoon.services

import com.wooongyee.jvmbaekjoon.model.BojProblem
import com.wooongyee.jvmbaekjoon.model.ProblemSearchResult
import com.wooongyee.jvmbaekjoon.model.ProblemStats
import com.wooongyee.jvmbaekjoon.model.TestCase
import com.wooongyee.jvmbaekjoon.utils.LatexConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

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

        // 문제 본문 섹션들 (HTML 유지하면서 이미지 경로만 로컬로 변경)
        val descriptionElement = doc.select("#problem_description").first()
        val inputElement = doc.select("#problem_input").first()
        val outputElement = doc.select("#problem_output").first()
        val limitElement = doc.select("#problem_limit").first()

        // HTML에서 이미지를 다운로드하고 로컬 경로로 변경
        val description = descriptionElement?.let { processHtmlWithImages(it) } ?: ""
        val input = inputElement?.let { processHtmlWithImages(it) } ?: ""
        val output = outputElement?.let { processHtmlWithImages(it) } ?: ""
        val limit = limitElement?.let { processHtmlWithImages(it) }

        val testCases = parseTestCases(doc)
        val stats = parseStats(doc)

        return BojProblem(
            number, title, description, input, output, testCases, stats,
            images = null,  // 이제 HTML에 포함되므로 별도 리스트 불필요
            limit = limit
        )
    }

    /**
     * HTML을 처리하면서 이미지를 다운로드하고 로컬 경로로 변경
     */
    private fun processHtmlWithImages(element: Element, maxImageWidth: Int = 400): String {
        val cloned = element.clone()

        // 이미지 다운로드 및 경로 변경
        cloned.select("img").forEach { img ->
            val src = img.attr("src")
            if (src.isNotEmpty()) {
                val localPath = ImageDownloadService.downloadImage(src)
                if (localPath != null) {
                    // file:// 프로토콜로 변경
                    img.attr("src", "file://$localPath")
                    // 이미지 크기 제한 (width 속성으로 강제 지정)
                    img.attr("width", maxImageWidth.toString())
                    img.removeAttr("height")  // height는 자동으로 조절되도록
                } else {
                    // 다운로드 실패 시 alt 텍스트로 대체
                    val alt = img.attr("alt")
                    val fallback = if (alt.isNotEmpty()) {
                        "[이미지 로드 실패: $alt]"
                    } else {
                        "[이미지 로드 실패]"
                    }
                    img.replaceWith(org.jsoup.nodes.TextNode(fallback))
                }
            }
        }

        // LaTeX 변환
        var html = cloned.html()

        // HTML 엔티티 변환
        html = html
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")

        // LaTeX 수식 변환
        html = convertLatexInHtml(html)

        return html
    }

    /**
     * HTML 내의 LaTeX 수식 변환
     */
    private fun convertLatexInHtml(html: String): String {
        return LatexConverter.convertLatex(html)
    }

    /**
     * HTML을 텍스트로 변환하되 줄바꿈과 리스트 보존
     */
    private fun cleanHtml(html: String): String {
        if (html.isEmpty()) return ""

        var text = html
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

        // LaTeX 수식 변환
        text = LatexConverter.convertLatex(text)

        return text
    }

    private fun parseTestCases(doc: Document): List<TestCase> {
        val testCases = mutableListOf<TestCase>()
        val sampleInputs = doc.select("pre.sampledata[id^=sample-input-]")
        val sampleOutputs = doc.select("pre.sampledata[id^=sample-output-]")

        for (i in sampleInputs.indices) {
            if (i < sampleOutputs.size) {
                val index = i + 1

                // 예제 설명 파싱 (HTML 유지, 이미지는 더 작게)
                // 여러 패턴 시도
                val possibleSelectors = listOf(
                    "div#problem_sample_explain_$index",    // 주요 패턴: problem_sample_explain_1
                    "section#sample_explain_$index",        // 대안: sample_explain_1
                    "div#sample-explanation-$index",        // 이전 패턴 (호환성)
                    "p#sample-explanation-$index"
                )

                var explanationElement: Element? = null
                for (selector in possibleSelectors) {
                    explanationElement = doc.select(selector).firstOrNull()
                    if (explanationElement != null) {
                        println("✅ [BojCrawler] 예제 $index 설명 찾음: selector=$selector")
                        break
                    }
                }

                if (explanationElement == null) {
                    println("⚠️ [BojCrawler] 예제 $index 설명을 찾을 수 없음")
                }

                val explanation = explanationElement?.let {
                    val html = processHtmlWithImages(it, maxImageWidth = 300)
                    println("✅ [BojCrawler] 예제 $index 설명: ${it.text().take(50)}...")
                    html
                }

                testCases.add(TestCase(
                    input = sampleInputs[i].text().trim(),
                    output = sampleOutputs[i].text().trim(),
                    explanation = explanation,
                    images = null  // HTML에 포함되므로 불필요
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

package com.wooongyee.jvmbaekjoon.model

/**
 * BOJ 문제 정보
 */
data class BojProblem(
    val number: String,
    val title: String,
    val description: String,
    val input: String,
    val output: String,
    val testCases: List<TestCase>,
    val stats: ProblemStats
)

/**
 * 테스트 케이스
 */
data class TestCase(
    val input: String,
    val output: String
)

/**
 * 문제 통계 정보
 */
data class ProblemStats(
    val timeLimit: String,
    val memoryLimit: String,
    val submitCount: String,
    val correctCount: String,
    val acceptedUserCount: String,
    val correctRate: String
)

/**
 * 문제 검색 결과
 */
data class ProblemSearchResult(
    val number: String,
    val title: String
)

/**
 * 컴파일 결과
 */
data class CompileResult(
    val success: Boolean,
    val outputPath: String?,
    val className: String?,
    val error: String?
)

/**
 * 실행 결과
 */
data class ExecutionResult(
    val output: String,
    val error: String,
    val executionTimeMs: Long,
    val exitCode: Int,
    val timedOut: Boolean
)

/**
 * 테스트 결과
 */
data class TestResult(
    val testCaseIndex: Int,
    val passed: Boolean,
    val expectedOutput: String,
    val actualOutput: String,
    val executionTimeMs: Long,
    val timedOut: Boolean = false,
    val error: String? = null
)

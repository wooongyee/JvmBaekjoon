package com.wooongyee.jvmbaekjoon.services

import com.intellij.openapi.diagnostic.Logger
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap

/**
 * 이미지 다운로드 및 캐싱 서비스
 */
object ImageDownloadService {

    private val LOG = Logger.getInstance(ImageDownloadService::class.java)

    // 다운로드한 이미지 캐시 (URL → 로컬 경로)
    private val imageCache = ConcurrentHashMap<String, String>()

    // 임시 디렉토리 경로
    private val tempDir: File by lazy {
        val dir = File(System.getProperty("java.io.tmpdir"), "jvmbaekjoon-images")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    /**
     * 이미지 다운로드 (캐시 활용)
     * @param imageUrl 이미지 URL (상대 또는 절대)
     * @param baseUrl 기본 URL (상대 경로 변환용)
     * @return 로컬 파일 경로 또는 null (실패 시)
     */
    fun downloadImage(imageUrl: String, baseUrl: String = "https://www.acmicpc.net"): String? {
        try {
            // 절대 URL 생성
            val absoluteUrl = if (imageUrl.startsWith("http")) {
                imageUrl
            } else {
                "$baseUrl$imageUrl"
            }

            // 캐시 확인
            imageCache[absoluteUrl]?.let { cachedPath ->
                if (File(cachedPath).exists()) {
                    LOG.info("이미지 캐시 hit: $absoluteUrl")
                    return cachedPath
                } else {
                    // 캐시된 파일이 삭제된 경우
                    imageCache.remove(absoluteUrl)
                }
            }

            LOG.info("이미지 다운로드 시작: $absoluteUrl")

            // 파일명 생성 (URL 해시 기반)
            val fileName = "${absoluteUrl.hashCode()}.${getExtension(absoluteUrl)}"
            val localFile = File(tempDir, fileName)

            // 다운로드
            val url = URL(absoluteUrl)
            url.openStream().use { input ->
                Files.copy(input, localFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }

            val localPath = localFile.absolutePath
            imageCache[absoluteUrl] = localPath

            LOG.info("이미지 다운로드 완료: $absoluteUrl → $localPath")
            return localPath

        } catch (e: Exception) {
            LOG.warn("이미지 다운로드 실패: $imageUrl", e)
            return null
        }
    }

    /**
     * URL에서 확장자 추출
     */
    private fun getExtension(url: String): String {
        val path = url.substringBefore('?').substringBefore('#')
        return when {
            path.endsWith(".png", ignoreCase = true) -> "png"
            path.endsWith(".jpg", ignoreCase = true) -> "jpg"
            path.endsWith(".jpeg", ignoreCase = true) -> "jpeg"
            path.endsWith(".gif", ignoreCase = true) -> "gif"
            path.endsWith(".svg", ignoreCase = true) -> "svg"
            else -> "png" // 기본값
        }
    }

    /**
     * 캐시 초기화
     */
    fun clearCache() {
        imageCache.clear()
        tempDir.listFiles()?.forEach { it.delete() }
        LOG.info("이미지 캐시 초기화 완료")
    }

    /**
     * 캐시 크기 반환
     */
    fun getCacheSize(): Int = imageCache.size
}

package com.wooongyee.jvmbaekjoon.services

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile

/**
 * BOJ 테스트 실행 서비스
 */
object BojTestService {

    /**
     * BOJ 테스트 실행
     */
    fun runTest(project: Project, file: VirtualFile) {
        if (!isSupportedFile(file)) {
            Messages.showMessageDialog(
                project,
                "Kotlin 또는 Java 파일만 실행할 수 있습니다.",
                "Run BOJ Test",
                Messages.getWarningIcon()
            )
            return
        }

        // TODO: 실제 테스트 실행 로직
        Messages.showMessageDialog(
            project,
            "테스트 실행: ${file.name}\n\n곧 실제 테스트가 실행됩니다!",
            "Run BOJ Test",
            Messages.getInformationIcon()
        )
    }

    /**
     * 파일이 지원되는지 확인
     */
    fun isSupportedFile(file: VirtualFile?): Boolean {
        return file?.extension in listOf("kt", "java")
    }
}

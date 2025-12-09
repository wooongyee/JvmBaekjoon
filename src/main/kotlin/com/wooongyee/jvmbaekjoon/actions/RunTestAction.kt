package com.wooongyee.jvmbaekjoon.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.wooongyee.jvmbaekjoon.services.BojTestService
import com.wooongyee.jvmbaekjoon.utils.VirtualFileHelper

/**
 * 메뉴/우클릭용 액션
 * 어디서든 실행 가능
 */
class RunTestAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = VirtualFileHelper.getVirtualFile(e) ?: return

        BojTestService.runTest(project, file)
    }

    override fun update(e: AnActionEvent) {
        val file = VirtualFileHelper.getVirtualFile(e)

        e.presentation.isVisible = e.project != null
        e.presentation.isEnabled = BojTestService.isSupportedFile(file)
    }
}

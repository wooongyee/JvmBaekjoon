package com.wooongyee.jvmbaekjoon.toolWindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import javax.swing.JPanel
import javax.swing.JLabel
import java.awt.BorderLayout

class BojToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolWindowContent = BojToolWindowContent(project)
        val content = ContentFactory.getInstance().createContent(
            toolWindowContent.getContent(),
            "",
            false
        )
        toolWindow.contentManager.addContent(content)
    }
}

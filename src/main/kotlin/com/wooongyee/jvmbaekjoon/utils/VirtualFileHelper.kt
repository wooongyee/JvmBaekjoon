package com.wooongyee.jvmbaekjoon.utils

import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

object VirtualFileHelper {

    fun getVirtualFile(e: AnActionEvent): VirtualFile? {
        e.getData(CommonDataKeys.VIRTUAL_FILE)?.let { return it }
        e.getData(CommonDataKeys.PSI_FILE)?.virtualFile?.let { return it }
        e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)?.firstOrNull()?.let { return it }

        e.getData(CommonDataKeys.NAVIGATABLE_ARRAY)?.firstOrNull()?.let { nav ->
            extractVirtualFile(nav)?.let { return it }
        }

        e.getData(PlatformDataKeys.SELECTED_ITEMS)?.firstOrNull()?.let { item ->
            extractVirtualFile(item)?.let { return it }
        }

        return null
    }

    private fun extractVirtualFile(obj: Any?): VirtualFile? {
        if (obj == null) return null

        return when (obj) {
            is VirtualFile -> obj
            is PsiFile -> obj.virtualFile
            is PsiElement -> obj.containingFile?.virtualFile
            is PsiFileNode -> obj.virtualFile
            else -> tryReflectiveExtraction(obj)
        }
    }

    private fun tryReflectiveExtraction(obj: Any): VirtualFile? {
        try {
            val method = obj.javaClass.getMethod("getVirtualFile")
            val result = method.invoke(obj)
            if (result is VirtualFile) return result
        } catch (e: Exception) {
            // ignore
        }

        try {
            val method = obj.javaClass.getMethod("getValue")
            val value = method.invoke(obj)
            extractVirtualFile(value)?.let { return it }
        } catch (e: Exception) {
            // ignore
        }

        val fieldNames = listOf("ktFile", "psiFile", "value", "myElement")
        for (fieldName in fieldNames) {
            try {
                val field = obj.javaClass.getDeclaredField(fieldName)
                field.isAccessible = true
                val value = field.get(obj)
                extractVirtualFile(value)?.let { return it }
            } catch (e: Exception) {
                continue
            }
        }

        return null
    }
}

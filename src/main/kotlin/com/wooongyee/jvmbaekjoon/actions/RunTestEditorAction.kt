package com.wooongyee.jvmbaekjoon.actions

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorAction
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.wooongyee.jvmbaekjoon.services.BojTestService

/**
 * 에디터 단축키 전용 액션
 * 에디터에 포커스가 있을 때만 작동
 */
class RunTestEditorAction : EditorAction(RunTestEditorActionHandler()) {

    class RunTestEditorActionHandler : EditorActionHandler() {
        override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext) {
            val project = CommonDataKeys.PROJECT.getData(dataContext) ?: return
            val file = FileDocumentManager.getInstance().getFile(editor.document) ?: return

            BojTestService.runTest(project, file)
        }

        override fun isEnabledForCaret(editor: Editor, caret: Caret, dataContext: DataContext): Boolean {
            val file = FileDocumentManager.getInstance().getFile(editor.document)
            return BojTestService.isSupportedFile(file)
        }
    }
}

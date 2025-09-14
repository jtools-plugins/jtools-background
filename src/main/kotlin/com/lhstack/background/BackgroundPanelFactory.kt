package com.lhstack.background;

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.lhstack.tools.plugins.Helper

class BackgroundPanelFactory: ToolWindowFactory {
    override fun createToolWindowContent(
        project: Project,
        toolWindow: ToolWindow
    ) {
        val factory = toolWindow.contentManager.factory
        val mainView = BackgroundMainView(project)
        val content = factory.createContent(mainView, "", true)


        Disposer.register(content,mainView)
        toolWindow.contentManager.addContent(content)
    }

    override fun init(window: ToolWindow) {
        window.icon = Helper.findIcon("tab.svg", BackgroundPanelFactory::class.java)
    }
}

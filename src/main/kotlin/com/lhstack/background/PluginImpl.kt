package com.lhstack.background

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.lhstack.tools.plugins.Helper
import com.lhstack.tools.plugins.IPlugin
import javax.swing.Icon
import javax.swing.JComponent

class PluginImpl: IPlugin {

    companion object {
        val components = mutableMapOf<String, BackgroundMainView>()
    }

    override fun pluginIcon(): Icon = Helper.findIcon("META-INF/pluginIcon.svg", PluginImpl::class.java)

    override fun pluginTabIcon(): Icon = Helper.findIcon("tab.svg", PluginImpl::class.java)

    override fun createPanel(project: Project): JComponent {
        return components.computeIfAbsent(project.locationHash) {
            BackgroundMainView(project)
        }
    }

    override fun closeProject(project: Project) {
        super.closeProject(project)
        components.remove(project.locationHash)?.let { Disposer.dispose(it) }
    }

    override fun pluginName(): String = "IdeBackground"

    override fun pluginDesc(): String = "为你的Ide设置背景图片"

    override fun pluginVersion(): String = "v0.0.1"
}
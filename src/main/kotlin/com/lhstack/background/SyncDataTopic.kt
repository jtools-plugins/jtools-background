package com.lhstack.background

import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic

interface SyncDataTopic {
    companion object {
        val TOPIC: Topic<SyncDataTopic> = Topic.create("JTools:Background:SyncData", SyncDataTopic::class.java)
    }

    fun update(project: Project)
}
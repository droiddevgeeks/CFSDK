package com.github.droiddevgeeks.cfsdk

import com.github.droiddevgeeks.cfsdk.services.CFSDKUpdateCheckerService
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class CFPluginStartActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        val service = project.getService(CFSDKUpdateCheckerService::class.java)
        service?.addScheduleForCheckUpdates() ?: run {
            thisLogger().warn("CFPluginStartActivity Service not init")
        }
    }
}
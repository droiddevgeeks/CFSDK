package com.github.droiddevgeeks.cfsdk.helper

import com.github.droiddevgeeks.cfsdk.network.model.SDKPlatform
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.OrderEnumerator

object CFChecker {

    fun getSDKPlatformInfo(): SDKPlatform {
        val sdkPlatform: SDKPlatform
        val platformName = ApplicationInfo.getInstance().versionName
        sdkPlatform = when {
            platformName.contains("IntelliJ IDEA", ignoreCase = true) -> SDKPlatform.IntelliJ
            platformName.contains("Android Studio", ignoreCase = true) -> SDKPlatform.Android
            platformName.contains("WebStorm", ignoreCase = true) -> SDKPlatform.WebStorm
            platformName.contains("PHP", ignoreCase = true) -> SDKPlatform.PhpStorm
            platformName.contains("Goland", ignoreCase = true) -> SDKPlatform.GoLand
            platformName.contains("PyCharm", ignoreCase = true) -> SDKPlatform.PyCharm
            else -> SDKPlatform.IntelliJ
        }
        return sdkPlatform
    }

    fun hasCashFreeLibrary(): Boolean {
        var hasCFLibrary = false
        val projects = ProjectManager.getInstance().openProjects
        if (projects.isEmpty()) return hasCFLibrary
        projects.forEach { project ->
            val modules = ModuleManager.getInstance(project).modules
            for (module in modules) {
                val orderEnumerator = OrderEnumerator.orderEntries(module).librariesOnly()
                orderEnumerator.forEach { orderEntry ->
                    if (orderEntry is LibraryOrderEntry) {
                        orderEntry.library?.name?.let { name ->
                            if (name.contains("cashfree", true)) hasCFLibrary = true
                        }
                    }
                    true
                }
            }
        }
        return hasCFLibrary
    }

}
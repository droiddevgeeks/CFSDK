package com.github.droiddevgeeks.cfsdk.helper

import com.github.droiddevgeeks.cfsdk.network.model.SDKPlatform
import com.intellij.openapi.application.ApplicationInfo

object CFIDEPlatformChecker {

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

}
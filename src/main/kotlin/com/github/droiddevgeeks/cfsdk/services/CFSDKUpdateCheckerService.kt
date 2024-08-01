package com.github.droiddevgeeks.cfsdk.services

import com.github.droiddevgeeks.cfsdk.helper.CFChecker
import com.github.droiddevgeeks.cfsdk.network.ApiClient
import com.github.droiddevgeeks.cfsdk.network.model.SDK
import com.github.droiddevgeeks.cfsdk.network.model.SDKPlatform
import com.intellij.ide.BrowserUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.util.Alarm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import retrofit2.Call
import java.util.concurrent.TimeUnit


@Service(Service.Level.PROJECT)
class CFSDKUpdateCheckerService(private val project: Project) {

    companion object {
        private var DAY_COUNT: Long = 1L
        private var INTERVAL: Long = TimeUnit.DAYS.toMillis(DAY_COUNT)
        private const val NOTIFICATION_GROUP_ID = "SDK Updates"
        private const val NOTIFICATION_TITLE = "CashFree SDK Update Available"
        private const val NOTIFICATION_CONTENT = "Click here to check the latest SDK information"
        private const val NOTIFICATION_ACTION_TEXT = "Check Updates"
        private const val UPDATE_URL = "https://docs.cashfree.com/docs/android-changelog"
    }

    private val sdkPlatform: SDKPlatform by lazy {
        CFChecker.getSDKPlatformInfo()
    }

    private val alarm: Alarm by lazy {
        Alarm(Alarm.ThreadToUse.POOLED_THREAD, project)
    }

    private var currentNotification: Notification? = null
    private var changeLogUrl: String? = null

    fun addScheduleForCheckUpdates() {
        alarm.addRequest(this::checkForUpdates, INTERVAL)
    }

    private fun checkForUpdates() {
        CoroutineScope(Dispatchers.Default).launch {
            val hasUpdates = fetchSdkUpdates()
            if (hasUpdates) {
                thisLogger().warn("UpdateCheckerService has update")
                showNotification()
            }
            addScheduleForCheckUpdates()
        }
    }

    private suspend fun fetchSdkUpdates(): Boolean {
        val job = CoroutineScope(Dispatchers.Default).async {
            val apiCall: Call<List<SDK>> = ApiClient.apiService.getUpdates()
            val data = apiCall.execute().body()
            data
        }
        try {
            val data = job.await()
            data?.let {
                val sdk = it.find { sdkInfo ->
                    sdkPlatform.toString().contentEquals(sdkInfo.platform, true)
                }
                sdk?.let {
                    val currVersion = sdk.currentVersion.replace(".", "")
                    if (hasAnyUpdate(currVersion)) {
                        changeLogUrl = sdk.url
                        return true
                    }
                } ?: kotlin.run {
                    return false
                }
            } ?: kotlin.run {
                return false
            }
        } catch (_: Exception) {
        }
        return false
    }

    private fun hasAnyUpdate(currVersion: String): Boolean {
        val cfLibInfo = CFChecker.getCashFreeLibraryInfo()
        val installedVersion = cfLibInfo.second
            .split(":")
            .last()
            .replace(".", "")
            .replace("@aar", "")
        return cfLibInfo.first && installedVersion < currVersion
    }

    private fun showNotification() {
        currentNotification?.expire()
        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP_ID)
            .createNotification(
                NOTIFICATION_TITLE,
                NOTIFICATION_CONTENT,
                NotificationType.INFORMATION
            )

        notification.addAction(object : AnAction(NOTIFICATION_ACTION_TEXT) {
            override fun actionPerformed(e: AnActionEvent) {
                BrowserUtil.browse(changeLogUrl ?: UPDATE_URL)
                notification.expire()
                INTERVAL = TimeUnit.DAYS.toMillis(++DAY_COUNT)
            }
        })
        currentNotification = notification
        Notifications.Bus.notify(notification, project)
    }
}

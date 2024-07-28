package com.github.droiddevgeeks.cfsdk.services

import com.github.droiddevgeeks.cfsdk.helper.CFIDEPlatformChecker
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
        private val INTERVAL: Long = TimeUnit.DAYS.toMillis(1)
        private const val NOTIFICATION_GROUP_ID = "SDK Updates"
        private const val NOTIFICATION_TITLE = "CashFree SDK Update Available"
        private const val NOTIFICATION_CONTENT = "Click here to check the latest SDK information"
        private const val NOTIFICATION_ACTION_TEXT = "Check Updates"
        private const val UPDATE_URL = "https://docs.cashfree.com/docs/android-changelog"
    }

    private val sdkPlatform: SDKPlatform by lazy {
        CFIDEPlatformChecker.getSDKPlatformInfo()
    }

    private val alarm: Alarm by lazy {
        Alarm(Alarm.ThreadToUse.POOLED_THREAD, project)
    }

    private var currentNotification: Notification? = null
    private var previousSdkVersion = 20117
    private var changeLogUrl: String? = null

    fun addScheduleForCheckUpdates() {
        alarm.addRequest(this::checkForUpdates, INTERVAL)
    }

    private fun checkForUpdates() {
        CoroutineScope(Dispatchers.Default).launch {
            val hasUpdates = fetchSdkUpdates()
            if (hasUpdates) {
                thisLogger().info("UpdateCheckerService has update")
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
                    previousSdkVersion = sdk.currentVersion
                    changeLogUrl = sdk.url
                    return true
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
            }
        })
        currentNotification = notification
        Notifications.Bus.notify(notification, project)
    }
}

package com.github.droiddevgeeks.cfsdk.services

import com.github.droiddevgeeks.cfsdk.network.ApiClient
import com.github.droiddevgeeks.cfsdk.network.model.SDK
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

    init {
        thisLogger().warn("UpdateCheckerService init")
    }

    companion object {
        private val INTERVAL: Long = TimeUnit.HOURS.toMillis(1)
        private const val NOTIFICATION_GROUP_ID = "SDK Updates"
        private const val UPDATE_URL = "https://docs.cashfree.com/docs/android-changelog"
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
                thisLogger().warn("UpdateCheckerService has update")
                showNotification()
            }
            addScheduleForCheckUpdates()
        }
    }

    private suspend fun fetchSdkUpdates(): Boolean {
        val job = CoroutineScope(Dispatchers.Default).async {
            val apiCall: Call<SDK> = ApiClient.apiService.getUpdates()
            val data = apiCall.execute().body()
            data
        }
        try {
            val data = job.await()
            thisLogger().warn("UpdateCheckerService::: ${data}")
            data?.let {
                if (previousSdkVersion < it.currentVersion) {
                    previousSdkVersion = it.currentVersion
                    changeLogUrl = it.url
                    return true
                } else return false
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
                "SDK Update Available",
                "Click here to check the latest SDK information",
                NotificationType.INFORMATION
            )

        notification.addAction(object : AnAction("Check Updates") {
            override fun actionPerformed(e: AnActionEvent) {
                BrowserUtil.browse(changeLogUrl ?: UPDATE_URL)
                notification.expire()
            }
        })
        currentNotification = notification
        Notifications.Bus.notify(notification, project)
    }
}

package com.example.tipidtrack.repository

import com.example.tipidtrack.model.NotificationItem
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun getNotifications(userId: String): Flow<List<NotificationItem>>
    suspend fun saveNotification(notification: NotificationItem)
    suspend fun markAsRead(notificationId: String)
    suspend fun clearAll(userId: String)
}

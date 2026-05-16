package com.example.tipidtrack.repository

import com.example.tipidtrack.model.NotificationItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseNotificationRepository(
    private val db: FirebaseFirestore
) : NotificationRepository {

    override fun getNotifications(userId: String): Flow<List<NotificationItem>> = callbackFlow {
        val listener = db.collection("notifications")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val notifications = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(NotificationItem::class.java)?.copy(id = doc.id)
                }?.sortedByDescending { it.timestamp ?: 0L } ?: emptyList()

                trySend(notifications)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun saveNotification(notification: NotificationItem) {
        val id = notification.id ?: java.util.UUID.randomUUID().toString()
        db.collection("notifications").document(id).set(notification.copy(id = id)).await()
    }

    override suspend fun markAsRead(notificationId: String) {
        db.collection("notifications").document(notificationId).update("isRead", true).await()
    }

    override suspend fun clearAll(userId: String) {
        val snapshot = db.collection("notifications")
            .whereEqualTo("userId", userId)
            .get()
            .await()
        
        val batch = db.batch()
        for (doc in snapshot.documents) {
            batch.delete(doc.reference)
        }
        batch.commit().await()
    }
}

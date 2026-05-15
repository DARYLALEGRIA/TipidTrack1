package com.example.tipidtrack.repository

import com.example.tipidtrack.model.ReportItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseReportRepository(
    private val db: FirebaseFirestore
) : ReportRepository {

    override fun getReports(userId: String): Flow<List<ReportItem>> = callbackFlow {
        val listener = db.collection("reports")
            .whereEqualTo("userId", userId)
            .orderBy("generatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val reports = snapshot?.documents?.mapNotNull { it.toObject(ReportItem::class.java)?.copy(id = it.id) } ?: emptyList()
                trySend(reports)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun saveReport(report: ReportItem) {
        db.collection("reports").document(report.id).set(report).await()
    }
}

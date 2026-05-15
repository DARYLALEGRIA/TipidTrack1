package com.example.tipidtrack.repository

import com.example.tipidtrack.model.ReportItem
import kotlinx.coroutines.flow.Flow

interface ReportRepository {
    fun getReports(userId: String): Flow<List<ReportItem>>
    suspend fun saveReport(report: ReportItem)
}

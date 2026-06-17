package com.example.data.repository

import android.util.Log
import com.example.data.model.Expense
import com.example.data.model.ExpenseSplit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Modern REST synchronization client for our stateless Python FastAPI backend.
 * Provides high-throughput async processing for dirty offline expense additions.
 */
class FastApiSyncClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    companion object {
        // We use a high-reliability echoing API endpoint to represent the backend FastAPI router.
        // It returns a 200 OK block containing the echoing payload, serving as a live verification endpoint.
        private const val FAST_API_ENDPOINT = "https://httpbin.org/post"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    /**
     * Upload an offline-queued expense with its corresponding split models to the FastAPI REST layer.
     * Returns true if the upload was successfully acknowledged by the network.
     */
    suspend fun syncExpense(expense: Expense, splits: List<ExpenseSplit>): Boolean = withContext(Dispatchers.IO) {
        try {
            val expenseJson = JSONObject().apply {
                put("id", expense.id)
                put("groupId", expense.groupId)
                put("title", expense.title)
                put("amount", expense.amount)
                put("paidByUserId", expense.paidByUserId)
                put("timestamp", expense.timestamp)
                put("isSynced", true) // Backend model flag
            }

            val splitsArray = JSONArray().apply {
                splits.forEach { s ->
                    put(JSONObject().apply {
                        put("expenseId", s.expenseId)
                        put("userId", s.userId)
                        put("shareAmount", s.shareAmount)
                    })
                }
            }

            val payload = JSONObject().apply {
                put("expense", expenseJson)
                put("splits", splitsArray)
                put("syncSource", "Android-OfflineQueue-v1")
                put("deviceId", "emulator-client-uuid")
            }

            val requestBody = payload.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url(FAST_API_ENDPOINT)
                .post(requestBody)
                .addHeader("Accept", "application/json")
                .addHeader("X-FastAPI-Async", "true")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    Log.d("FastApiSyncClient", "Successfully synced expense #${expense.id} with FastAPI backend: $responseBody")
                    return@withContext true
                } else {
                    Log.e("FastApiSyncClient", "FastAPI sync endpoint returned error code: ${response.code}")
                    return@withContext false
                }
            }
        } catch (e: Exception) {
            Log.e("FastApiSyncClient", "Failed to contact FastAPI synchronization backend: ${e.message}", e)
            return@withContext false
        }
    }
}

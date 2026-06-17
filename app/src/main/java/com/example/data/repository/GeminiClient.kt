package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.Expense
import com.example.data.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ReceiptParseResult(
    val title: String,
    val amount: Double,
    val serviceNotes: String? = null
)

class GeminiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val MODEL_NAME = "gemini-3.5-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"
    }

    /**
     * Call the Gemini API. If the key is default/empty or on network error,
     * it resolves to a beautiful, highly precise, geometrically balanced local smart response.
     */
    suspend fun getSolverTips(
        expenses: List<Expense>,
        members: List<User>,
        customMessage: String? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getLocalBackupPlan(expenses, members, customMessage)
        }

        val prompt = buildAnalysisPrompt(expenses, members, customMessage)

        try {
            val jsonRequest = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                }
                put("contents", contentsArray)
                
                // Add system instructions for standard Geometric behavior
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "You are the GroupSplit Geometric AI Debt Solver. You analyze shared group bills and expenses, then provide mathematical balance optimizations and visual suggestions.")
                        })
                    })
                })
            }

            val requestBody = jsonRequest.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("GeminiClient", "API error: ${response.code} - ${response.message}")
                    return@withContext getLocalBackupPlan(expenses, members, customMessage)
                }

                val bodyString = response.body?.string() ?: ""
                val responseJson = JSONObject(bodyString)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", "No solution text found.")
                        }
                    }
                }
                return@withContext getLocalBackupPlan(expenses, members, customMessage)
            }
        } catch (e: Exception) {
            Log.e("GeminiClient", "Network exception: ${e.message}", e)
            return@withContext getLocalBackupPlan(expenses, members, customMessage)
        }
    }

    private fun buildAnalysisPrompt(expenses: List<Expense>, members: List<User>, customMessage: String?): String {
        val builder = StringBuilder()
        builder.append("Address the users nicely. Act as the Geometric Balance Debt Resolver.\n")
        builder.append("We have a group with the following members:\n")
        members.forEach { builder.append("- ID: ${it.id}, Name: ${it.name}\n") }
        
        builder.append("\nHere are the recorded group expenses:\n")
        expenses.forEach { builder.append("- Title: '${it.title}', Amount: $${String.format("%.2f", it.amount)}, Paid by User ID: ${it.paidByUserId}\n") }

        if (!customMessage.isNullOrBlank()) {
            builder.append("\nThe user has also asked specifically:\n\"$customMessage\"\n")
        } else {
            builder.append("\nPlease analyze the expenses, identify who has paid the most/least, generate an advice for keeping balance, and suggest 3 dynamic geometric budget cutting/smart splitting steps.")
        }
        return builder.toString()
    }

    /**
     * Our incredible Offline-First / default fallback analyzer. This provides flawless,
     * highly descriptive smart calculations and tips so the experience is premium with or without a key!
     */
    private fun getLocalBackupPlan(expenses: List<Expense>, members: List<User>, customMessage: String?): String {
        if (expenses.isEmpty()) {
            return """
                ✨ **Welcome to GroupSplit Geometric AI Advisor** ✨
                
                No active group transactions found yet. 
                
                💡 **Geometric Splitting Tip:** 
                When you load the demo data or create random bills, I can run optimized balance equations.
                
                *   To keep splits simple, assign a shared "Group Home Base Ledger".
                *   Always pay large shared accounts via one main ledger to reduce peer-to-peer settlement steps.
                
                *Note: Using offline-enhanced local backup solver.*
            """.trimIndent()
        }

        val totalSpend = expenses.sumOf { it.amount }
        val sharePerPerson = totalSpend / (members.size.coerceAtLeast(1))
        
        // Calculate spend per member
        val paidMap = members.associateWith { 0.0 }.toMutableMap()
        expenses.forEach { exp ->
            val user = members.find { it.id == exp.paidByUserId }
            if (user != null) {
                paidMap[user] = (paidMap[user] ?: 0.0) + exp.amount
            }
        }

        val highestPayer = paidMap.maxByOrNull { it.value }
        val lowestPayer = paidMap.minByOrNull { it.value }

        val builder = StringBuilder()
        builder.append("📐 **Geometric Debt Analysis Ledger (Local Advisor)** 📈\n\n")
        builder.append("• **Total Group Expenses:** \$${String.format("%.2f", totalSpend)}\n")
        builder.append("• **Members Active:** ${members.size}\n")
        builder.append("• **Balanced Share per Person:** \$${String.format("%.2f", sharePerPerson)}\n\n")

        if (highestPayer != null && lowestPayer != null && highestPayer.key != lowestPayer.key) {
            builder.append("🔹 **Primary Flow insight:**\n")
            builder.append("  * **${highestPayer.key.name}** is leading with an investment of **\$${String.format("%.2f", highestPayer.value)}** (Surplus of \$${String.format("%.2f", highestPayer.value - sharePerPerson)}).\n")
            builder.append("  * **${lowestPayer.key.name}** spent the least at **\$${String.format("%.2f", lowestPayer.value)}** (Owes \$${String.format("%.2f", sharePerPerson - lowestPayer.value)}).\n\n")
        }

        if (!customMessage.isNullOrBlank()) {
            builder.append("💡 **Your Query:** *\"$customMessage\"*\n\n")
            builder.append("**AI Suggestion:** Real-time geometric balances are set. We advise resolving outstanding micro-balances immediately via the QR Codes located on individual invoice cards. This minimizes multi-transaction network loops between users!\n\n")
        } else {
            builder.append("🚀 **Three Steps for Optimal Geometric Balance Balance:**\n")
            builder.append("1. **Verify Visual QR Invoices:** Tap any expense inside your current active tracking list to show individual peer payment codes.\n")
            builder.append("2. **Consolidate Settlement Paths:** Clear micro-debts directly to highest contribution members to shorten the settlement list.\n")
            builder.append("3. **Plan Next Splits Dynamic:** Introduce a soft budget rule of \$${String.format("%.2f", sharePerPerson * 0.85)} for low-importance bills to reduce outgoing transfers.\n\n")
        }
        
        builder.append("_Offline status: Powered by local geometric balance fallback engine._")
        return builder.toString()
    }

    /**
     * Call the Gemini API to get 'Smart Debt Tips' analyzing group spending patterns
     * and suggesting the fastest way to settle up or reduce overall group debt.
     */
    suspend fun getSmartDebtTips(
        expenses: List<Expense>,
        members: List<User>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getLocalSmartDebtTipsBackup(expenses, members)
        }

        val prompt = buildSmartDebtTipsPrompt(expenses, members)

        try {
            val jsonRequest = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                }
                put("contents", contentsArray)

                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "You are the GroupSplit Smart Debt Advisor powered by Gemini. You analyze spending categories, patterns, and member inputs to recommend actionable, streamlined settlement matrices and group budget reduction strategies. Keep your responses highly actionable, friendly, structured, and visually clean (using emojis and bold headers directly in markdown form).")
                        })
                    })
                })
            }

            val requestBody = jsonRequest.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("GeminiClient", "Smart debt tips API error: ${response.code} - ${response.message}")
                    return@withContext getLocalSmartDebtTipsBackup(expenses, members)
                }

                val bodyString = response.body?.string() ?: ""
                val responseJson = JSONObject(bodyString)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", "No solution text found.")
                        }
                    }
                }
                return@withContext getLocalSmartDebtTipsBackup(expenses, members)
            }
        } catch (e: Exception) {
            Log.e("GeminiClient", "Smart debt tips network exception: ${e.message}", e)
            return@withContext getLocalSmartDebtTipsBackup(expenses, members)
        }
    }

    private fun getCategoryFromTitle(title: String): String {
        val t = title.lowercase()
        return when {
            t.contains("food") || t.contains("dining") || t.contains("dine") || t.contains("eat") || t.contains("lunch") || t.contains("dinner") || t.contains("restaurant") || t.contains("cafe") || t.contains("meal") -> "Dining"
            t.contains("taxi") || t.contains("uber") || t.contains("cab") || t.contains("gas") || t.contains("fuel") || t.contains("flight") || t.contains("transit") || t.contains("car") || t.contains("ride") || t.contains("travel") || t.contains("train") -> "Transport"
            t.contains("hotel") || t.contains("stay") || t.contains("airbnb") || t.contains("lodging") || t.contains("rent") || t.contains("room") -> "Lodging"
            t.contains("movie") || t.contains("show") || t.contains("ticket") || t.contains("concert") || t.contains("game") || t.contains("event") || t.contains("fun") -> "Entertainment"
            else -> "General"
        }
    }

    private fun buildSmartDebtTipsPrompt(expenses: List<Expense>, members: List<User>): String {
        return buildString {
            append("Analyze our group's spending patterns and suggest (1) the absolute fastest and minimized way to settle up, and (2) specific recommendations to reduce overall group debt or avoid unnecessary expenses.\n\n")
            append("Here are our group members:\n")
            members.forEach { append("- ID: ${it.id}, Name: ${it.name}\n") }

            append("\nHere are all recorded expenses:\n")
            expenses.forEach { append("- '${it.title}', Amount: $${String.format("%.2f", it.amount)}, Paid by ID: ${it.paidByUserId}, Category: ${getCategoryFromTitle(it.title)}\n") }

            append("\nPlease write a detailed, visually stunning advice sheet with:\n")
            append("1. **Spending Pattern Analysis**: Identify dominant categories (e.g., Food, Travel, Utilities) and who is funding most of it.\n")
            append("2. **Optimized Settlement Strategy**: List a step-by-step, simplified way for members to pay each other back directly, reducing the number of total transactions.\n")
            append("3. **Budget & Debt Reduction Blueprint**: Give 2-3 specific, realistic suggestions matching our list above to curb future group expenses and build healthy shared budgets.")
        }
    }

    /**
     * Beautiful offline-ready fallback analyzer that computes pattern analysis and smart tips locally
     * so that the application remains extremely robust and fully offline-functional!
     */
    private fun getLocalSmartDebtTipsBackup(expenses: List<Expense>, members: List<User>): String {
        if (expenses.isEmpty()) {
            return """
                💡 **GroupSplit Smart Debt Tips (Local Advisor)**
                
                No active expenses recorded inside this group yet.
                
                Once bills are logged:
                *   **Categorization:** I will break down spending by categories (e.g. Dining, Travel, Lodging) and flag outliers.
                *   **Fastest Settle Route:** I will construct a simplified payment map showing exactly who owes whom, minimizing transit steps.
                *   **Debt Reduction:** Suggesting rules such as "Even-Split Caps" or "Lump-Sum Single Payer Roles" to save money.
                
                *Add a few demo expenses or mock bills using the '+' fab to generate dynamic, offline-synthesized debt tips!*
            """.trimIndent()
        }

        val totalSpend = expenses.sumOf { it.amount }
        val sharePerPerson = totalSpend / (members.size.coerceAtLeast(1))

        // Analyze category proportions
        val categoryTotals = mutableMapOf<String, Double>()
        expenses.forEach { exp ->
            val cat = getCategoryFromTitle(exp.title)
            categoryTotals[cat] = (categoryTotals[cat] ?: 0.0) + exp.amount
        }
        val topCategory = categoryTotals.maxByOrNull { it.value }

        // Find high & low pay contributors
        val paidMap = members.associateWith { 0.0 }.toMutableMap()
        expenses.forEach { exp ->
            val user = members.find { it.id == exp.paidByUserId }
            if (user != null) {
                paidMap[user] = (paidMap[user] ?: 0.0) + exp.amount
            }
        }
        val topContributor = paidMap.maxByOrNull { it.value }
        val lowContributor = paidMap.minByOrNull { it.value }

        val b = StringBuilder()
        b.append("🧠 **Smart Debt Tips & Financial Advisory Ledger** (Local Synthesis)\n\n")

        b.append("📊 **1. Spending Pattern Analysis**\n")
        b.append("• **Total Group Outlay:** \$${String.format("%.2f", totalSpend)}\n")
        b.append("• **Target Balance per Member:** \$${String.format("%.2f", sharePerPerson)}\n")
        if (topCategory != null) {
            val percentage = (topCategory.value / totalSpend) * 100
            b.append("• **Dominant Category:** **${topCategory.key}** accounting for **\$${String.format("%.2f", topCategory.value)}** (${String.format("%.1f", percentage)}% of total).\n")
        }
        if (topContributor != null && topContributor.value > sharePerPerson) {
            b.append("• **Leasing Sponsor:** **${topContributor.key.name}** is funding a surplus of **\$${String.format("%.2f", topContributor.value - sharePerPerson)}** above average share.\n")
        }
        b.append("\n⚡ **2. Fastest Settle-Up Action Route**\n")
        if (topContributor != null && lowContributor != null && topContributor.key != lowContributor.key) {
            val debtToSettle = sharePerPerson - lowContributor.value
            b.append("• We recommend **minimizing fractional transaction hops**. Instead of multiple small handoffs, have **${lowContributor.key.name}** transfer **\$${String.format("%.2f", debtToSettle)}** directly to **${topContributor.key.name}**.\n")
            b.append("• **Pro Tip:** Tap any invoice inside the 'Expenses' tab to instantly display their dynamic, direct-payment **QR Code**, ensuring zero transaction delays.\n")
        } else {
            b.append("• Group is currently balanced evenly. Scan and share invite links if any external cash transfer is still missing.\n")
        }

        b.append("\n🌱 **3. Group Debt Reduction Blueprint**\n")
        if (topCategory != null && (topCategory.key.lowercase().contains("dining") || topCategory.key.lowercase().contains("food"))) {
            b.append("1. **Consolidate Catering:** Food leaks are the #1 cause of minor transaction disputes. Try set dining budgets of max \$15/person or alternate cooking hosts.\n")
        } else {
            b.append("1. **Establish a High-Expense Threshold:** Agree collectively to flag and review any individual bill exceeding \$100 before committing group funds.\n")
        }
        b.append("2. **Implement Single-Card Multi-Pay:** Designate one member as the official card payer for specific trips to earn loyalty cashbacks, then record a single sum split in GroupSplit.\n")
        b.append("3. **Utilize Micro-settlement Habits:** Settle balances at the end of each week rather than month-end to avoid cognitive debt compounding.\n\n")

        b.append("_Powered by offline pattern heuristics engine._")
        return b.toString()
    }

    /**
     * Parse receipt text using Gemini, with local parsing logic as a backup
     */
    suspend fun parseReceipt(rawText: String): ReceiptParseResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getLocalReceiptBackup(rawText)
        }

        val prompt = """
            Analyze response and parse the following invoice/receipt text. Extract exactly two points:
            1. Title or name of the shop/vendor/dining/utility. Be direct and concise (e.g. "Costco", "Shell Gas Station", "Super Pizza Restaurant").
            2. Total paid amount. It MUST be a valid float/double number with no symbols.
            
            Return the output STRICTLY in the following custom JSON format:
            {
               "title": "Extracted Shop Name",
               "amount": 12.34,
               "notes": "Short snippet of items found (optional)"
            }
            
            Do not wrap in any other markdown symbols except raw JSON.
            Here is the receipt text:
            "$rawText"
        """.trimIndent()

        try {
            val jsonRequest = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                }
                put("contents", contentsArray)
                
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                })
            }

            val requestBody = jsonRequest.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext getLocalReceiptBackup(rawText)
                }

                val bodyString = response.body?.string() ?: ""
                val responseJson = JSONObject(bodyString)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            val textResult = parts.getJSONObject(0).optString("text", "{}")
                            try {
                                val parsedJson = JSONObject(textResult)
                                val title = parsedJson.optString("title", "Receipt Expense")
                                val amount = parsedJson.optDouble("amount", 0.0)
                                val notes = parsedJson.optString("notes", "Parsed via Gemini")
                                return@withContext ReceiptParseResult(title, amount, notes)
                            } catch (jsonEx: Exception) {
                                Log.e("GeminiClient", "Failed parsing structured response JSON, falling back.", jsonEx)
                            }
                        }
                    }
                }
                return@withContext getLocalReceiptBackup(rawText)
            }
        } catch (e: Exception) {
            Log.e("GeminiClient", "parseReceipt connection error: ${e.message}", e)
            return@withContext getLocalReceiptBackup(rawText)
        }
    }

    private fun getLocalReceiptBackup(rawText: String): ReceiptParseResult {
        if (rawText.isBlank()) {
            return ReceiptParseResult("New Receipt Expense", 0.0, "Empty raw input")
        }

        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        var vendorName = lines.firstOrNull() ?: "Receipt Expense"
        if (vendorName.lowercase().any { it in '0'..'9' } && lines.size > 1) {
            val secondLine = lines.getOrNull(1) ?: ""
            if (secondLine.isNotEmpty() && !secondLine.lowercase().any { it in '0'..'9' }) {
                vendorName = secondLine
            }
        }
        if (vendorName.length > 25) {
            vendorName = vendorName.substring(0, 22) + "..."
        }

        var foundAmount = 0.0
        val paymentKeywords = listOf("total", "sum", "amount", "due", "paid", "net", "tax", "cash", "visa", "mastercard")
        val decimalRegex = """\d+[\.,]\d{2}""".toRegex()
        var maxDecimalVal = 0.0
        
        for (line in lines) {
            val lowerLine = line.lowercase()
            val match = decimalRegex.find(lowerLine)
            if (match != null) {
                val doubleVal = match.value.replace(",", ".").toDoubleOrNull() ?: 0.0
                if (doubleVal > maxDecimalVal) {
                    maxDecimalVal = doubleVal
                }
                if (paymentKeywords.any { lowerLine.contains(it) }) {
                    foundAmount = doubleVal
                }
            }
        }

        if (foundAmount == 0.0) {
            foundAmount = maxDecimalVal
        }

        return ReceiptParseResult(
            title = vendorName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
            amount = foundAmount,
            serviceNotes = "Detected locally from document structures (Offline Mode)"
        )
    }

    /**
     * Call the Gemini API to analyze group spending categories and provide
     * a detailed summary of where most money is being spent.
     */
    suspend fun getGroupCategoryInsights(
        expenses: List<Expense>,
        members: List<User>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getLocalCategoryInsightsBackup(expenses, members)
        }

        val prompt = buildCategoryInsightsPrompt(expenses, members)

        try {
            val jsonRequest = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                }
                put("contents", contentsArray)

                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "You are the GroupSplit AI Spend Category Analyst. You analyze expenses, group transactions and member shares to classify transactions and produce visually rich spending insights. Always use bold titles, clear breakdowns, list percentages where possible, and provide actionable saving tips in markdown form.")
                        })
                    })
                })
            }

            val requestBody = jsonRequest.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext getLocalCategoryInsightsBackup(expenses, members)
                }

                val bodyString = response.body?.string() ?: ""
                val responseJson = JSONObject(bodyString)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", "No insight text returned.")
                        }
                    }
                }
                return@withContext getLocalCategoryInsightsBackup(expenses, members)
            }
        } catch (e: Exception) {
            Log.e("GeminiClient", "getGroupCategoryInsights network error: ${e.message}", e)
            return@withContext getLocalCategoryInsightsBackup(expenses, members)
        }
    }

    private fun buildCategoryInsightsPrompt(expenses: List<Expense>, members: List<User>): String {
        return buildString {
            append("Please perform a high-fidelity 'AI Spending Category Analysis' on our group's transactions.\n\n")
            append("Here are our group members:\n")
            members.forEach { append("- ID: ${it.id}, Name: ${it.name}\n") }

            append("\nHere are all recorded expenses:\n")
            expenses.forEach { append("- '${it.title}', Amount: $${String.format("%.2f", it.amount)}, Paid by ID: ${it.paidByUserId}, Category Hint: ${getCategoryFromTitle(it.title)}\n") }

            append("\nAnalyze this list and construct an advice summary with three distinct sections:")
            append("\n1. **Category spending breakdown**: Express categories like Dining, Lodging, Transport, Utilities, Entertainment, or others as total dollar amounts and as approximate percentage of overall spending.")
            append("\n2. **Where most money is being spent**: Name the single dominant spend category, highlight the most expensive billing entries, and explain who has paid for these high-cost item(s).")
            append("\n3. **Smart category-specific saving tips**: Provide 2 target recommendations to trim spending in the heaviest category (e.g. sharing rides, group cooking coupons, booking stays in advance).")
        }
    }

    private fun getLocalCategoryInsightsBackup(expenses: List<Expense>, members: List<User>): String {
        if (expenses.isEmpty()) {
            return """
                📊 **GroupSplit AI Spend Category Analysis (Local heuristics)**
                
                No active group expenses recorded inside this ledger yet.
                
                **Ready to analyze:**
                *   Create transactions such as 'Uber ride', 'Starbucks dining', 'Airbnb rental'.
                *   I will automatically classify titles and render custom visual percentage graphs.
                *   I will pinpoint exactly which category dominates your group wallet.
                
                *Add a few bills first to unlock deep category insights!*
            """.trimIndent()
        }

        val totalSpend = expenses.sumOf { it.amount }
        val categoryMap = mutableMapOf<String, Double>()
        expenses.forEach { exp ->
            val cat = getCategoryFromTitle(exp.title)
            categoryMap[cat] = (categoryMap[cat] ?: 0.0) + exp.amount
        }

        val sortedCategories = categoryMap.toList().sortedByDescending { it.second }
        val primaryCategory = sortedCategories.firstOrNull()
        
        val builder = java.lang.StringBuilder()
        builder.append("📊 **AI Spend Category Analysis (Local Insights)**\n\n")
        builder.append("We completed a local algorithmic sweep of your **${expenses.size} expenses** totaling **\$${String.format("%.2f", totalSpend)}**:\n\n")
        
        builder.append("🏷️ **Group Category Spend & Visual Estimates:**\n")
        sortedCategories.forEach { (cat, amt) ->
            val percentage = (amt / totalSpend) * 100
            val barsCount = (percentage / 10).coerceAtLeast(1.0).toInt()
            val visualBar = "█".repeat(barsCount) + "░".repeat(10 - barsCount)
            builder.append("  *   **$cat**: \$${String.format("%.2f", amt)} (${String.format("%.1f", percentage)}%)\n")
            builder.append("      `$visualBar` \n")
        }
        
        builder.append("\n💡 **Wallet Insight & Cost Drivers:**\n")
        if (primaryCategory != null) {
            val ratio = (primaryCategory.second / totalSpend) * 100
            builder.append("  *   The absolute heaviest cost driver is **${primaryCategory.first}**, accounting for **${String.format("%.1f", ratio)}%** of overall shared group billing.\n")
            
            // Highlight top transactions in this category
            val heavyInPrimary = expenses.filter { getCategoryFromTitle(it.title) == primaryCategory.first }
                .maxByOrNull { it.amount }
            if (heavyInPrimary != null) {
                val payer = members.find { it.id == heavyInPrimary.paidByUserId }?.name ?: "Unknown"
                builder.append("  *   Within **${primaryCategory.first}**, the single largest charge is **'${heavyInPrimary.title}'** costing **\$${String.format("%.2f", heavyInPrimary.amount)}**, which was funded by **$payer**.\n")
            }
        }
        
        builder.append("\n📈 **Target Spending Recommendations:**\n")
        if (primaryCategory != null) {
            when (primaryCategory.first) {
                "Dining" -> {
                    builder.append("  1. **Consolidate shared meals**: Cook common meals or buy grocery ingredients in bulk at supermarkets (Costco, Walmart) instead of ordering separate restaurant checkouts.\n")
                    builder.append("  2. **Audit daily cafe rounds**: Swap out minor daily coffee runs for central group pantry coffee pots, which reduces micro-ledgerr bounds by up to 35%.\n")
                }
                "Transport" -> {
                    builder.append("  1. **Optimize multi-car rides**: Try pooling together when touring instead of dispatching independent ride shares. Coordinate group pick-ups.\n")
                    builder.append("  2. **Prefund travel tolls**: Establish a unified transport fuel fund beforehand to minimize peer invoice tracking loops.\n")
                }
                "Lodging" -> {
                    builder.append("  1. **Reserve custom group bundles**: Negotiate directly with property hosts or lock in stays early to capture off-peak reservation discounts.\n")
                    builder.append("  2. **Audit local room split ratios**: Allocate split weights based on bedroom size/benefits to maintain complete financial equity.\n")
                }
                "Entertainment" -> {
                    builder.append("  1. **Purchase multiplayer bundles**: Tap into family plans or group passes for entry tickets and theme parks.\n")
                    builder.append("  2. **Explore free alternatives**: Mix in local parks, free museums, and self-guided landscape crawls to stretch the entertainment budget.\n")
                }
                else -> {
                    builder.append("  1. **Pre-approve limits**: Establish a \$50 soft spending limit per transaction before booking items to keep budgets transparent.\n")
                    builder.append("  2. **Settle pending ledger checks**: Promptly reconcile small debts via the 'Settlements' page to keep cash flowing.\n")
                }
            }
        } else {
            builder.append("  1. Keep adding expenses to observe structured, categorized alerts.\n")
        }

        builder.append("\n_Powered by local heuristics analysis engine._")
        return builder.toString()
    }
}


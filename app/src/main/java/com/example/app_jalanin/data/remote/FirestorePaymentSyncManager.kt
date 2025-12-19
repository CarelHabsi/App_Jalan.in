package com.example.app_jalanin.data.remote

import android.content.Context
import android.util.Log
import com.example.app_jalanin.data.AppDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Manager untuk sinkronisasi PaymentHistory antara Room dan Firestore
 * Memastikan payment history tetap tersimpan di cloud untuk backup dan cross-device access
 */
object FirestorePaymentSyncManager {
    private const val TAG = "FirestorePaymentSync"
    private const val PAYMENT_HISTORY_COLLECTION = "payment_history"

    /**
     * Sync all unsynced payment history to Firestore
     */
    suspend fun syncUnsyncedPayments(context: Context) = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            val unsyncedPayments = db.paymentHistoryDao().getUnsyncedPayments()

            if (unsyncedPayments.isEmpty()) {
                Log.d(TAG, "✅ No unsynced payments to upload")
                return@withContext
            }

            Log.d(TAG, "🔄 Syncing ${unsyncedPayments.size} payment records to Firestore...")

            val firestore = FirebaseFirestore.getInstance()
            var successCount = 0
            var failedCount = 0

            for (payment in unsyncedPayments) {
                try {
                    val paymentData = hashMapOf(
                        "id" to payment.id,
                        "userId" to payment.userId,
                        "userEmail" to payment.userEmail,
                        "rentalId" to payment.rentalId,
                        "vehicleName" to payment.vehicleName,
                        "amount" to payment.amount,
                        "paymentMethod" to payment.paymentMethod,
                        "paymentType" to payment.paymentType,
                        "ownerEmail" to payment.ownerEmail,
                        "driverEmail" to (payment.driverEmail ?: ""),
                        "ownerIncome" to payment.ownerIncome,
                        "driverIncome" to payment.driverIncome,
                        "senderRole" to (payment.senderRole ?: ""),
                        "receiverRole" to (payment.receiverRole ?: ""),
                        "status" to payment.status,
                        "createdAt" to payment.createdAt,
                        "synced" to true
                    )

                    firestore.collection(PAYMENT_HISTORY_COLLECTION)
                        .document(payment.id.toString())
                        .set(paymentData)
                        .await()

                    // Mark as synced in Room
                    db.paymentHistoryDao().updateSyncStatus(payment.id, true)
                    successCount++

                    Log.d(TAG, "✅ Synced payment: ${payment.id} (${payment.rentalId})")
                } catch (e: Exception) {
                    failedCount++
                    Log.e(TAG, "❌ Failed to sync payment ${payment.id}: ${e.message}")
                }
            }

            Log.d(TAG, "🎉 Sync complete: $successCount/${unsyncedPayments.size} payments synced successfully")
            if (failedCount > 0) {
                Log.w(TAG, "⚠️ $failedCount payments failed to sync (will retry later)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error syncing payments: ${e.message}", e)
        }
    }

    /**
     * Sync single payment history to Firestore
     */
    suspend fun syncSinglePayment(context: Context, paymentId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            val payment = db.paymentHistoryDao().getPaymentById(paymentId)

            if (payment == null) {
                Log.e(TAG, "❌ Payment not found: $paymentId")
                return@withContext false
            }

            val paymentData = hashMapOf(
                "id" to payment.id,
                "userId" to payment.userId,
                "userEmail" to payment.userEmail,
                "rentalId" to payment.rentalId,
                "vehicleName" to payment.vehicleName,
                "amount" to payment.amount,
                "paymentMethod" to payment.paymentMethod,
                "paymentType" to payment.paymentType,
                "ownerEmail" to payment.ownerEmail,
                "driverEmail" to (payment.driverEmail ?: ""),
                "ownerIncome" to payment.ownerIncome,
                "driverIncome" to payment.driverIncome,
                "status" to payment.status,
                "createdAt" to payment.createdAt,
                "synced" to true
            )

            val firestore = FirebaseFirestore.getInstance()
            firestore.collection(PAYMENT_HISTORY_COLLECTION)
                .document(payment.id.toString())
                .set(paymentData)
                .await()

            // Mark as synced
            db.paymentHistoryDao().updateSyncStatus(payment.id, true)

            Log.d(TAG, "✅ Payment $paymentId synced successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to sync payment $paymentId: ${e.message}")
            false
        }
    }

    /**
     * Download payment history from Firestore for a specific user
     */
    suspend fun downloadUserPayments(context: Context, userEmail: String) = withContext(Dispatchers.IO) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val db = AppDatabase.getDatabase(context)

            val snapshot = firestore.collection(PAYMENT_HISTORY_COLLECTION)
                .whereEqualTo("userEmail", userEmail)
                .get()
                .await()

            Log.d(TAG, "📥 Downloading ${snapshot.documents.size} payment records for $userEmail...")

            for (doc in snapshot.documents) {
                try {
                    val paymentId = doc.getLong("id") ?: continue
                    val existingPayment = db.paymentHistoryDao().getPaymentById(paymentId)

                    if (existingPayment == null) {
                        // Get userId from userEmail if userId is 0 or missing
                        var userId = doc.getLong("userId")?.toInt() ?: 0
                        val userEmailFromDoc = doc.getString("userEmail") ?: ""
                        
                        // If userId is 0, try to get it from database
                        if (userId == 0 && userEmailFromDoc.isNotEmpty()) {
                            val user = db.userDao().getUserByEmail(userEmailFromDoc)
                            userId = user?.id ?: 0
                            Log.d(TAG, "🔍 Found userId $userId for email $userEmailFromDoc")
                        }
                        
                        // Create new payment history from Firestore
                        val payment = com.example.app_jalanin.data.local.entity.PaymentHistory(
                            id = paymentId,
                            userId = userId,
                            userEmail = userEmailFromDoc,
                            rentalId = doc.getString("rentalId") ?: "",
                            vehicleName = doc.getString("vehicleName") ?: "",
                            amount = doc.getLong("amount")?.toInt() ?: 0,
                            paymentMethod = doc.getString("paymentMethod") ?: "",
                            paymentType = doc.getString("paymentType") ?: "",
                            ownerEmail = doc.getString("ownerEmail") ?: "",
                            driverEmail = doc.getString("driverEmail")?.takeIf { it.isNotEmpty() },
                            ownerIncome = doc.getLong("ownerIncome")?.toInt() ?: 0,
                            driverIncome = doc.getLong("driverIncome")?.toInt() ?: 0,
                            senderRole = doc.getString("senderRole")?.takeIf { it.isNotEmpty() },
                            receiverRole = doc.getString("receiverRole")?.takeIf { it.isNotEmpty() },
                            status = doc.getString("status") ?: "COMPLETED",
                            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                            synced = true
                        )

                        db.paymentHistoryDao().insert(payment)
                        Log.d(TAG, "✅ Downloaded payment: $paymentId for userEmail: $userEmailFromDoc, userId: $userId")
                    } else {
                        Log.d(TAG, "⏭️ Payment $paymentId already exists locally, skipping")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error processing payment document: ${e.message}", e)
                }
            }

            Log.d(TAG, "✅ Payment download complete")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error downloading payments: ${e.message}", e)
        }
    }

    /**
     * Download payment history from Firestore where driver is the receiver
     * Used for driver dashboard to restore payment data after app data is cleared
     */
    suspend fun downloadDriverPayments(context: Context, driverEmail: String) = withContext(Dispatchers.IO) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val db = AppDatabase.getDatabase(context)

            // Download payments where driverEmail matches and driverIncome > 0
            val snapshot = firestore.collection(PAYMENT_HISTORY_COLLECTION)
                .whereEqualTo("driverEmail", driverEmail)
                .get()
                .await()

            Log.d(TAG, "📥 Downloading ${snapshot.documents.size} driver payment records for $driverEmail...")

            var downloadedCount = 0
            var skippedCount = 0

            for (doc in snapshot.documents) {
                try {
                    val driverIncome = doc.getLong("driverIncome")?.toInt() ?: 0
                    // Only process payments where driver actually received income
                    if (driverIncome <= 0) {
                        skippedCount++
                        continue
                    }

                    val paymentId = doc.getLong("id") ?: continue
                    val existingPayment = db.paymentHistoryDao().getPaymentById(paymentId)

                    if (existingPayment == null) {
                        // Get userId from userEmail if userId is 0 or missing
                        var userId = doc.getLong("userId")?.toInt() ?: 0
                        val userEmailFromDoc = doc.getString("userEmail") ?: ""
                        
                        // If userId is 0, try to get it from database
                        if (userId == 0 && userEmailFromDoc.isNotEmpty()) {
                            val user = db.userDao().getUserByEmail(userEmailFromDoc)
                            userId = user?.id ?: 0
                        }
                        
                        // Create new payment history from Firestore
                        val payment = com.example.app_jalanin.data.local.entity.PaymentHistory(
                            id = paymentId,
                            userId = userId,
                            userEmail = userEmailFromDoc,
                            rentalId = doc.getString("rentalId") ?: "",
                            vehicleName = doc.getString("vehicleName") ?: "",
                            amount = doc.getLong("amount")?.toInt() ?: 0,
                            paymentMethod = doc.getString("paymentMethod") ?: "",
                            paymentType = doc.getString("paymentType") ?: "",
                            ownerEmail = doc.getString("ownerEmail") ?: "",
                            driverEmail = doc.getString("driverEmail")?.takeIf { it.isNotEmpty() },
                            ownerIncome = doc.getLong("ownerIncome")?.toInt() ?: 0,
                            driverIncome = driverIncome,
                            senderRole = doc.getString("senderRole")?.takeIf { it.isNotEmpty() },
                            receiverRole = doc.getString("receiverRole")?.takeIf { it.isNotEmpty() },
                            status = doc.getString("status") ?: "COMPLETED",
                            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                            synced = true
                        )

                        db.paymentHistoryDao().insert(payment)
                        downloadedCount++
                        Log.d(TAG, "✅ Downloaded driver payment: $paymentId (driverIncome: $driverIncome)")
                    } else {
                        skippedCount++
                        Log.d(TAG, "⏭️ Payment $paymentId already exists locally, skipping")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error processing driver payment document: ${e.message}", e)
                }
            }

            Log.d(TAG, "✅ Driver payment download complete: $downloadedCount new, $skippedCount skipped")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error downloading driver payments: ${e.message}", e)
        }
    }
}


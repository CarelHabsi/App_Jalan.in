package com.example.app_jalanin.utils

import com.example.app_jalanin.data.AppDatabase
import com.example.app_jalanin.data.local.entity.ChatChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Helper untuk membuat dan mendapatkan chat channel
 */
object ChatHelper {
    /**
     * Get or create DM channel between two users
     */
    suspend fun getOrCreateDMChannel(
        database: AppDatabase,
        user1: String,
        user2: String
    ): ChatChannel {
        return withContext(Dispatchers.IO) {
            // Sort emails to ensure consistent channel ID
            val sortedEmails = listOf(user1, user2).sorted()
            val channelId = "CHAT_DM_${sortedEmails[0]}_${sortedEmails[1]}"
            
            // Try to get existing channel
            val existing = database.chatChannelDao().getDMChannel(sortedEmails[0], sortedEmails[1])
            if (existing != null) {
                return@withContext existing
            }
            
            // Create new channel
            val newChannel = ChatChannel(
                id = channelId,
                channelType = "DM",
                participant1 = sortedEmails[0],
                participant2 = sortedEmails[1],
                participant3 = null,
                rentalId = null
            )
            database.chatChannelDao().insertChannel(newChannel)
            return@withContext newChannel
        }
    }
    
    /**
     * Get or create group chat channel for rental (Owner, Driver, Passenger)
     */
    suspend fun getOrCreateGroupChannel(
        database: AppDatabase,
        ownerEmail: String,
        driverEmail: String,
        passengerEmail: String,
        rentalId: String
    ): ChatChannel {
        return withContext(Dispatchers.IO) {
            val channelId = "CHAT_GROUP_${rentalId}"
            
            // Try to get existing channel
            val existing = database.chatChannelDao().getGroupChannelByRental(rentalId)
            if (existing != null) {
                return@withContext existing
            }
            
            // Create new group channel
            val newChannel = ChatChannel(
                id = channelId,
                channelType = "GROUP",
                participant1 = ownerEmail,
                participant2 = driverEmail,
                participant3 = passengerEmail,
                rentalId = rentalId
            )
            database.chatChannelDao().insertChannel(newChannel)
            return@withContext newChannel
        }
    }
    
    /**
     * Generate channel ID for DM
     */
    fun generateDMChannelId(user1: String, user2: String): String {
        val sortedEmails = listOf(user1, user2).sorted()
        return "CHAT_DM_${sortedEmails[0]}_${sortedEmails[1]}"
    }
    
    /**
     * Generate channel ID for group chat
     */
    fun generateGroupChannelId(rentalId: String): String {
        return "CHAT_GROUP_${rentalId}"
    }
}

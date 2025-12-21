package com.example.app_jalanin.data.local.dao

import androidx.room.*
import com.example.app_jalanin.data.local.entity.ChatChannel
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatChannelDao {
    
    @Query("SELECT * FROM chat_channels WHERE participant1 = :userEmail OR participant2 = :userEmail OR participant3 = :userEmail ORDER BY lastMessageAt DESC")
    fun getChannelsByUser(userEmail: String): Flow<List<ChatChannel>>
    
    @Query("SELECT * FROM chat_channels ORDER BY lastMessageAt DESC")
    fun getAllChannelsFlow(): Flow<List<ChatChannel>>
    
    @Query("SELECT * FROM chat_channels WHERE id = :channelId")
    suspend fun getChannelById(channelId: String): ChatChannel?
    
    @Query("SELECT * FROM chat_channels WHERE id = :channelId")
    fun getChannelByIdFlow(channelId: String): Flow<ChatChannel?>
    
    @Query("""
        SELECT * FROM chat_channels 
        WHERE channelType = 'DM' 
        AND ((participant1 = :user1 AND participant2 = :user2) OR (participant1 = :user2 AND participant2 = :user1))
        LIMIT 1
    """)
    suspend fun getDMChannel(user1: String, user2: String): ChatChannel?
    
    @Query("""
        SELECT * FROM chat_channels 
        WHERE channelType = 'GROUP' 
        AND rentalId = :rentalId
        LIMIT 1
    """)
    suspend fun getGroupChannelByRental(rentalId: String): ChatChannel?
    
    @Query("""
        SELECT * FROM chat_channels 
        WHERE channelType = 'GROUP' 
        AND participant1 = :userEmail OR participant2 = :userEmail OR participant3 = :userEmail
        ORDER BY lastMessageAt DESC
    """)
    fun getGroupChannelsByUser(userEmail: String): Flow<List<ChatChannel>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: ChatChannel)
    
    @Update
    suspend fun updateChannel(channel: ChatChannel)
    
    @Query("UPDATE chat_channels SET lastMessage = :message, lastMessageAt = :timestamp, updatedAt = :timestamp WHERE id = :channelId")
    suspend fun updateLastMessage(channelId: String, message: String, timestamp: Long)
    
    @Delete
    suspend fun deleteChannel(channel: ChatChannel)
}

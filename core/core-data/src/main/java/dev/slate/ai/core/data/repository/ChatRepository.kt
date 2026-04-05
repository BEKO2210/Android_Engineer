package dev.slate.ai.core.data.repository

import dev.slate.ai.core.database.dao.ConversationDao
import dev.slate.ai.core.database.dao.MessageDao
import dev.slate.ai.core.database.entity.ConversationEntity
import dev.slate.ai.core.database.entity.MessageEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
) {
    fun observeConversations(): Flow<List<ConversationEntity>> {
        return conversationDao.observeAll()
    }

    fun observeMessages(conversationId: String): Flow<List<MessageEntity>> {
        return messageDao.observeMessages(conversationId)
    }

    suspend fun createConversation(modelId: String, title: String = "New chat"): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        conversationDao.insert(
            ConversationEntity(
                id = id,
                title = title,
                modelId = modelId,
                createdAt = now,
                updatedAt = now,
            )
        )
        return id
    }

    suspend fun addUserMessage(conversationId: String, content: String): String {
        val id = UUID.randomUUID().toString()
        messageDao.insert(
            MessageEntity(
                id = id,
                conversationId = conversationId,
                role = "user",
                content = content,
                isComplete = true,
                createdAt = System.currentTimeMillis(),
            )
        )
        updateConversationTimestamp(conversationId)
        return id
    }

    suspend fun addAssistantMessage(conversationId: String, content: String = ""): String {
        val id = UUID.randomUUID().toString()
        messageDao.insert(
            MessageEntity(
                id = id,
                conversationId = conversationId,
                role = "assistant",
                content = content,
                isComplete = false,
                createdAt = System.currentTimeMillis(),
            )
        )
        return id
    }

    suspend fun updateMessageContent(messageId: String, content: String) {
        val msg = messageDao.getById(messageId) ?: return
        messageDao.update(msg.copy(content = content))
    }

    suspend fun markMessageComplete(messageId: String) {
        val msg = messageDao.getById(messageId) ?: return
        messageDao.update(msg.copy(isComplete = true))
    }

    suspend fun deleteMessage(messageId: String) {
        messageDao.delete(messageId)
    }

    suspend fun getLastAssistantMessage(conversationId: String): MessageEntity? {
        val messages = messageDao.getMessages(conversationId)
        return messages.lastOrNull { it.role == "assistant" }
    }

    suspend fun clearConversation(conversationId: String) {
        messageDao.deleteByConversation(conversationId)
    }

    suspend fun deleteConversation(conversationId: String) {
        conversationDao.delete(conversationId)
    }

    suspend fun deleteAllConversations() {
        conversationDao.deleteAll()
    }

    suspend fun updateConversationTitle(conversationId: String, title: String) {
        val conv = conversationDao.getById(conversationId) ?: return
        conversationDao.update(conv.copy(title = title))
    }

    suspend fun getConversation(conversationId: String): ConversationEntity? {
        return conversationDao.getById(conversationId)
    }

    suspend fun getMessages(conversationId: String): List<MessageEntity> {
        return messageDao.getMessages(conversationId)
    }

    private suspend fun updateConversationTimestamp(conversationId: String) {
        val conv = conversationDao.getById(conversationId) ?: return
        conversationDao.update(conv.copy(updatedAt = System.currentTimeMillis()))
    }
}

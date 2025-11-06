package com.android.joinme.repository

import com.android.joinme.model.chat.ChatRepositoryLocal
import com.android.joinme.model.chat.Message
import com.android.joinme.model.chat.MessageType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class ChatRepositoryLocalTest {

    private lateinit var repo: ChatRepositoryLocal
    private lateinit var sampleMessage: Message

    @Before
    fun setup() {
        repo = ChatRepositoryLocal()
        sampleMessage =
            Message(
                id = "1",
                chatId = "chat1",
                senderId = "user1",
                senderName = "Alice",
                content = "Hello, world!",
                timestamp = 1000L,
                type = MessageType.TEXT,
                readBy = emptyList(),
                isPinned = false
            )
    }

    @Test
    fun addAndGetMessage_success() = runTest {
        repo.addMessage(sampleMessage)
        val message = repo.getMessage("1")
        Assert.assertEquals(sampleMessage.content, message.content)
    }

    @Test(expected = Exception::class)
    fun getMessage_notFound_throwsException() = runTest { repo.getMessage("unknown") }

    @Test
    fun editMessage_updatesSuccessfully() = runTest {
        repo.addMessage(sampleMessage)
        val updated = sampleMessage.copy(content = "Updated content")
        repo.editMessage("1", updated)
        val message = repo.getMessage("1")
        Assert.assertEquals("Updated content", message.content)
    }

    @Test
    fun deleteMessage_removesSuccessfully() = runTest {
        repo.addMessage(sampleMessage)
        repo.deleteMessage("1")

        val messages = repo.observeMessages("chat1").first()
        Assert.assertTrue(messages.isEmpty())
    }

    @Test
    fun observeMessages_emitsInitialEmptyList() = runTest {
        val messages = repo.observeMessages("chat1").first()
        Assert.assertTrue(messages.isEmpty())
    }

    @Test
    fun observeMessages_returnsAddedMessages() = runTest {
        repo.addMessage(sampleMessage)

        val messages = repo.observeMessages("chat1").first()
        Assert.assertEquals(1, messages.size)
        Assert.assertEquals("Hello, world!", messages[0].content)
    }

    @Test
    fun observeMessages_filtersCorrectChatId() = runTest {
        val chat1Message = sampleMessage.copy(id = "1", chatId = "chat1")
        val chat2Message =
            sampleMessage.copy(id = "2", chatId = "chat2", content = "Different chat")

        repo.addMessage(chat1Message)
        repo.addMessage(chat2Message)

        val messages = repo.observeMessages("chat1").first()
        Assert.assertEquals(1, messages.size)
        Assert.assertEquals("chat1", messages[0].chatId)
        Assert.assertEquals("Hello, world!", messages[0].content)
    }

    @Test
    fun observeMessages_sortsByTimestamp() = runTest {
        val msg1 = sampleMessage.copy(id = "1", timestamp = 3000L, content = "Third")
        val msg2 = sampleMessage.copy(id = "2", timestamp = 1000L, content = "First")
        val msg3 = sampleMessage.copy(id = "3", timestamp = 2000L, content = "Second")

        repo.addMessage(msg1)
        repo.addMessage(msg2)
        repo.addMessage(msg3)

        val messages = repo.observeMessages("chat1").first()
        Assert.assertEquals(3, messages.size)
        Assert.assertEquals("First", messages[0].content)
        Assert.assertEquals("Second", messages[1].content)
        Assert.assertEquals("Third", messages[2].content)
    }

    @Test
    fun markMessageAsRead_addsUserToReadByList() = runTest {
        repo.addMessage(sampleMessage)
        repo.markMessageAsRead("1", "user2")

        val message = repo.getMessage("1")
        Assert.assertTrue(message.readBy.contains("user2"))
    }

    @Test
    fun markMessageAsRead_doesNotAddDuplicateUser() = runTest {
        val messageWithReader = sampleMessage.copy(readBy = listOf("user2"))
        repo.addMessage(messageWithReader)

        repo.markMessageAsRead("1", "user2")

        val message = repo.getMessage("1")
        Assert.assertEquals(1, message.readBy.size)
        Assert.assertEquals("user2", message.readBy[0])
    }

    @Test
    fun markMessageAsRead_multipleUsers() = runTest {
        repo.addMessage(sampleMessage)
        repo.markMessageAsRead("1", "user2")
        repo.markMessageAsRead("1", "user3")
        repo.markMessageAsRead("1", "user4")

        val message = repo.getMessage("1")
        Assert.assertEquals(3, message.readBy.size)
        Assert.assertTrue(message.readBy.containsAll(listOf("user2", "user3", "user4")))
    }

    @Test(expected = Exception::class)
    fun markMessageAsRead_notFound_throwsException() = runTest {
        repo.markMessageAsRead("unknown", "user1")
    }

    @Test
    fun markMessageAsRead_updatesObservableFlow() = runTest {
        repo.addMessage(sampleMessage)
        repo.markMessageAsRead("1", "user2")

        val messages = repo.observeMessages("chat1").first()
        Assert.assertTrue(messages[0].readBy.contains("user2"))
    }

    @Test
    fun getNewMessageId_incrementsSequentially() {
        val id1 = repo.getNewMessageId()
        val id2 = repo.getNewMessageId()
        val id3 = repo.getNewMessageId()
        Assert.assertEquals((id1.toInt() + 1).toString(), id2)
        Assert.assertEquals((id2.toInt() + 1).toString(), id3)
    }

    @Test
    fun addMultipleMessages_storesAll() = runTest {
        val msg1 = sampleMessage.copy(id = "1")
        val msg2 = sampleMessage.copy(id = "2", content = "Message 2")
        val msg3 = sampleMessage.copy(id = "3", content = "Message 3")
        repo.addMessage(msg1)
        repo.addMessage(msg2)
        repo.addMessage(msg3)

        val messages = repo.observeMessages("chat1").first()
        Assert.assertEquals(3, messages.size)
    }

    @Test
    fun getMessage_preservesAllProperties() = runTest {
        repo.addMessage(sampleMessage)
        val retrieved = repo.getMessage("1")
        Assert.assertEquals(sampleMessage.id, retrieved.id)
        Assert.assertEquals(sampleMessage.chatId, retrieved.chatId)
        Assert.assertEquals(sampleMessage.senderId, retrieved.senderId)
        Assert.assertEquals(sampleMessage.senderName, retrieved.senderName)
        Assert.assertEquals(sampleMessage.content, retrieved.content)
        Assert.assertEquals(sampleMessage.timestamp, retrieved.timestamp)
        Assert.assertEquals(sampleMessage.type, retrieved.type)
        Assert.assertEquals(sampleMessage.readBy, retrieved.readBy)
        Assert.assertEquals(sampleMessage.isPinned, retrieved.isPinned)
    }

    @Test
    fun messageType_systemMessage_handledCorrectly() = runTest {
        val systemMessage =
            sampleMessage.copy(id = "2", type = MessageType.SYSTEM, content = "User joined")
        repo.addMessage(systemMessage)

        val retrieved = repo.getMessage("2")
        Assert.assertEquals(MessageType.SYSTEM, retrieved.type)
    }

    @Test(expected = Exception::class)
    fun editMessage_notFound_throwsException() = runTest {
        val fake = sampleMessage.copy(id = "999")
        repo.editMessage(fake.id, fake)
    }

    @Test(expected = Exception::class)
    fun deleteMessage_notFound_throwsException() = runTest { repo.deleteMessage("nonexistent") }

    @Test
    fun editMessage_updatesObservableFlow() = runTest {
        repo.addMessage(sampleMessage)
        val updated = sampleMessage.copy(content = "Edited content")
        repo.editMessage("1", updated)

        val messages = repo.observeMessages("chat1").first()
        Assert.assertEquals("Edited content", messages[0].content)
    }

    @Test
    fun deleteMessage_updatesObservableFlow() = runTest {
        repo.addMessage(sampleMessage)
        repo.deleteMessage("1")

        val messages = repo.observeMessages("chat1").first()
        Assert.assertTrue(messages.isEmpty())
    }

    @Test
    fun observeMessages_emptyForNonExistentChat() = runTest {
        repo.addMessage(sampleMessage.copy(chatId = "chat1"))

        val messages = repo.observeMessages("nonexistent_chat").first()
        Assert.assertTrue(messages.isEmpty())
    }

    @Test
    fun observeMessages_multipleChatsIndependent() = runTest {
        val chat1Msg = sampleMessage.copy(id = "1", chatId = "chat1", content = "Chat 1")
        val chat2Msg = sampleMessage.copy(id = "2", chatId = "chat2", content = "Chat 2")

        repo.addMessage(chat1Msg)
        repo.addMessage(chat2Msg)

        val chat1Messages = repo.observeMessages("chat1").first()
        val chat2Messages = repo.observeMessages("chat2").first()

        Assert.assertEquals(1, chat1Messages.size)
        Assert.assertEquals(1, chat2Messages.size)
        Assert.assertEquals("Chat 1", chat1Messages[0].content)
        Assert.assertEquals("Chat 2", chat2Messages[0].content)
    }
}
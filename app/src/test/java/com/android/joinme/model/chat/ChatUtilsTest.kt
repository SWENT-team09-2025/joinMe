package com.android.joinme.model.chat

//Implemented with help of Claude AI
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatUtilsTest {

  @Test
  fun `generateDirectMessageId with valid user IDs returns correct format and is deterministic`() {
    // Test correct format
    val result = ChatUtils.generateDirectMessageId("user_alice", "user_bob")
    assertEquals("dm_user_alice_user_bob", result)

    // Test determinism - different order returns same ID
    val result1 = ChatUtils.generateDirectMessageId("user_charlie", "user_alice")
    val result2 = ChatUtils.generateDirectMessageId("user_alice", "user_charlie")
    assertEquals(result1, result2)
    assertEquals("dm_user_alice_user_charlie", result1)

    // Test alphabetical sorting
    val result3 = ChatUtils.generateDirectMessageId("zzz_user", "aaa_user")
    assertEquals("dm_aaa_user_zzz_user", result3)

    // Test with Firebase-like user IDs
    val result4 = ChatUtils.generateDirectMessageId("aBcDeF123456789", "XyZ987654321")
    assertEquals("dm_XyZ987654321_aBcDeF123456789", result4)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `generateDirectMessageId with blank or whitespace userId1 throws exception`() {
    ChatUtils.generateDirectMessageId("", "user_bob")
  }

  @Test(expected = IllegalArgumentException::class)
  fun `generateDirectMessageId with blank or whitespace userId2 throws exception`() {
    ChatUtils.generateDirectMessageId("user_alice", "   ")
  }

  @Test(expected = IllegalArgumentException::class)
  fun `generateDirectMessageId with identical user IDs throws exception`() {
    ChatUtils.generateDirectMessageId("user_alice", "user_alice")
  }
}

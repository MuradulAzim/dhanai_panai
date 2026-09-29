package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CallDirection
import com.example.data.local.entity.CallEntity
import com.example.data.local.entity.CallStatus
import com.example.data.local.entity.MessageDirection
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.MessageStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  private lateinit var database: AppDatabase

  @Before
  fun setup() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Second Number", appName)
  }

  @Test
  fun `insert and retrieve call entity`() = runBlocking {
    val call = CallEntity(
      remoteNumber = "+15551234567",
      direction = CallDirection.OUTGOING,
      status = CallStatus.COMPLETED,
      durationSeconds = 45
    )
    val id = database.callDao().insertCall(call)
    val retrieved = database.callDao().getCallById(id)

    assertNotNull(retrieved)
    assertEquals("+15551234567", retrieved?.remoteNumber)
    assertEquals(CallDirection.OUTGOING, retrieved?.direction)
    assertEquals(45, retrieved?.durationSeconds)
  }

  @Test
  fun `insert and retrieve message entity`() = runBlocking {
    val msg = MessageEntity(
      conversationNumber = "+15559876543",
      senderNumber = "+18005550199",
      receiverNumber = "+15559876543",
      body = "Test SMS from Second Number",
      direction = MessageDirection.OUTGOING,
      status = MessageStatus.SENT,
      isRead = true
    )
    database.messageDao().insertMessage(msg)
    val messages = database.messageDao().getMessagesForConversation("+15559876543").first()

    assertEquals(1, messages.size)
    assertEquals("Test SMS from Second Number", messages[0].body)
  }
}


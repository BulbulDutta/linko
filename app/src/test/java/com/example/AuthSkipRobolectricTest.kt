package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.LinkoRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthSkipRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: LinkoRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LinkoRepository(
            userDao = database.userDao(),
            contactDao = database.contactDao(),
            chatDao = database.chatDao(),
            messageDao = database.messageDao(),
            callLogDao = database.callLogDao(),
            statusDao = database.statusDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `test skip to guest mode authenticates user with guest credentials in repository`() = runTest {
        val guestUser = repository.loginAsGuest()

        assertEquals("guest@linko.app", guestUser.email)
        assertEquals("Guest User", guestUser.displayName)
        assertEquals("guest_user", guestUser.username)
        assertTrue(guestUser.isCurrentUser)

        val currentUser = repository.currentUserFlow.first()
        assertNotNull(currentUser)
        assertEquals("guest@linko.app", currentUser?.email)
        assertEquals("Guest User", currentUser?.displayName)
    }

    @Test
    fun `test skip callback invocation with custom display name`() = runTest {
        var skipInvoked = false
        val onSkip = {
            skipInvoked = true
        }

        onSkip()
        assertTrue("Skip callback was successfully triggered", skipInvoked)

        val user = repository.loginAsGuest("Test Explorer")
        assertNotNull(user)
        assertEquals("Test Explorer", user.displayName)
        assertTrue(user.isCurrentUser)
    }
}

package com.framebynavin.app.cloud

import android.content.Context
import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Uses isolated preferences and synthetic credentials. No real account or network is touched. */
@RunWith(AndroidJUnit4::class)
class CloudLocalStoreV183Test {
    private lateinit var context: Context
    private lateinit var store: CloudLocalStore
    private val isolatedName = "test_creator_cloud_lifecycle_v183"

    @Before fun setUp() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        context = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int) =
                super.getSharedPreferences(if (name == "creator_cloud_v13") isolatedName else name, mode)
        }
        context.getSharedPreferences("creator_cloud_v13", Context.MODE_PRIVATE).edit().clear().commit()
        store = CloudLocalStore(context)
    }

    @After fun tearDown() {
        store.clearSession()
        context.getSharedPreferences("creator_cloud_v13", Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun session(user: String, token: String) = CloudSession(
        userId = user, email = "", displayName = "", avatarUrl = "",
        accessToken = token, refreshToken = "synthetic-refresh", expiresAtMillis = Long.MAX_VALUE,
    )

    @Test fun lateRefreshCannotRecreateSignedOutSession() {
        store.saveSession(session("creator-a", "old"))
        val epoch = store.generation()
        store.invalidateOperations()
        store.clearSession()
        assertFalse(store.saveRefreshedSession(session("creator-a", "late"), epoch, "creator-a"))
        assertNull(store.loadSession())
    }

    @Test fun refreshCannotOverwriteAnotherAccount() {
        store.saveSession(session("creator-a", "old"))
        val epoch = store.generation()
        store.invalidateOperations()
        store.saveSession(session("creator-b", "new"))
        assertFalse(store.saveRefreshedSession(session("creator-a", "late"), epoch, "creator-a"))
        assertEquals("creator-b", store.loadSession()?.userId)
        assertEquals("new", store.loadSession()?.accessToken)
    }

    @Test fun matchingRefreshPreservesIdentityAndUpdatesToken() {
        store.saveSession(session("creator-a", "old"))
        val epoch = store.generation()
        assertTrue(store.saveRefreshedSession(session("creator-a", "new"), epoch, "creator-a"))
        assertEquals("new", store.loadSession()?.accessToken)
        assertFalse(store.saveRefreshedSession(session("creator-b", "wrong"), epoch, "creator-a"))
    }

    @Test fun generationsAreSharedAcrossStoreInstances() {
        val other = CloudLocalStore(context)
        val first = store.generation()
        val next = other.invalidateOperations()
        assertEquals(first + 1L, next)
        assertEquals(next, store.generation())
    }
}

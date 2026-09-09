package com.framebynavin.app.cloud

import android.content.Context
import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Isolated preferences and synthetic identities only. No network or real account is touched. */
@RunWith(AndroidJUnit4::class)
class CloudDeletionJournalV183Test {
    private lateinit var context: Context
    private lateinit var store: CloudLocalStore
    private val isolatedName = "test_creator_cloud_deletion_v183"
    private val owner = UUID.randomUUID().toString()
    private val other = UUID.randomUUID().toString()

    @Before fun setUp() {
        HardeningTestEnvironment.requireCiEmulator()
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        context = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int) =
                super.getSharedPreferences(if (name == "creator_cloud_v13") isolatedName else name, mode)
        }
        check(context.getSharedPreferences("creator_cloud_v13", Context.MODE_PRIVATE).edit().clear().commit())
        store = CloudLocalStore(context)
    }

    @After fun tearDown() {
        check(context.getSharedPreferences("creator_cloud_v13", Context.MODE_PRIVATE).edit().clear().commit())
    }

    private fun session(user: String) = CloudSession(user, "", "", "", "synthetic-access", "synthetic-refresh", Long.MAX_VALUE)

    @Test fun pendingDeletionSurvivesSignOutAndReauthentication() {
        store.saveSession(session(owner))
        store.begin(owner)
        assertTrue(store.settings().deletionPending)
        val otherStore = CloudLocalStore(context)
        assertEquals(owner, otherStore.pendingUserId())
        assertThrows(CloudDeletionPending::class.java) { otherStore.requireWritable(owner) }
        assertTrue(otherStore.approveUser(owner)) // approval alone never bypasses the journal
        assertTrue(otherStore.needsReconciliation())
        otherStore.clearSession()
        assertEquals(owner, store.pendingUserId())
        assertFalse(store.settings().deletionPending)
        store.saveSession(session(owner))
        assertTrue(store.settings().deletionPending)
        store.clear(owner)
        assertNull(otherStore.pendingUserId())
        assertTrue(otherStore.needsReconciliation())
    }

    @Test fun ownerCheckPreventsCrossAccountDeletionAndMarkerLoss() {
        store.begin(owner)
        assertThrows(CloudDeletionPending::class.java) { store.begin(other) }
        assertThrows(IllegalStateException::class.java) { store.clear(other) }
        store.saveSession(session(other))
        assertFalse(store.settings().deletionPending)
        store.requireWritable(other)
        assertEquals(owner, store.pendingUserId())
        assertThrows(CloudDeletionPending::class.java) { store.begin(other) }
        store.saveSession(session(owner))
        assertTrue(store.settings().deletionPending)
        store.abandonDeletion(owner)
        assertNull(store.pendingUserId())
        assertTrue(store.needsReconciliation())
    }

    @Test fun portableBackupDoesNotContainDeletionJournal() = kotlinx.coroutines.runBlocking {
        // The isolation wrapper makes the journal synthetic; no real backup data is changed.
        store.begin(owner)
        val raw = com.framebynavin.app.data.CreatorBackupManager(context).createBackup()
        assertFalse(raw.contains("pending_cloud_deletion_v183"))
        assertEquals(owner, store.pendingUserId())
    }
}

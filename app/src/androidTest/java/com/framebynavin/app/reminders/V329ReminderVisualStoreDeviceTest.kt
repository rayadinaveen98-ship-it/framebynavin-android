package com.framebynavin.app.reminders

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V329ReminderVisualStoreDeviceTest {
    @Test fun everyDomainAndChannelKeepsItsOwnVisualChoicesAfterReload() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = V329ReminderVisualStore(context)
        val keys = V329ReminderDomain.entries.flatMap { domain ->
            V329ReminderChannel.entries.map { channel -> domain to channel }
        }
        val backups = keys.associateWith { (domain, channel) -> store.get(domain, channel) }
        try {
            val styles = V329ReminderWorld.entries
            keys.forEachIndexed { i, (domain, channel) ->
                store.set(domain, channel,
                    V329ReminderAppearance(styles[i], .2f + i * .1f, .4f, .55f))
            }
            val reloaded = V329ReminderVisualStore(context)
            keys.forEachIndexed { i, (domain, channel) ->
                assertEquals(styles[i], reloaded.get(domain, channel).world)
                assertEquals(.2f + i * .1f, reloaded.get(domain, channel).intensity)
            }
        } finally {
            backups.forEach { (key, config) -> store.set(key.first, key.second, config) }
        }
    }
}

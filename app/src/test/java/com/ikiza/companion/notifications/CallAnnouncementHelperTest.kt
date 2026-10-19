package com.ikiza.companion.notifications

import org.junit.Assert.assertEquals
import org.junit.Test

class CallAnnouncementHelperTest {

    @Test
    fun testCellularCallWithCallerName() {
        val announcement = CallAnnouncementHelper.formatCallAnnouncement("Mom")
        assertEquals("Call from Mom.", announcement)
    }

    @Test
    fun testCellularCallWithoutCaller() {
        val nullAnnouncement = CallAnnouncementHelper.formatCallAnnouncement(null)
        assertEquals("Incoming call.", nullAnnouncement)

        val blankAnnouncement = CallAnnouncementHelper.formatCallAnnouncement("   ")
        assertEquals("Incoming call.", blankAnnouncement)
    }

    @Test
    fun testVoipCallWithCaller() {
        val announcement = CallAnnouncementHelper.formatVoipCallAnnouncement("WhatsApp", "David")
        assertEquals("WhatsApp call from David.", announcement)
    }

    @Test
    fun testVoipCallWithoutCaller() {
        val announcement = CallAnnouncementHelper.formatVoipCallAnnouncement("Telegram", null)
        assertEquals("Incoming Telegram call.", announcement)
    }
}

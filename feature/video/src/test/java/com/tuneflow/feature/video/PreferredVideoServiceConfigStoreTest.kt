package com.tuneflow.feature.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PreferredVideoServiceConfigStoreTest {
    @Test
    fun `normalizes LAN address without scheme`() {
        assertEquals(
            "http://192.168.1.10:8090",
            normalizePreferredVideoServiceUrl("192.168.1.10:8090/"),
        )
    }

    @Test
    fun `keeps https path for reverse proxy`() {
        assertEquals(
            "https://media.example.test/tuneflow-video",
            normalizePreferredVideoServiceUrl("https://media.example.test/tuneflow-video/"),
        )
    }

    @Test
    fun `blank value disables service`() {
        assertEquals("", normalizePreferredVideoServiceUrl("  "))
    }

    @Test
    fun `rejects unsupported or credentialed URLs`() {
        assertNull(normalizePreferredVideoServiceUrl("ftp://192.168.1.10:8090"))
        assertNull(normalizePreferredVideoServiceUrl("http://user:password@192.168.1.10:8090"))
        assertNull(normalizePreferredVideoServiceUrl("http://192.168.1.10:8090?token=secret"))
    }
}

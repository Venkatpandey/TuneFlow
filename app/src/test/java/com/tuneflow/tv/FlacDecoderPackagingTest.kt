package com.tuneflow.tv

import org.junit.Assert.assertEquals
import org.junit.Test

class FlacDecoderPackagingTest {
    @Test
    fun `software flac renderer is bundled in app`() {
        val rendererClass = Class.forName("androidx.media3.decoder.flac.LibflacAudioRenderer")

        assertEquals("androidx.media3.decoder.flac.LibflacAudioRenderer", rendererClass.name)
    }
}

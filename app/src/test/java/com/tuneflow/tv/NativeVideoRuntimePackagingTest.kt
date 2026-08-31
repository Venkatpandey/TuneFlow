package com.tuneflow.tv

import org.junit.Assert.assertEquals
import org.junit.Test

class NativeVideoRuntimePackagingTest {
    @Test
    fun `SmartTube WebViewFeature dependency is bundled in app`() {
        val featureClass = Class.forName("androidx.webkit.WebViewFeature")

        assertEquals("androidx.webkit.WebViewFeature", featureClass.name)
    }
}

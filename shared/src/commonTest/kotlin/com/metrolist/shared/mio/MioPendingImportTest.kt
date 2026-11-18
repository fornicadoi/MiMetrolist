package com.metrolist.shared.mio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MioPendingImportTest {
    @Test
    fun findsSpotifyLinkInsideSharedText() {
        assertEquals(
            "https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M?si=abc",
            MioPendingImport.linkIn("Escucha esta lista https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M?si=abc"),
        )
    }

    @Test
    fun findsAppleMusicLink() {
        val link = "https://music.apple.com/us/playlist/todays-hits/pl.f4d106fed2bd41149aaacabb233eb5eb"
        assertEquals(link, MioPendingImport.linkIn(link))
    }

    @Test
    fun ignoresYouTubeLinksAndPlainSearches() {
        assertNull(MioPendingImport.linkIn("https://music.youtube.com/playlist?list=PL1234567890"))
        assertNull(MioPendingImport.linkIn("bad bunny"))
    }

    @Test
    fun consumeReturnsLinkOnce() {
        MioPendingImport.url = "https://open.spotify.com/playlist/x"
        assertEquals("https://open.spotify.com/playlist/x", MioPendingImport.consume())
        assertNull(MioPendingImport.consume())
    }
}

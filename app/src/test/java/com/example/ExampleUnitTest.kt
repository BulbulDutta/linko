package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testSecretMessageCoder_preservesYouTubeUrlsWithoutMangling() {
    val messageWithUrl = "Watch this video https://www.youtube.com/watch?v=dQw4w9WgXcQ it is great"
    val encoded = com.example.util.SecretMessageCoder.encodeMessage(messageWithUrl)

    // The URL itself should remain plain text inside the encoded payload
    assertTrue(encoded.contains("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))

    val decoded = com.example.util.SecretMessageCoder.decodeMessage(encoded)
    assertEquals(messageWithUrl, decoded)
  }

  @Test
  fun testSecretMessageCoder_preservesMultipleUrlsAndPunctuation() {
    val input = "Check https://youtu.be/xyz and www.google.com! Enjoy."
    val encoded = com.example.util.SecretMessageCoder.encodeMessage(input)
    val decoded = com.example.util.SecretMessageCoder.decodeMessage(encoded)
    assertEquals(input, decoded)
  }

  @Test
  fun testSecretMessageCoder_slashesInUrlDoNotConflictWithCipher() {
    // In cipher, 'B' is "//" and 'A' is "/."
    val input = "https://example.com/A/B/test"
    val encoded = com.example.util.SecretMessageCoder.encodeMessage(input)
    val decoded = com.example.util.SecretMessageCoder.decodeMessage(encoded)
    assertEquals(input, decoded)
  }

  @Test
  fun testWebRtcConfig_stunTurnAndHdVideoConstants() {
    assertTrue(
      com.example.webrtc.LinkoWebRtcClient.DEFAULT_GOOGLE_STUN_URLS.contains(
        "stun:stun.l.google.com:19302"
      )
    )
    assertEquals(1280, com.example.webrtc.LinkoWebRtcClient.HD_720P_WIDTH)
    assertEquals(720, com.example.webrtc.LinkoWebRtcClient.HD_720P_HEIGHT)
    assertEquals(1920, com.example.webrtc.LinkoWebRtcClient.FHD_1080P_WIDTH)
    assertEquals(1080, com.example.webrtc.LinkoWebRtcClient.FHD_1080P_HEIGHT)
    assertEquals(30, com.example.webrtc.LinkoWebRtcClient.TARGET_FPS)

    val turnConfig = com.example.webrtc.LinkoWebRtcClient.TurnServerConfig(
      urls = listOf("turn:turn.example.com:3478?transport=udp"),
      username = "linko_user",
      credential = "secret_token"
    )
    assertEquals("linko_user", turnConfig.username)
  }
}

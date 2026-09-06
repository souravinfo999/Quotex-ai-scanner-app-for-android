package com.example
 
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.PredictionResult
import com.example.ui.components.PredictionResultCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val sampleResult = PredictionResult(
        prediction = "UP",
        confidence = 88,
        primarySignal = "Bullish Price Action Reaction at Support Zone",
        confirmations = listOf("Long lower wick indicates aggressive buyer absorption", "Reaction off horizontal support line"),
        candlePatternFound = "Hammer / Bullish Pin Bar",
        srZone = "Support Zone",
        trend = "Bullish",
        advice = "ENTER NOW (CALL / UP)"
    )
    composeTestRule.setContent {
      MyApplicationTheme {
        PredictionResultCard(result = sampleResult)
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/prediction_card.png")
  }
}


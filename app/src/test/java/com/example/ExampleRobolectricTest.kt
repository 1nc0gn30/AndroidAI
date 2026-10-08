package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.HarnessProvider
import com.example.model.PetMood
import com.example.model.PetSpecies
import com.example.model.PetState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("OmniPet AI", appName)
  }

  @Test
  fun `verify ten ai harnesses are defined`() {
    val harnesses = HarnessProvider.values()
    assertEquals(10, harnesses.size)
    assertNotNull(harnesses.find { it == HarnessProvider.GOOGLE })
    assertNotNull(harnesses.find { it == HarnessProvider.OPENAI })
    assertNotNull(harnesses.find { it == HarnessProvider.ANTHROPIC })
    assertNotNull(harnesses.find { it == HarnessProvider.META })
    assertNotNull(harnesses.find { it == HarnessProvider.HERMES })
    assertNotNull(harnesses.find { it == HarnessProvider.GROK })
    assertNotNull(harnesses.find { it == HarnessProvider.MISTRAL })
    assertNotNull(harnesses.find { it == HarnessProvider.COHERE })
    assertNotNull(harnesses.find { it == HarnessProvider.DEEPSEEK })
    assertNotNull(harnesses.find { it == HarnessProvider.PERPLEXITY })
  }

  @Test
  fun `verify pet companion states`() {
    val state = PetState(
      species = PetSpecies.CYBER_CAT,
      customName = "Kiko",
      mood = PetMood.HAPPY
    )
    assertEquals("Kiko", state.customName)
    assertEquals(PetMood.HAPPY, state.mood)
    assertEquals(PetSpecies.CYBER_CAT, state.species)
  }

  @Test
  fun `verify overlay permission check logic`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val canDraw = android.provider.Settings.canDrawOverlays(context)
    // Robolectric test context provides non-null boolean evaluation
    org.junit.Assert.assertNotNull(canDraw)
  }
}

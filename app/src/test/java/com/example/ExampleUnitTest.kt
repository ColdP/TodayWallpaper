package com.example

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.lang.reflect.Modifier

@RunWith(RobolectricTestRunner::class)
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testDummySubclassCompiles() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    
    // Check Config constructors
    val configClass = Class.forName("com.qmdeve.liquidglass.Config")
    val sb = StringBuilder()
    sb.append("Config constructors:\n")
    for (constructor in configClass.declaredConstructors) {
      sb.append("${Modifier.toString(constructor.modifiers)} ${constructor.name}(${constructor.parameterTypes.joinToString { it.name }})\n")
    }
    
    val config = configClass.getDeclaredConstructor().newInstance() as com.qmdeve.liquidglass.Config
    
    val dummyGlass = object : com.qmdeve.liquidglass.LiquidGlass(context, config) {
      override fun updateParameters() {
        System.out.println("DummyGlass.updateParameters called")
      }
    }
    
    dummyGlass.updateParameters()
    
    File("config_info.txt").writeText(sb.toString())
  }
}

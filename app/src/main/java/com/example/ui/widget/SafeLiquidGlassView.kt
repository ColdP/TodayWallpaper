package btm.m.todaywallpaper.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import com.qmdeve.liquidglass.widget.LiquidGlassView
import java.lang.reflect.Field

class SafeLiquidGlassView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LiquidGlassView(context, attrs, defStyleAttr) {

    private val glassField: Field? by lazy {
        try {
            val field = LiquidGlassView::class.java.getDeclaredField("glass")
            field.isAccessible = true
            field
        } catch (e: Exception) {
            Log.e("SafeLiquidGlassView", "Failed to access private glass field: ${e.message}")
            null
        }
    }

    private val dummyGlass: com.qmdeve.liquidglass.LiquidGlass by lazy {
        try {
            val config = com.qmdeve.liquidglass.Config()
            object : com.qmdeve.liquidglass.LiquidGlass(context, config) {
                override fun updateParameters() {
                    Log.d("SafeLiquidGlassView", "DummyLiquidGlass.updateParameters called (NPE prevented!)")
                }
            }
        } catch (t: Throwable) {
            Log.e("SafeLiquidGlassView", "Failed to construct dummy Glass", t)
            throw t
        }
    }

    init {
        injectDummyGlassIfNull()
    }

    private fun injectDummyGlassIfNull() {
        try {
            val currentField = glassField
            if (currentField != null) {
                val currentVal = currentField.get(this)
                if (currentVal == null) {
                    currentField.set(this, dummyGlass)
                }
            }
        } catch (t: Throwable) {
            Log.e("SafeLiquidGlassView", "Failed to inject dummy glass: ${t.message}")
        }
    }

    private fun removeDummyGlassIfPresent() {
        try {
            val currentField = glassField
            if (currentField != null) {
                val currentVal = currentField.get(this)
                if (currentVal === dummyGlass) {
                    currentField.set(this, null)
                }
            }
        } catch (t: Throwable) {
            Log.e("SafeLiquidGlassView", "Failed to clear dummy glass: ${t.message}")
        }
    }

    override fun onAttachedToWindow() {
        removeDummyGlassIfPresent()
        try {
            super.onAttachedToWindow()
        } catch (t: Throwable) {
            Log.e("SafeLiquidGlassView", "Caught exception during onAttachedToWindow: ${t.message}")
        }
    }

    override fun onDetachedFromWindow() {
        try {
            super.onDetachedFromWindow()
        } catch (t: Throwable) {
            Log.e("SafeLiquidGlassView", "Caught exception during onDetachedFromWindow super-call: ${t.message}")
        } finally {
            injectDummyGlassIfNull()
        }
    }
}

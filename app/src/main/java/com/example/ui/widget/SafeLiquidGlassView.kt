package btm.m.todaywallpaper.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.LinearGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.util.Log
import android.graphics.drawable.Drawable
import com.qmdeve.liquidglass.widget.LiquidGlassView
import java.lang.reflect.Field

/**
 * Draws the selected navigation item on the same Canvas as the glass view.
 *
 * The navigation controls themselves live in a ComposeView above this view. A
 * BlendMode used in that separate view only blends against its own transparent
 * buffer, not against the glass/wallpaper below it. Keeping this drawable as
 * the glass view's foreground makes DARKEN use the already-rendered glass as
 * its destination.
 */
private class NavigationHighlightDrawable(
    private val density: Float
) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var selectedIndex = 0
    private var highlightColor = android.graphics.Color.WHITE
    private var highlightAlpha = 190
    private var borderColor = android.graphics.Color.TRANSPARENT
    private var borderWidth = 0f
    private var rowLeft = 0f
    private var rowTop = 0f
    private var rowWidth = 0f
    private var rowHeight = 0f
    private var highlightScale = 1f
    private var highlightVisible = true
    private var backgroundDarkenColor = android.graphics.Color.BLACK
    private var backgroundDarkenAlpha = 0

    fun setSelection(index: Int) {
        val newIndex = index.coerceIn(0, 2)
        if (selectedIndex != newIndex) {
            selectedIndex = newIndex
            invalidateSelf()
        }
    }

    fun setStyle(
        highlightColor: Int,
        borderColor: Int,
        borderWidth: Float
    ) {
        this.highlightColor = highlightColor
        this.borderColor = borderColor
        this.borderWidth = borderWidth
        invalidateSelf()
    }

    fun setRowGeometry(left: Float, top: Float, width: Float, height: Float) {
        rowLeft = left
        rowTop = top
        rowWidth = width
        rowHeight = height
        invalidateSelf()
    }

    fun setHighlightScale(scale: Float) {
        val newScale = scale.coerceIn(1f, 1.32f)
        if (highlightScale != newScale) {
            highlightScale = newScale
            invalidateSelf()
        }
    }

    fun setHighlightVisible(visible: Boolean) {
        if (highlightVisible != visible) {
            highlightVisible = visible
            invalidateSelf()
        }
    }

    fun setBackgroundDarken(color: Int, alpha: Int) {
        backgroundDarkenColor = color
        backgroundDarkenAlpha = alpha.coerceIn(0, 255)
        invalidateSelf()
    }

    override fun draw(canvas: Canvas) {
        val width = bounds.width().toFloat()
        val height = bounds.height().toFloat()
        if (width <= 0f || height <= 0f) return

        // This foreground is drawn by the native glass view, directly over the
        // already-rendered liquid material. A Compose BlendMode would only see
        // its own transparent layer here, while this DARKEN pass correctly
        // blends with the sampled wallpaper/glass pixels underneath.
        if (backgroundDarkenAlpha > 0) {
            rect.set(bounds)
            paint.style = Paint.Style.FILL
            paint.color = backgroundDarkenColor
            paint.alpha = backgroundDarkenAlpha
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DARKEN)
            canvas.drawRoundRect(rect, rect.height() / 2f, rect.height() / 2f, paint)
            paint.xfermode = null
        }

        if (!highlightVisible) return

        val outerHorizontalPadding = 5f * density
        val actualRowLeft = if (rowWidth > 0f) rowLeft else 0f
        val actualRowTop = if (rowHeight > 0f) rowTop else 0f
        val actualRowWidth = if (rowWidth > 0f) rowWidth else width
        val actualRowHeight = if (rowHeight > 0f) rowHeight else 72f * density
        val capsuleHeight = 62f * density
        val itemWidth = (actualRowWidth - outerHorizontalPadding * 2f) / 3f
        // Each highlight occupies exactly one destination column. This makes
        // the outer left/right gaps 5dp, identical to the 5dp top/bottom gaps.
        val capsuleWidth = itemWidth
        val unscaledLeft = actualRowLeft + outerHorizontalPadding + selectedIndex * itemWidth
        // Keep the native background on the same deterministic three-column
        // grid as Compose. In particular, do not accept asynchronous bounds
        // callbacks from each tab: while dragging, a stale callback from the
        // previously selected tab could otherwise overwrite the new position.
        val unscaledTop = actualRowTop + (actualRowHeight - capsuleHeight) / 2f
        val centerX = unscaledLeft + capsuleWidth / 2f
        val centerY = unscaledTop + capsuleHeight / 2f
        val scaledWidth = capsuleWidth * highlightScale
        val scaledHeight = capsuleHeight * highlightScale
        rect.set(
            centerX - scaledWidth / 2f,
            centerY - scaledHeight / 2f,
            centerX + scaledWidth / 2f,
            centerY + scaledHeight / 2f
        )

        // First add a translucent normal composite. DARKEN alone can be
        // invisible on some hardware paths when the foreground is rendered as
        // a separate Drawable layer; this pass guarantees the material has a
        // visible, non-opaque fill while the following pass still reacts to
        // the pixels underneath.
        paint.style = Paint.Style.FILL
        paint.color = highlightColor
        paint.alpha = (highlightAlpha * 0.50f).toInt()
        paint.xfermode = null
        canvas.drawRoundRect(rect, rect.height() / 2f, rect.height() / 2f, paint)

        paint.alpha = (highlightAlpha * 0.30f).toInt()
        // PorterDuff.DARKEN is available from the app's minSdk and is the
        // hardware-compatible equivalent needed here for the native Canvas.
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DARKEN)
        canvas.drawRoundRect(rect, rect.height() / 2f, rect.height() / 2f, paint)
        paint.xfermode = null

        if (borderWidth > 0f && android.graphics.Color.alpha(borderColor) > 0) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = borderWidth
            paint.color = borderColor
            paint.alpha = android.graphics.Color.alpha(borderColor)
            val halfStroke = borderWidth / 2f
            rect.inset(halfStroke, halfStroke)
            canvas.drawRoundRect(
                rect,
                rect.height() / 2f - halfStroke,
                rect.height() / 2f - halfStroke,
                paint
            )
        }
        paint.style = Paint.Style.FILL
    }

    override fun setAlpha(alpha: Int) {
        highlightAlpha = (highlightAlpha * alpha / 255f).toInt()
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
}

/** Draws the detail action deck frame on the native glass layer. */
private class DetailGlassFrameDrawable : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var cornerRadius = 0f
    private var strokeWidth = 0f
    private var startColor = android.graphics.Color.TRANSPARENT
    private var endColor = android.graphics.Color.TRANSPARENT

    fun setStyle(cornerRadius: Float, strokeWidth: Float, startColor: Int, endColor: Int) {
        this.cornerRadius = cornerRadius
        this.strokeWidth = strokeWidth
        this.startColor = startColor
        this.endColor = endColor
        invalidateSelf()
    }

    override fun draw(canvas: Canvas) {
        if (strokeWidth <= 0f || bounds.isEmpty) return
        rect.set(bounds)
        val halfStroke = strokeWidth / 2f
        rect.inset(halfStroke, halfStroke)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth
        paint.shader = LinearGradient(
            rect.left,
            rect.top,
            rect.right,
            rect.bottom,
            startColor,
            endColor,
            Shader.TileMode.CLAMP
        )
        val radius = (cornerRadius - halfStroke).coerceAtLeast(0f)
        canvas.drawRoundRect(rect, radius, radius, paint)
        paint.shader = null
        paint.style = Paint.Style.FILL
    }

    override fun setAlpha(alpha: Int) = Unit

    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) = Unit

    override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
}

class SafeLiquidGlassView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LiquidGlassView(context, attrs, defStyleAttr) {

    private val navigationHighlight = NavigationHighlightDrawable(resources.displayMetrics.density)
    private val detailFrame = DetailGlassFrameDrawable()

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

    private val ensureGlassMethod by lazy {
        try {
            LiquidGlassView::class.java.getDeclaredMethod("ensureGlass").apply {
                isAccessible = true
            }
        } catch (e: Exception) {
            Log.e("SafeLiquidGlassView", "Failed to access ensureGlass: ${e.message}")
            null
        }
    }

    private val removeGlassMethod by lazy {
        try {
            LiquidGlassView::class.java.getDeclaredMethod("removeGlass").apply {
                isAccessible = true
            }
        } catch (e: Exception) {
            Log.e("SafeLiquidGlassView", "Failed to access removeGlass: ${e.message}")
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
        // Inject dummy immediately so that even during construction,
        // any code that accesses the glass field won't crash.
        injectDummyGlassIfNull()
    }

    /**
     * Ensures the private "glass" field is never null by injecting a no-op dummy
     * if the real glass hasn't been initialized yet.
     */
    private fun injectDummyGlassIfNull() {
        try {
            val field = glassField ?: return
            val currentVal = field.get(this)
            if (currentVal == null) {
                field.set(this, dummyGlass)
                Log.d("SafeLiquidGlassView", "Dummy glass injected (was null)")
            }
        } catch (t: Throwable) {
            Log.e("SafeLiquidGlassView", "Failed to inject dummy glass: ${t.message}")
        }
    }

    override fun onAttachedToWindow() {
        // NEVER remove dummy glass — always ensure it's present BEFORE super call.
        // The library's super.onAttachedToWindow() may schedule a handler callback
        // that calls glass.updateParameters(). If we remove the dummy first, there's
        // a race condition where the handler fires between remove and re-inject.
        injectDummyGlassIfNull()
        try {
            super.onAttachedToWindow()
        } catch (t: Throwable) {
            Log.e("SafeLiquidGlassView", "Caught exception during onAttachedToWindow: ${t.message}")
        }
        // Re-inject after super in case the library's initialization cleared the field.
        injectDummyGlassIfNull()
    }

    override fun onDetachedFromWindow() {
        try {
            super.onDetachedFromWindow()
        } catch (t: Throwable) {
            Log.e("SafeLiquidGlassView", "Caught exception during onDetachedFromWindow super-call: ${t.message}")
        } finally {
            // Ensure dummy is present after detachment too, in case the view is re-attached
            // or further operations are scheduled.
            injectDummyGlassIfNull()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (w == oldw && h == oldh) return
        if (w <= 0 || h <= 0) return

        // Upstream removes its child and clears `glass` before a delayed
        // ensureGlass() rebuild. Configuration calls posted just before that
        // removal can otherwise dereference null. Keep a guard in place while
        // those already-queued callbacks drain, then create the real child in
        // a later callback. This preserves the normal renderer lifecycle.
        val maxPx = h / 2f
        try {
            val cornerField = LiquidGlassView::class.java
                .getDeclaredField("cornerRadius")
                .apply { isAccessible = true }
            val currentCorner = cornerField.getFloat(this)
            if (currentCorner > maxPx) cornerField.setFloat(this, maxPx)
            removeGlassMethod?.invoke(this)
            injectDummyGlassIfNull()
            post {
                try {
                    glassField?.set(this, null)
                    ensureGlassMethod?.invoke(this)
                } catch (t: Throwable) {
                    Log.e("SafeLiquidGlassView", "Failed to rebuild real glass", t)
                    injectDummyGlassIfNull()
                }
            }
        } catch (t: Throwable) {
            Log.e("SafeLiquidGlassView", "Failed to handle size change", t)
            injectDummyGlassIfNull()
        }
    }

    /**
     * Public hook: call this before any deferred (posted/handler) operation
     * to guarantee the glass field isn't null when the callback fires.
     */
    fun ensureGlassNotNull() {
        injectDummyGlassIfNull()
    }

    /**
     * Replaces the construction-time dummy with the library's real rendering
     * child. The upstream view's ensureGlass() exits whenever `glass != null`;
     * our crash-prevention dummy therefore also prevented its Config and shader
     * from ever being created for views that started at 0 x 0. This synchronous
     * hand-off is used once real bounds and a sampling source are available.
     */
    fun initializeRealGlass(source: android.view.ViewGroup): Boolean {
        return try {
            val field = glassField ?: return false
            val current = field.get(this)
            if (current != null && current !== dummyGlass) {
                super.bind(source)
                return true
            }
            // bind() stores customSource. With a null glass it deliberately does
            // not initialize anything until ensureGlass creates the real child.
            field.set(this, null)
            super.bind(source)
            val ensureMethod = ensureGlassMethod ?: return false
            // Method.invoke() returns null for Java void methods even when the
            // invocation succeeds, so success must be checked through `glass`.
            ensureMethod.invoke(this)
            val realGlass = field.get(this)
            val initialized = realGlass != null && realGlass !== dummyGlass
            if (!initialized) injectDummyGlassIfNull()
            initialized
        } catch (t: Throwable) {
            Log.e("SafeLiquidGlassView", "Failed to initialize real glass", t)
            injectDummyGlassIfNull()
            false
        }
    }

    /**
     * Installs the navigation foreground once and updates its visual state.
     * The foreground is deliberately drawn by this native view so its blend
     * operation can sample the glass content underneath the Compose controls.
     */
    fun configureNavigationHighlight(
        selectedIndex: Int,
        highlightColor: Int,
        borderColor: Int,
        borderWidth: Float,
        scale: Float = 1f
    ) {
        navigationHighlight.setSelection(selectedIndex)
        navigationHighlight.setStyle(highlightColor, borderColor, borderWidth)
        navigationHighlight.setHighlightScale(scale)
        if (foreground !== navigationHighlight) foreground = navigationHighlight
        invalidate()
    }

    /** Adds a DARKEN blend overlay across the whole native navigation material. */
    fun configureNavigationBackgroundDarken(color: Int, alpha: Int) {
        navigationHighlight.setBackgroundDarken(color, alpha)
        if (foreground !== navigationHighlight) foreground = navigationHighlight
        invalidate()
    }

    fun setNavigationRowGeometry(left: Float, top: Float, width: Float, height: Float) {
        navigationHighlight.setRowGeometry(left, top, width, height)
        invalidate()
    }

    fun setNavigationHighlightScale(scale: Float) {
        navigationHighlight.setHighlightScale(scale)
        invalidate()
    }

    fun setNavigationHighlightVisible(visible: Boolean) {
        navigationHighlight.setHighlightVisible(visible)
        invalidate()
    }

    /**
     * Keeps the detail action deck border in the same native layer as the glass,
     * so it inherits liquid-glass distortion and native view transforms.
     */
    fun configureDetailFrame(
        cornerRadius: Float,
        strokeWidth: Float,
        startColor: Int,
        endColor: Int
    ) {
        detailFrame.setStyle(cornerRadius, strokeWidth, startColor, endColor)
        if (foreground !== detailFrame) foreground = detailFrame
        invalidate()
    }

    fun clearDetailFrame() {
        if (foreground === detailFrame) foreground = null
    }

}
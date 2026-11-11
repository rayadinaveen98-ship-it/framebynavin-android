package com.framebynavin.app.reminders

import android.animation.ValueAnimator
import android.app.ActivityManager
import android.content.Context
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Backlot V330 — direct, offline rendering of the approved Motion Lab V2 art.
 *
 * The V329 native scene reduced the V2 1,250-point Canvas engine to 200–380
 * dots and simplified its ribbons, shards and lighting. This adapter embeds
 * that SAME approved renderer instead of another native visual approximation.
 *
 * Safety: local asset only; no URLs loaded over the network; no JS bridge;
 * native Compose recognizes double taps and keeps the original reminder
 * screen/action callbacks outside this visual scene.
 */
@Composable
internal fun V330ReminderPrototypeScene(
    domain: V329ReminderDomain,
    channel: V329ReminderChannel,
    appearance: V329ReminderAppearance,
    onReveal: () -> Unit,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    motionOverride: Boolean? = null,
    lowPowerOverride: Boolean? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val revealNow by rememberUpdatedState(onReveal)
    val motionEnabled = motionOverride ?: remember { ValueAnimator.areAnimatorsEnabled() }
    val eco = lowPowerOverride ?: remember(context) {
        context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true ||
            context.getSystemService(ActivityManager::class.java)?.isLowRamDevice == true
    }
    val safe = appearance.normalized()
    val identity = "${domain.name}_${channel.name}_${safe.world.name}_${interactive}_${eco}"
    var rendererFailed by remember(identity) { mutableStateOf(false) }
    // On devices lacking a working WebView provider, preserve the usable
    // legacy particle scene and unchanged reminder actions.
    val providerReady = remember {
        runCatching { WebView.getCurrentWebViewPackage() != null }.getOrDefault(false)
    }
    if (!providerReady || rendererFailed) {
        V329ReminderMotionScene(
            channel = channel,
            appearance = safe,
            onReveal = onReveal,
            modifier = modifier,
            interactive = interactive,
            motionOverride = motionEnabled,
            lowPowerOverride = eco,
        )
        return
    }

    val html = remember(context) {
        context.assets.open("backlot_v330_motion.html").bufferedReader().use { it.readText() }
    }
    var currentWeb by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(currentWeb, lifecycleOwner) {
        val observed = currentWeb
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP ->
                    observed?.evaluateJavascript("window.BacklotMotion?.pause()", null)
                Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME ->
                    observed?.evaluateJavascript("window.BacklotMotion?.resume()", null)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier.fillMaxSize().testTag("v330-prototype-motion-entry")) {
        key(identity) {
            AndroidView(
                modifier = Modifier.fillMaxSize().testTag("v1952-ambient-particles"),
                factory = { viewContext ->
                    WebView(viewContext).apply {
                        setBackgroundColor(android.graphics.Color.rgb(5, 4, 7))
                        // Do not force hardware layers. WebView's default renderer
                        // selects hardware/software compositing for the actual device.
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = false
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.blockNetworkLoads = true
                        settings.mediaPlaybackRequiresUserGesture = true
                        isHorizontalScrollBarEnabled = false
                        isVerticalScrollBarEnabled = false
                        isFocusable = false
                        webChromeClient = object : WebChromeClient() {
                            override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                                if (message.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                                    Log.e("BacklotMotion", "JS ${message.message()} line ${message.lineNumber()}")
                                }
                                return true
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?,
                            ): Boolean = true

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                val checkedView = view ?: return
                                checkedView.postDelayed({
                                    if (currentWeb === checkedView) {
                                        checkedView.evaluateJavascript(
                                            "(function(){try{var s=window.BacklotMotion?.state?.();" +
                                                "return !!(s&&s.nodes>=180&&s.viewportWidth>=120&&s.viewportHeight>=120)}" +
                                                "catch(e){return false}})()",
                                        ) { result ->
                                            if (result.trim() != "true" && currentWeb === checkedView) {
                                                Log.e("BacklotMotion", "Canvas did not initialize; using safe fallback")
                                                rendererFailed = true
                                            }
                                        }
                                    }
                                }, 1250L)
                            }

                            override fun onRenderProcessGone(
                                view: WebView,
                                detail: RenderProcessGoneDetail,
                            ): Boolean {
                                Log.e("BacklotMotion", "WebView renderer exited; using safe fallback")
                                rendererFailed = true
                                return true
                            }
                        }
                        val query = "?mode=${safe.world.ordinal}" +
                            "&hub=${domain.name.lowercase()}" +
                            "&kind=${channel.name.lowercase()}" +
                            "&eco=${if (eco) 1 else 0}" +
                            "&preview=${if (interactive) 0 else 1}" +
                            "&intensity=${safe.intensity}" +
                            "&density=${safe.density}" +
                            "&speed=${safe.speed}"
                        val page = html.replace(
                            "const params=new URLSearchParams(location.search);",
                            "const params=new URLSearchParams('$query');",
                        )
                        // AndroidView factory executes BEFORE its final measure.
                        // Load the animation only after the view has real dimensions;
                        // the HTML also uses ResizeObserver on subsequent rotations.
                        addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
                            override fun onLayoutChange(
                                v: View, left: Int, top: Int, right: Int, bottom: Int,
                                oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int,
                            ) {
                                if (right - left >= 120 && bottom - top >= 120) {
                                    v.removeOnLayoutChangeListener(this)
                                    loadDataWithBaseURL(
                                        "https://appassets.androidplatform.net/backlot/v332/",
                                        page, "text/html", "UTF-8", null,
                                    )
                                }
                            }
                        })
                        currentWeb = this
                    }
                },
                update = { web ->
                    // Slider changes update the live browser rendering without
                    // rebuilding the WebView or changing any reminder record.
                    web.evaluateJavascript(
                        "window.BacklotMotion?.update(${safe.intensity},${safe.density},${safe.speed})",
                        null,
                    )
                },
                onReset = null,
                onRelease = { released ->
                    if (currentWeb === released) currentWeb = null
                    runCatching { released.stopLoading() }
                    runCatching { released.destroy() }
                },
            )
        }
        if (interactive) {
            // Android, not web JS, owns the reveal gesture. A single tap only
            // energizes the scene; a double tap reaches pre-existing controls.
            Box(
                Modifier.fillMaxSize()
                    // The original adapter only reported a single tap *after*
                    // release. That prevented continuous particle displacement.
                    // Forward down / drag / up at a capped frame rate, while
                    // keeping native Compose in charge of the reveal gesture.
                    .pointerInput(identity) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            currentWeb?.evaluateJavascript(
                                "window.BacklotMotion?.startNormalized(" +
                                    "${(down.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)}," +
                                    "${(down.position.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)})",
                                null,
                            )
                            var lastForwardedAt = 0L
                            var active = true
                            while (active) {
                                val pointer = awaitPointerEvent().changes
                                    .firstOrNull { it.id == down.id }
                                if (pointer == null || !pointer.pressed) {
                                    currentWeb?.evaluateJavascript(
                                        "window.BacklotMotion?.end()", null,
                                    )
                                    active = false
                                } else {
                                    val at = SystemClock.uptimeMillis()
                                    if (at - lastForwardedAt >= 32L) {
                                        currentWeb?.evaluateJavascript(
                                            "window.BacklotMotion?.moveNormalized(" +
                                                "${(pointer.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)}," +
                                                "${(pointer.position.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)})",
                                            null,
                                        )
                                        lastForwardedAt = at
                                    }
                                }
                            }
                        }
                    }
                    .pointerInput(identity) {
                        detectTapGestures(
                            onTap = { /* First touch already energized the particles. */ },
                            onDoubleTap = { revealNow() },
                        )
                    }
                    .semantics {
                        contentDescription = "Reminder animation. Double tap to open controls"
                        // TalkBack's usual double-tap means one accessibility
                        // activation, not two rapid physical touches.
                        onClick(label = "Open reminder controls") {
                            revealNow()
                            true
                        }
                    },
            )
        }
    }
}

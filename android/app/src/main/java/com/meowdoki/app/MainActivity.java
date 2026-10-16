package com.meowdoki.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.webkit.WebViewAssetLoader;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MEOWDOKI: the game runs in a WebView from the app's assets; ads come from the Google Mobile Ads SDK.
 * The game calls window.MeowNative.showAd(kind, adUnitId, requestId) and gets the result back through
 * window.__meowAdCallbacks[requestId]('earned' | 'closed' | 'failed').
 */
public class MainActivity extends Activity {

    // Google's official test ad units. Debug builds always use these (see app/build.gradle).
    private static final String TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712";
    private static final String TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917";
    private static final String TEST_REWARDED_INTERSTITIAL = "ca-app-pub-3940256099942544/5354046379";

    private WebView webView;
    private ConsentInformation consentInformation;
    private final AtomicBoolean adsInitialized = new AtomicBoolean(false);
    private boolean adShowing = false;
    private volatile boolean personalized = true;

    private volatile float safeTopDp = 0;

    /* height of the status bar / camera cutout in CSS pixels (dp); the larger of the two */
    private float statusBarDp(int cutoutPx) {
        int px = cutoutPx;
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (id > 0) px = Math.max(px, getResources().getDimensionPixelSize(id));
        return px / getResources().getDisplayMetrics().density;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        installCrashReporter();
        if (Build.VERSION.SDK_INT >= 28) {
            // draw the game behind the camera cutout too (no black band at the top)
            android.view.WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }
        hideSystemBars();

        final WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        try {
            webView = new WebView(this);
        } catch (Throwable t) {
            showError("MEOWDOKI needs Android System WebView. Please update \"Android System WebView\" and \"Google Chrome\" in the Play Store, then open the game again.", t);
            return;
        }
        webView.setBackgroundColor(0xFFFCEFDF);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setTextZoom(100);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.addJavascriptInterface(new Bridge(), "MeowNative");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public boolean onRenderProcessGone(WebView view, android.webkit.RenderProcessGoneDetail detail) {
                // The web engine crashed or was killed (e.g. low memory): keep the app alive and reload the game.
                String why = Build.VERSION.SDK_INT >= 26 && detail != null && detail.didCrash() ? "crashed" : "was stopped by the system (low memory?)";
                try (FileOutputStream out = new FileOutputStream(crashFile())) {
                    out.write(("The game engine " + why + ".\nDevice: " + Build.MANUFACTURER + " " + Build.MODEL + ", Android " + Build.VERSION.RELEASE).getBytes(StandardCharsets.UTF_8));
                } catch (Throwable ignored) { }
                recreate();
                return true;
            }
        });
        setContentView(webView);
        // keep the game below the front camera / status bar area: measure it and tell the page
        safeTopDp = statusBarDp(0);
        webView.setOnApplyWindowInsetsListener((v, insets) -> {
            int px = 0;
            if (Build.VERSION.SDK_INT >= 28 && insets.getDisplayCutout() != null) px = insets.getDisplayCutout().getSafeInsetTop();
            float dp = statusBarDp(px);
            if (Math.abs(dp - safeTopDp) > 0.5f) {
                safeTopDp = dp;
                v.post(() -> webView.evaluateJavascript("window.__meowSafe && window.__meowSafe(" + safeTopDp + ")", null));
            }
            return v.onApplyWindowInsets(insets);
        });
        webView.loadUrl("https://appassets.androidplatform.net/App.dc.html");

        try {
            gatherConsentThenInitAds();
        } catch (Throwable t) {
            initAds();
        }
        showPreviousCrashIfAny();
    }

    // ---------- crash report (shown on the next start, so testers can send it) ----------

    private File crashFile() { return new File(getFilesDir(), "last_crash.txt"); }

    private void installCrashReporter() {
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, e) -> {
            try (FileOutputStream out = new FileOutputStream(crashFile())) {
                out.write(describe(e).getBytes(StandardCharsets.UTF_8));
            } catch (Throwable ignored) { }
            if (previous != null) previous.uncaughtException(thread, e);
        });
    }

    private String describe(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return "Device: " + Build.MANUFACTURER + " " + Build.MODEL + ", Android " + Build.VERSION.RELEASE
                + " (SDK " + Build.VERSION.SDK_INT + ")\n\n" + sw;
    }

    private void showPreviousCrashIfAny() {
        File f = crashFile();
        if (!f.exists()) return;
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            try (FileInputStream in = new FileInputStream(f)) {
                byte[] b = new byte[4096];
                int r;
                while ((r = in.read(b)) > 0) buf.write(b, 0, r);
            }
            String text = new String(buf.toByteArray(), StandardCharsets.UTF_8);
            f.delete();
            showReport("MEOWDOKI crashed last time", text);
        } catch (Throwable ignored) { }
    }

    private void showError(String message, Throwable t) {
        showReport("MEOWDOKI can't start", message + "\n\n" + describe(t));
    }

    private void showReport(String title, String text) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(text.length() > 3000 ? text.substring(0, 3000) : text)
                .setPositiveButton("Copy report", (d, w) -> {
                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("MEOWDOKI crash", text));
                })
                .setNegativeButton("Close", null)
                .show();
    }

    // ---------- consent (Google UMP) + ads init ----------

    private void gatherConsentThenInitAds() {
        consentInformation = UserMessagingPlatform.getConsentInformation(this);
        ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();
        consentInformation.requestConsentInfoUpdate(this, params,
                () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(this, formError -> initAds()),
                requestError -> initAds());
        if (consentInformation.canRequestAds()) initAds();
    }

    private void initAds() {
        if (adsInitialized.getAndSet(true)) return;
        new Thread(() -> MobileAds.initialize(this, status -> { })).start();
    }

    // ---------- ads ----------

    private String unitFor(String kind, String realId) {
        if (BuildConfig.USE_TEST_ADS || realId == null || realId.isEmpty()) {
            if ("interstitial".equals(kind)) return TEST_INTERSTITIAL;
            if ("rewardedInterstitial".equals(kind)) return TEST_REWARDED_INTERSTITIAL;
            return TEST_REWARDED;
        }
        return realId;
    }

    private void reply(String requestId, String status) {
        adShowing = false;
        hideSystemBars();
        String rid = requestId.replaceAll("[^A-Za-z0-9]", "");
        String js = "window.__meowAdCallbacks&&window.__meowAdCallbacks['" + rid + "']&&window.__meowAdCallbacks['" + rid + "']('" + status + "')";
        runOnUiThread(() -> webView.evaluateJavascript(js, null));
    }

    private void showAd(String kind, String realId, String requestId) {
        if (adShowing) { reply(requestId, "failed"); return; }
        if (consentInformation != null && !consentInformation.canRequestAds()) { reply(requestId, "failed"); return; }
        adShowing = true;
        initAds();
        final Activity activity = this;
        final String unit = unitFor(kind, realId);
        AdRequest.Builder builder = new AdRequest.Builder();
        if (!personalized) {
            // The player switched off personalized ads in the game: request non-personalized ads.
            Bundle extras = new Bundle();
            extras.putString("npa", "1");
            builder.addNetworkExtrasBundle(AdMobAdapter.class, extras);
        }
        final AdRequest request = builder.build();
        final boolean[] earned = {false};

        final FullScreenContentCallback rewardCallback = new FullScreenContentCallback() {
            @Override public void onAdDismissedFullScreenContent() { reply(requestId, earned[0] ? "earned" : "closed"); }
            @Override public void onAdFailedToShowFullScreenContent(@NonNull AdError e) { reply(requestId, "failed"); }
        };

        if ("interstitial".equals(kind)) {
            InterstitialAd.load(activity, unit, request, new InterstitialAdLoadCallback() {
                @Override public void onAdLoaded(@NonNull InterstitialAd ad) {
                    ad.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override public void onAdDismissedFullScreenContent() { reply(requestId, "closed"); }
                        @Override public void onAdFailedToShowFullScreenContent(@NonNull AdError e) { reply(requestId, "failed"); }
                    });
                    ad.show(activity);
                }
                @Override public void onAdFailedToLoad(@NonNull LoadAdError e) { reply(requestId, "failed"); }
            });
        } else if ("rewardedInterstitial".equals(kind)) {
            RewardedInterstitialAd.load(activity, unit, request, new RewardedInterstitialAdLoadCallback() {
                @Override public void onAdLoaded(@NonNull RewardedInterstitialAd ad) {
                    ad.setFullScreenContentCallback(rewardCallback);
                    ad.show(activity, reward -> earned[0] = true);
                }
                @Override public void onAdFailedToLoad(@NonNull LoadAdError e) { reply(requestId, "failed"); }
            });
        } else {
            RewardedAd.load(activity, unit, request, new RewardedAdLoadCallback() {
                @Override public void onAdLoaded(@NonNull RewardedAd ad) {
                    ad.setFullScreenContentCallback(rewardCallback);
                    ad.show(activity, reward -> earned[0] = true);
                }
                @Override public void onAdFailedToLoad(@NonNull LoadAdError e) { reply(requestId, "failed"); }
            });
        }
    }

    /** Methods the game can call from JavaScript (window.MeowNative). */
    private class Bridge {
        @JavascriptInterface
        public void showAd(String kind, String adUnitId, String requestId) {
            runOnUiThread(() -> MainActivity.this.showAd(kind, adUnitId, requestId));
        }

        @JavascriptInterface
        public float safeTop() {
            return safeTopDp;
        }

        @JavascriptInterface
        public void setPersonalized(boolean on) {
            personalized = on;
        }

        /** Opens Google's privacy options form when the user's region requires it. Returns false otherwise. */
        @JavascriptInterface
        public boolean showPrivacyOptions() {
            if (consentInformation == null || consentInformation.getPrivacyOptionsRequirementStatus()
                    != ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED) return false;
            runOnUiThread(() -> UserMessagingPlatform.showPrivacyOptionsForm(MainActivity.this, formError -> { }));
            return true;
        }

        @JavascriptInterface
        public void vibrate(int ms) {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v == null) return;
            if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(Math.max(10, Math.min(ms, 400)), VibrationEffect.DEFAULT_AMPLITUDE));
            else v.vibrate(Math.max(10, Math.min(ms, 400)));
        }
    }

    // ---------- full screen ----------

    @SuppressWarnings("deprecation")
    private void hideSystemBars() {
        runOnUiThread(() -> {
            Window w = getWindow();
            if (Build.VERSION.SDK_INT >= 30) {
                w.setDecorFitsSystemWindows(false);
                WindowInsetsController c = w.getInsetsController();
                if (c != null) {
                    c.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                    c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            } else {
                w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && !adShowing) hideSystemBars();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        // The game handles Back itself (closes a window, or goes to Home). On the Home screen the app goes to the background.
        if (webView == null) { moveTaskToBack(true); return; }
        webView.evaluateJavascript("(window.__meowBack && window.__meowBack()) ? 'handled' : 'home'", value -> {
            if (value == null || !value.contains("handled")) moveTaskToBack(true);
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // stop the music when the player switches to another app (but not while one of our ads is on screen)
        if (webView != null && !adShowing) {
            webView.evaluateJavascript("window.__meowSetActive && window.__meowSetActive(false)", null);
            webView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
            webView.evaluateJavascript("window.__meowSetActive && window.__meowSetActive(true)", null);
        }
    }
}

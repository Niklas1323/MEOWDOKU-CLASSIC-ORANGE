package com.meowdoki.app;

import android.annotation.SuppressLint;
import android.app.Activity;
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

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemBars();

        final WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        webView = new WebView(this);
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
        });
        setContentView(webView);
        webView.loadUrl("https://appassets.androidplatform.net/App.dc.html");

        gatherConsentThenInitAds();
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
        final AdRequest request = new AdRequest.Builder().build();
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
        moveTaskToBack(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }
}

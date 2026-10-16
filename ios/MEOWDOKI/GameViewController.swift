import UIKit
import WebKit
import AppTrackingTransparency
@preconcurrency import GoogleMobileAds
import UserMessagingPlatform

/// MEOWDOKI: the game runs in a WKWebView from the app's "web" folder; ads come from the Google Mobile Ads SDK.
/// Same bridge as the Android app: the game calls window.MeowNative.showAd(kind, adUnitId, requestId) and gets
/// the result back through window.__meowAdCallbacks[requestId]('earned' | 'closed' | 'failed').
final class GameViewController: UIViewController, WKScriptMessageHandler, WKNavigationDelegate, FullScreenContentDelegate {

    static let background = UIColor(red: 0xFC / 255.0, green: 0xEF / 255.0, blue: 0xDF / 255.0, alpha: 1)

    /// window.MeowNative, defined before any script of the page runs (the game only saves progress when it exists).
    private static let bridgeScript = """
    (function () {
      if (window.MeowNative) return;
      function post(msg) { window.webkit.messageHandlers.meow.postMessage(msg); }
      window.MeowNative = {
        showAd: function (kind, adUnitId, requestId) {
          post({ type: 'showAd', kind: String(kind || ''), unit: String(adUnitId || ''), rid: String(requestId || '') });
        },
        safeTop: function () { return Number(window.__meowSafeTopValue) || 0; },
        setPersonalized: function (on) { try { post({ type: 'personalized', on: !!on }); } catch (e) {} },
        showPrivacyOptions: function () {
          if (!window.__meowPrivacyRequired) return false;
          try { post({ type: 'privacy' }); } catch (e) { return false; }
          return true;
        },
        vibrate: function (ms) { try { post({ type: 'vibrate', ms: Number(ms) || 0 }); } catch (e) {} }
      };
    })();
    """

    private var webView: WKWebView!
    private var started = false
    private var adsInitialized = false
    private var adShowing = false
    private var personalized = true
    private var privacyOptionsRequired = false
    private var safeTop = 0
    private var pendingTracking: (() -> Void)?

    // the ad on screen right now
    private var currentAd: AnyObject?
    private var currentRid = ""
    private var currentKind = ""
    private var earned = false

    private lazy var lightHaptic = UIImpactFeedbackGenerator(style: .light)
    private lazy var mediumHaptic = UIImpactFeedbackGenerator(style: .medium)
    private lazy var heavyHaptic = UIImpactFeedbackGenerator(style: .heavy)

    override var prefersStatusBarHidden: Bool { true }
    override var prefersHomeIndicatorAutoHidden: Bool { true }
    override var supportedInterfaceOrientations: UIInterfaceOrientationMask {
        UIDevice.current.userInterfaceIdiom == .pad ? [.portrait, .portraitUpsideDown] : .portrait
    }

    override func loadView() {
        let config = WKWebViewConfiguration()
        config.websiteDataStore = WKWebsiteDataStore.default()
        config.setURLSchemeHandler(WebAssetHandler(), forURLScheme: WebAssetHandler.scheme)
        config.allowsInlineMediaPlayback = true
        config.mediaTypesRequiringUserActionForPlayback = []
        config.userContentController.add(self, name: "meow")

        let web = WKWebView(frame: .zero, configuration: config)
        web.isOpaque = false
        web.backgroundColor = GameViewController.background
        web.scrollView.backgroundColor = GameViewController.background
        web.scrollView.bounces = false
        web.scrollView.alwaysBounceVertical = false
        web.scrollView.alwaysBounceHorizontal = false
        web.scrollView.contentInsetAdjustmentBehavior = .never
        web.scrollView.showsVerticalScrollIndicator = false
        web.scrollView.showsHorizontalScrollIndicator = false
        web.allowsLinkPreview = false
        web.allowsBackForwardNavigationGestures = false
        web.navigationDelegate = self
        #if DEBUG
        if #available(iOS 16.4, *) { web.isInspectable = true } // Safari > Develop, for testing
        #endif
        webView = web
        view = web
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        updateSafeTop()
        if !started {
            // start once the screen size and the notch area are known
            started = true
            startGame()
        }
    }

    override func viewSafeAreaInsetsDidChange() {
        super.viewSafeAreaInsetsDidChange()
        updateSafeTop()
    }

    private func startGame() {
        let state = WKUserScript(source: stateScript(), injectionTime: .atDocumentStart, forMainFrameOnly: true)
        let bridge = WKUserScript(source: GameViewController.bridgeScript, injectionTime: .atDocumentStart, forMainFrameOnly: true)
        webView.configuration.userContentController.addUserScript(state)
        webView.configuration.userContentController.addUserScript(bridge)
        webView.load(URLRequest(url: WebAssetHandler.startURL))
        gatherConsentThenInitAds()
    }

    // ---------- values the page reads from window.MeowNative ----------

    private func stateScript() -> String {
        return "window.__meowSafeTopValue=\(safeTop);window.__meowPrivacyRequired=\(privacyOptionsRequired ? "true" : "false");"
    }

    /// Keep the game below the notch / Dynamic Island: measure it and tell the page.
    private func updateSafeTop() {
        let top = Int(view.safeAreaInsets.top.rounded())
        guard top != safeTop else { return }
        safeTop = top
        if started { evaluate("window.__meowSafeTopValue=\(top);window.__meowSafe&&window.__meowSafe(\(top))") }
    }

    private func pushStateToPage() {
        evaluate(stateScript() + "window.__meowSafe&&window.__meowSafe(\(safeTop))")
    }

    private func evaluate(_ js: String) {
        webView?.evaluateJavaScript(js, completionHandler: nil)
    }

    // ---------- app background / foreground ----------

    func appDidEnterBackground() {
        // stop the music when the player switches to another app (but not while one of our ads is on screen)
        if !adShowing { evaluate("window.__meowSetActive&&window.__meowSetActive(false)") }
    }

    func appDidBecomeActive() {
        evaluate("window.__meowSetActive&&window.__meowSetActive(true)")
        if let next = pendingTracking {
            pendingTracking = nil
            requestTrackingThen(next)
        }
    }

    // ---------- consent (Google UMP), App Tracking Transparency, ads init ----------

    private func gatherConsentThenInitAds() {
        let parameters = RequestParameters()
        ConsentInformation.shared.requestConsentInfoUpdate(with: parameters) { [weak self] requestError in
            Task { @MainActor in
                guard let self = self else { return }
                if requestError == nil {
                    do {
                        try await ConsentForm.loadAndPresentIfRequired(from: self)
                    } catch {
                        // no form, or it could not be shown: carry on with what we have
                    }
                }
                self.consentFinished()
            }
        }
        // returning player who already answered everything: ads can start right away
        if ConsentInformation.shared.canRequestAds && ATTrackingManager.trackingAuthorizationStatus != .notDetermined {
            initAds()
        }
    }

    private func consentFinished() {
        privacyOptionsRequired = ConsentInformation.shared.privacyOptionsRequirementStatus == .required
        evaluate("window.__meowPrivacyRequired=\(privacyOptionsRequired ? "true" : "false")")
        requestTrackingThen { [weak self] in
            guard let self = self else { return }
            if ConsentInformation.shared.canRequestAds { self.initAds() }
        }
    }

    /// Asks once for permission to use the advertising id (Apple's tracking dialog), then calls done.
    private func requestTrackingThen(_ done: @escaping () -> Void) {
        guard ATTrackingManager.trackingAuthorizationStatus == .notDetermined else {
            done()
            return
        }
        // the dialog only appears while the app is in the foreground: otherwise ask when it comes back
        guard UIApplication.shared.applicationState == .active else {
            pendingTracking = done
            return
        }
        ATTrackingManager.requestTrackingAuthorization { _ in
            DispatchQueue.main.async { done() }
        }
    }

    private func initAds() {
        if adsInitialized { return }
        adsInitialized = true
        MobileAds.shared.start()
    }

    // ---------- ads ----------

    private func reply(_ requestId: String, _ status: String) {
        adShowing = false
        currentAd = nil
        setNeedsStatusBarAppearanceUpdate()
        let rid = String(requestId.filter { $0.isASCII && ($0.isLetter || $0.isNumber) })
        evaluate("window.__meowAdCallbacks&&window.__meowAdCallbacks['\(rid)']&&window.__meowAdCallbacks['\(rid)']('\(status)')")
    }

    private func showAd(kind: String, requestId: String) {
        if adShowing { reply(requestId, "failed"); return }
        if !ConsentInformation.shared.canRequestAds { reply(requestId, "failed"); return }
        adShowing = true
        initAds()
        let unit = AdConfig.unitID(for: kind)
        let request = Request()
        if !personalized {
            // The player switched off personalized ads in the game: request non-personalized ads.
            let extras = Extras()
            extras.additionalParameters = ["npa": "1"]
            request.register(extras)
        }
        currentRid = requestId
        currentKind = kind
        earned = false

        Task { @MainActor in
            do {
                switch kind {
                case "interstitial":
                    let ad = try await InterstitialAd.load(with: unit, request: request)
                    ad.fullScreenContentDelegate = self
                    self.currentAd = ad
                    ad.present(from: self)
                case "rewardedInterstitial":
                    let ad = try await RewardedInterstitialAd.load(with: unit, request: request)
                    ad.fullScreenContentDelegate = self
                    self.currentAd = ad
                    ad.present(from: self) { [weak self] in self?.earned = true }
                default:
                    let ad = try await RewardedAd.load(with: unit, request: request)
                    ad.fullScreenContentDelegate = self
                    self.currentAd = ad
                    ad.present(from: self) { [weak self] in self?.earned = true }
                }
            } catch {
                self.reply(requestId, "failed")
            }
        }
    }

    func adDidDismissFullScreenContent(_ ad: FullScreenPresentingAd) {
        let status = currentKind != "interstitial" && earned ? "earned" : "closed"
        reply(currentRid, status)
    }

    func ad(_ ad: FullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
        reply(currentRid, "failed")
    }

    // ---------- messages from the page (window.MeowNative) ----------

    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        guard message.name == "meow",
              let body = message.body as? [String: Any],
              let type = body["type"] as? String else { return }
        switch type {
        case "showAd":
            let kind = body["kind"] as? String ?? "rewarded"
            let rid = body["rid"] as? String ?? ""
            showAd(kind: kind, requestId: rid)
        case "personalized":
            personalized = (body["on"] as? NSNumber)?.boolValue ?? true
        case "privacy":
            // Google's privacy options form (only offered by the page when the user's region requires it)
            Task { @MainActor in
                do {
                    try await ConsentForm.presentPrivacyOptionsForm(from: self)
                } catch {
                    // nothing to show
                }
            }
        case "vibrate":
            let ms = (body["ms"] as? NSNumber)?.doubleValue ?? 30
            let haptic = ms < 40 ? lightHaptic : (ms < 70 ? mediumHaptic : heavyHaptic)
            haptic.impactOccurred()
        default:
            break
        }
    }

    // ---------- navigation ----------

    func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction,
                 decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        // links to web pages (e.g. Google's privacy policy) open in Safari, the game stays where it is
        if let url = navigationAction.request.url, let scheme = url.scheme?.lowercased(),
           scheme != WebAssetHandler.scheme && scheme != "about" {
            if scheme == "http" || scheme == "https" || scheme == "mailto" {
                UIApplication.shared.open(url, options: [:], completionHandler: nil)
            }
            decisionHandler(.cancel)
            return
        }
        decisionHandler(.allow)
    }

    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        pushStateToPage()
    }

    func webViewWebContentProcessDidTerminate(_ webView: WKWebView) {
        // The web engine was stopped (e.g. low memory): reload the game (progress is saved in localStorage).
        if adShowing { adShowing = false; currentAd = nil }
        webView.reload()
    }
}

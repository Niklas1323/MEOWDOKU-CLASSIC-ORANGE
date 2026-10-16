import Foundation

/// Which ad units the iPhone app uses.
///
/// The game passes the ANDROID ad unit ids, which do not work on iPhone. So the iPhone app picks its
/// own unit by the kind of ad. Debug builds always use Google's test ads; release builds use the real
/// iOS units below, or Google's test ads as long as a unit is still empty.
enum AdConfig {

    // >>> Put the real iOS ad unit ids from AdMob here (from the separate iOS app in AdMob). <<<
    // Example: "ca-app-pub-8181395204701342/1234567890". Empty = Google's test ads.
    static let realUnits: [String: String] = [
        "interstitial": "",
        "rewarded": "",
        "rewardedInterstitial": "",
    ]

    // Google's official iOS test ad units.
    static let testInterstitial = "ca-app-pub-3940256099942544/4411468910"
    static let testRewarded = "ca-app-pub-3940256099942544/1712485313"
    static let testRewardedInterstitial = "ca-app-pub-3940256099942544/6978759866"

    /// kind is "interstitial", "rewarded" or "rewardedInterstitial" (anything else counts as "rewarded").
    static func unitID(for kind: String) -> String {
        let test: String
        switch kind {
        case "interstitial": test = testInterstitial
        case "rewardedInterstitial": test = testRewardedInterstitial
        default: test = testRewarded
        }
        #if DEBUG
        return test
        #else
        let key = (kind == "interstitial" || kind == "rewardedInterstitial") ? kind : "rewarded"
        if let real = realUnits[key], !real.isEmpty { return real }
        return test
        #endif
    }
}

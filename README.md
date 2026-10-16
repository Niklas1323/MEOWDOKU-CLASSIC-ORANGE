# MEOWDOKI

Cozy cat logic puzzle (one cat per row, column and color area; cats never touch).

- `android/` – Android app: the game runs in a WebView from `app/src/main/assets`, ads come from Google AdMob.
  Debug builds always use Google's test ads. Release builds use the real ad units configured in the game.
- `tools/pack_web.py` – copies the design-canvas game files into the Android assets folder.
- `.github/workflows/android.yml` – builds `apk/meowdoki-debug.apk` (test ads) on every push that touches `android/`;
  with the signing secrets set it also builds the store version `release/meowdoki-release.aab` (+ `.apk`).
- `android/.../Widgets.java` – the four home-screen widgets (streak, play, daily challenge, before bed).
- `ios/` – iPhone shell for the same game files (see `ios/README.md`); `.github/workflows/ios.yml` checks that it builds.
- `site/` – website for https://niklas1323.github.io (privacy policy, app-ads.txt).
- `store/` – Play Store texts, data safety answers and graphics.
- `ANLEITUNG.md` – step-by-step guide (German) for publishing.

# MEOWDOKI

Cozy cat logic puzzle (one cat per row, column and color area; cats never touch).

- `android/` – Android app: the game runs in a WebView from `app/src/main/assets`, ads come from Google AdMob.
  Debug builds always use Google's test ads. Release builds use the real ad units configured in the game.
- `tools/pack_web.py` – copies the design-canvas game files into the Android assets folder.
- `.github/workflows/android.yml` – builds `app-debug.apk` on every push that touches `android/` (download it from the workflow run's Artifacts).

# MEOWDOKI für iPhone / iPad

Das ist die iPhone-Version von MEOWDOKI. Das Spiel selbst ist **dasselbe** wie in der Android-App
(die Dateien in `android/app/src/main/assets`). Die iPhone-App zeigt es nur an und holt die Werbung
von Google AdMob – genau wie die Android-App. Änderungen am Spiel landen also automatisch in beiden Apps.

## Was du brauchst

- **Einen Mac mit Xcode** (kostenlos im Mac App Store), um die App zu bauen und hochzuladen.
- **Ein Apple-Developer-Konto** (99 USD pro Jahr, https://developer.apple.com/programs/).
  Ohne dieses Konto kann die App weder über TestFlight getestet noch in den App Store gebracht werden.
- Auf dem Mac: `brew install xcodegen`, dann im Ordner `ios` den Befehl `xcodegen generate` ausführen.
  Danach `MEOWDOKI.xcodeproj` in Xcode öffnen, unter „Signing & Capabilities“ dein Team auswählen
  und die App auf das iPhone oder zu TestFlight (Product › Archive) bringen.

## Werbung: vor der Veröffentlichung unbedingt ändern

Im Moment zeigt die iPhone-App **nur Test-Werbung von Google** (damit verdienst du nichts).
Die Android-Werbe-IDs funktionieren auf dem iPhone nicht. So geht's:

1. In AdMob (https://apps.admob.com) eine **neue App für iOS** anlegen („MEOWDOKI“, Plattform iOS).
2. Ihre **App-ID** (sieht so aus: `ca-app-pub-8181395204701342~1234567890`) eintragen in
   `ios/MEOWDOKI/Info.plist`, beim Eintrag `GADApplicationIdentifier`
   (dort steht jetzt Googles Test-ID `ca-app-pub-3940256099942544~1458002511`).
   Wichtig: nicht die Android-App-ID nehmen und das Feld nie leer lassen.
3. In der neuen iOS-App drei **Anzeigenblöcke** anlegen: Interstitial, Rewarded (Prämie) und
   Rewarded Interstitial (Interstitial mit Prämie). Ihre IDs eintragen in
   `ios/MEOWDOKI/AdConfig.swift`, in der Liste `realUnits`:
   - `"interstitial": "…"`
   - `"rewarded": "…"`
   - `"rewardedInterstitial": "…"`

   Solange dort `""` steht, wird Test-Werbung gezeigt. Debug-Builds (direkt aus Xcode auf dem eigenen
   iPhone) zeigen immer Test-Werbung – das ist so gewollt.
4. In AdMob unter „Datenschutz und Mitteilungen“ die DSGVO-Einwilligung (und am besten auch die
   IDFA-Erklärung für iOS) für die iOS-App einrichten, wie bei Android.

## Was die GitHub-Prüfung bedeutet

Bei jeder Änderung im Ordner `ios/` baut GitHub die App einmal für den iPhone-Simulator
(„Build iOS app (check)“ unter „Actions“). Ein grüner Haken heißt nur: **der Code lässt sich bauen**.
Es entsteht dabei keine installierbare App – dafür braucht man den Mac und das Apple-Konto (siehe oben).

## Gut zu wissen

- Der Spielstand wird auf dem Gerät gespeichert (wie bei Android).
- Der Startbildschirm-Hintergrund und das App-Symbol liegen in `ios/MEOWDOKI/Assets.xcassets`.
- Die Version steht in `ios/project.yml` (`MARKETING_VERSION` = Versionsnummer, `CURRENT_PROJECT_VERSION`
  = Build-Nummer; die Build-Nummer bei jedem Upload um 1 erhöhen).

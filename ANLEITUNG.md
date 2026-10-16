# MEOWDOKI: Der Weg in den Play Store

Hier steht nur, was **du selbst** machen musst. Alles andere ist im Projekt schon vorbereitet.
Hake die Schritte der Reihe nach ab.

---

## 1. Signatur-Schlüssel bei GitHub hinterlegen (10 Minuten)

Damit GitHub die **Store-Version** (Datei `.aab`) bauen kann, braucht es deinen Upload-Schlüssel.
Du hast von mir zwei Dateien bekommen: `meowdoki-upload.jks.base64.txt` und `password.txt`.
Außerdem die Datei `meowdoki-upload.jks` selbst.

> **Sicher aufbewahren:** Speichere alle drei Dateien zusätzlich an einem sicheren Ort
> (z. B. Passwort-Manager oder USB-Stick). Nie öffentlich hochladen, nie ins Projekt legen.
> Wenn er verloren geht, kann Google den Upload-Schlüssel zwar zurücksetzen, aber das dauert ein paar Tage.

1. Öffne https://github.com/Niklas1323/MEOWDOKU-CLASSIC-ORANGE/settings/secrets/actions
2. Klicke **New repository secret**.
   - Name: `MEOWDOKI_KEYSTORE_BASE64`
   - Secret: den **ganzen Inhalt** von `meowdoki-upload.jks.base64.txt` einfügen
3. Nochmal **New repository secret**.
   - Name: `MEOWDOKI_KEYSTORE_PASSWORD`
   - Secret: den Inhalt von `password.txt`
4. Öffne https://github.com/Niklas1323/MEOWDOKU-CLASSIC-ORANGE/actions/workflows/android.yml
   und klicke **Run workflow** › **Run workflow**.
5. Nach ca. 3 Minuten liegt im Projekt der Ordner `release/` mit
   - `meowdoki-release.aab`: **das lädst du bei Google Play hoch**
   - `meowdoki-release.apk`: die gleiche Version zum direkten Installieren (mit echter Werbung,
     darum dort **nicht selbst auf Anzeigen tippen**)

Ab dann entsteht bei jeder Änderung automatisch eine neue Store-Version mit höherer Versionsnummer.

---

## 2. Webseite für Datenschutz und app-ads.txt (5 Minuten)

Die Seiten sind fertig (Ordner `site/`). Sie brauchen nur noch eine Adresse. Eine neue GitHub-Seite darf
ich selbst nicht anlegen, das musst du einmal tun:

1. Öffne https://github.com/new
2. Repository name: **`Niklas1323.github.io`** (genau so), **Public** auswählen, **Create repository**.
3. Schreib mir „Webseiten-Repo ist da“. Dann lade ich die Seiten hoch.
   (Alternativ selbst: im neuen Repo **uploading an existing file** anklicken und den **Inhalt** des
   Ordners `site/` hineinziehen.)

Danach sind diese Adressen online:
- Datenschutz (für die Play Console): https://niklas1323.github.io/meowdoki/privacy.html
- Deutsch: https://niklas1323.github.io/meowdoki/datenschutz.html
- app-ads.txt: https://niklas1323.github.io/app-ads.txt

---

## 3. AdMob (10 Minuten)

1. https://apps.admob.com › **Datenschutz und Mitteilungen** › **DSGVO** › Mitteilung erstellen,
   App **MEOWDOKI** auswählen, Sprachen Deutsch + Englisch, **Veröffentlichen**.
   Ohne diese Mitteilung fragt die App in Europa nie nach Einwilligung, und Google zeigt dort nur eingeschränkte Werbung.
2. Unter **Apps › MEOWDOKI › App-Einstellungen** die App mit dem Play-Store-Eintrag verknüpfen,
   sobald die App im Store ist. AdMob findet dann die `app-ads.txt` über die Webseite aus Schritt 2.
3. Empfehlung: unter **Blockierungseinstellungen** die maximale Anzeigen-Einstufung auf **T (Teen)** stellen.

---

## 4. Google Play Console

1. Entwicklerkonto: https://play.google.com/console (einmalig 25 USD, Ausweis-Prüfung).
2. **App erstellen**: Name `MEOWDOKI`, Standardsprache Deutsch, Spiel, kostenlos.
3. Texte: alles steht fertig zum Kopieren in
   - `store/listing-de.md` (Deutsch) und `store/listing-en.md` (Englisch)
   - Bei **Website** `https://niklas1323.github.io` eintragen (wichtig für app-ads.txt).
4. Grafiken: im Ordner `store/graphics/`
   - `app-icon-512.png`: App-Symbol
   - `de/feature-graphic-1024x500.png`: Titelbild (Deutsch), `en/…` für Englisch
   - `de/screenshot-1…6.png`: Handy-Screenshots (Deutsch), `en/…` für Englisch
5. **App-Inhalte** (Datensicherheit, Werbung, Werbe-ID, Zielgruppe, Einstufung):
   jede Antwort steht in `store/data-safety.md`.
6. **Testen**: Neue private Konten müssen die App zuerst **14 Tage mit mindestens 12 Testern**
   im **geschlossenen Test** laufen lassen. Dort `release/meowdoki-release.aab` hochladen,
   Tester per E-Mail-Liste einladen. Bei der ersten Hochladung bietet Google
   **„Play App-Signatur“** an: annehmen.
7. Nach den 14 Tagen: **Produktion** beantragen, dieselbe `.aab` (oder eine neuere) freigeben.

---

## 5. Widgets ausprobieren

Neue Test-APK: `apk/meowdoki-debug.apk`. Nach der Installation einmal das Spiel öffnen und wieder
schließen, dann auf dem Startbildschirm lange drücken › **Widgets** › **MEOWDOKI**. Es gibt vier:
Serie (4×2), Spielen (2×2), Tagesrätsel (2×2) und Vor dem Schlafen (4×2). Antippen öffnet das Spiel.
Sie zeigen deine echte Serie, das heutige Ziel, dein Level und deine Katze und aktualisieren sich,
sobald du das Spiel verlässt.

---

## 6. iPhone (später)

Die iPhone-Version ist gebaut und lässt sich übersetzen (Ordner `ios/`, Anleitung in `ios/README.md`).
Es fehlen nur Dinge, die dein Apple-Konto brauchen:
1. Apple-Developer-Konto (99 USD/Jahr).
2. In AdMob eine **iOS-App** anlegen und mir die App-ID und die drei Anzeigenblock-IDs schicken.
3. Dann richte ich ein, dass GitHub die App direkt zu **TestFlight** hochlädt (ein Mac ist dafür nicht nötig).

---

## 7. Tablet-Problem

Dafür brauche ich weiterhin den **Bericht** von deinem Tablet: Wenn die App dort beim letzten Start
abgestürzt ist, zeigt sie beim nächsten Start ein Fenster mit **„Bericht kopieren“**. Den Text bitte an mich schicken.

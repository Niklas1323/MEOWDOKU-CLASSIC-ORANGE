# MEOWDOKI – Antworten für die Google Play Console

Diese Anleitung hilft dir, in der Play Console den Bereich **Richtlinien › App-Inhalte** auszufüllen.
Die Begriffe stehen auf Deutsch und in Klammern auf Englisch, falls deine Play Console auf Englisch eingestellt ist.

**Wichtig:** Du bist selbst dafür verantwortlich, dass die Angaben stimmen. Diese Antworten passen zur App, so wie sie jetzt gebaut ist (Version 1.0, Google Mobile Ads SDK 23.3.0, UMP-SDK 3.0.0, kein Firebase, keine eigenen Server, keine Käufe). Wenn du später etwas einbaust (zum Beispiel Firebase, Käufe oder ein Konto), müssen die Antworten angepasst werden.

---

## Kurz erklärt: Was passiert mit Daten in MEOWDOKI?

- **Die App selbst** schickt keine Daten an dich oder an einen eigenen Server. Es gibt kein Konto.
- **Spielstand** (Level, XP, Herzen, Tipps, Katzen, Bretter, Serie, Einstellungen, Sprache) wird **nur auf dem Handy** gespeichert. Daten, die das Gerät nie verlassen, gelten für Google **nicht** als „erhoben“. Du musst sie also **nicht** angeben.
- **Codes** (zum Beispiel zum Entfernen der Werbung) werden in der App geprüft. Es wird nichts verschickt.
- **Werbung von Google AdMob** (Google Mobile Ads SDK) und das **Einwilligungsformular von Google** (UMP-SDK) schicken Daten an Google. **Nur diese Daten** musst du im Formular angeben.

---

## 1. Datensicherheit (Data safety)

Play Console › Richtlinien › App-Inhalte › **Datensicherheit** › Starten

### Schritt 1: Übersicht
Einfach weiter.

### Schritt 2: Datenerhebung und Sicherheit (Data collection and security)

| Frage | Antwort |
|---|---|
| Erhebt oder teilt deine App erforderliche Nutzerdatentypen? (Does your app collect or share any of the required user data types?) | **Ja** (wegen der Werbung von AdMob) |
| Werden alle von deiner App erhobenen Nutzerdaten bei der Übertragung verschlüsselt? (Is all of the user data collected by your app encrypted in transit?) | **Ja** (Google verschickt die Daten über HTTPS) |
| Welche Arten der Kontoerstellung unterstützt deine App? (Which of the following methods of account creation does your app support?) | **Meine App erlaubt Nutzern nicht, ein Konto zu erstellen** (My app does not allow users to create an account) |
| Können Nutzer die Löschung ihrer Daten beantragen? (Do you provide a way for users to request that their data is deleted?) | **Ja** – siehe Erklärung unten |

**Erklärung zur Löschung (einfach):**
- Den Spielstand löscht man selbst: App deinstallieren oder in Android unter *Einstellungen › Apps › MEOWDOKI › Speicher › Daten löschen*.
- Die Werbe-ID kann man in Android jederzeit zurücksetzen oder löschen (meist unter *Einstellungen › Datenschutz › Werbung*).
- Wer Fragen hat oder eine Löschung verlangen will, kann dir an **niklasrahm2301@gmail.com** schreiben (so steht es auch in der Datenschutzerklärung, Abschnitt 10).
- Die Werbedaten speichert Google, nicht du. Du hast keinen Zugriff darauf. Das steht ebenfalls in der Datenschutzerklärung.

Falls die Play Console eine **URL zum Löschen von Daten** verlangt (optional, wenn es kein Konto gibt), kannst du die Datenschutzerklärung angeben:
`https://niklas1323.github.io/meowdoki/datenschutz.html`

### Schritt 3: Datentypen (Data types)

Hake **nur diese** Datentypen an:

| Bereich | Datentyp | Ankreuzen? |
|---|---|---|
| Standort (Location) | **Ungefährer Standort** (Approximate location) | ✅ Ja |
| Standort (Location) | Genauer Standort (Precise location) | ❌ Nein |
| App-Aktivitäten (App activity) | **App-Interaktionen** (App interactions) | ✅ Ja |
| App-Informationen und -Leistung (App info and performance) | **Absturzprotokolle** (Crash logs) | ✅ Ja |
| App-Informationen und -Leistung (App info and performance) | **Diagnosedaten** (Diagnostics) | ✅ Ja |
| App-Informationen und -Leistung (App info and performance) | **Sonstige Leistungsdaten der App** (Other app performance data) | ✅ Ja |
| Geräte- oder andere IDs (Device or other IDs) | **Geräte- oder andere IDs** (Device or other IDs) | ✅ Ja |

**Alles andere bleibt leer:** keine persönlichen Daten (Name, E-Mail, Adresse, Telefon), keine Finanzdaten, keine Gesundheitsdaten, keine Nachrichten, keine Fotos/Videos, keine Audiodateien, keine Dateien, keine Kalender, keine Kontakte, kein Webbrowserverlauf.

Warum diese Datentypen?
- Google schreibt in seiner offiziellen Anleitung zum Google Mobile Ads SDK, dass das SDK automatisch **IP-Adresse** (daraus wird der *ungefähre Standort* geschätzt), **Interaktionen** (z. B. App-Start, Tippen auf Werbung, angesehene Videos), **Diagnosedaten** (z. B. Ladezeit, Hänger, Energieverbrauch) und **Geräte-IDs** (Werbe-ID, App-Set-ID) erhebt und teilt.
- **Absturzprotokolle** und **Sonstige Leistungsdaten** stehen in Googles Tabelle nicht immer als eigene Zeile. Sie werden aber meistens zusammen mit „Diagnosedaten“ angegeben. Es ist sicherer, sie anzukreuzen: Zu viel angeben ist kein Problem, zu wenig angeben schon.
- Das **Einwilligungsformular von Google (UMP)** nutzt Daten derselben Art (IP-Adresse, Geräte-ID, Diagnosedaten). Dafür musst du nichts Zusätzliches ankreuzen.

### Schritt 4: Fragen zu jedem Datentyp

Für **jeden** der angekreuzten Datentypen beantwortest du dieselben Fragen. Die Antworten sind überall gleich:

| Frage | Antwort |
|---|---|
| Werden diese Daten **erhoben** (collected)? | **Ja** |
| Werden diese Daten **geteilt** (shared)? | **Ja** (sie gehen an Google als Werbenetzwerk) |
| Werden diese Daten nur kurzzeitig verarbeitet (ephemeral)? | **Nein** |
| Ist die Erhebung erforderlich oder optional? (Is this data collection required or optional?) | **Erforderlich** (Nutzer können sie nicht ausschalten, solange Werbung geladen wird) |
| Zwecke der **Erhebung** (Why is this user data collected?) | ✅ **Werbung oder Marketing** (Advertising or marketing)<br>✅ **Analysen** (Analytics)<br>✅ **Betrugsprävention, Sicherheit und Compliance** (Fraud prevention, security, and compliance) |
| Zwecke der **Weitergabe** (Why is this user data shared?) | dieselben drei: ✅ Werbung oder Marketing, ✅ Analysen, ✅ Betrugsprävention, Sicherheit und Compliance |

**Nicht** ankreuzen: App-Funktionen, Entwicklerkommunikation, Personalisierung, Kontoverwaltung.

### Schritt 5: Vorschau
Prüfen und **Speichern**. Danach unter *Veröffentlichungsübersicht* zur Prüfung einreichen.

---

## 2. Werbung (Ads)

Play Console › App-Inhalte › **Werbung**

| Frage | Antwort |
|---|---|
| Enthält deine App Werbung? (Does your app contain ads?) | **Ja, meine App enthält Werbung** |

Hinweis: Auch Werbung mit Belohnung (Video ansehen für Herzen, Tipp, doppelte Belohnung oder Freischalten) zählt als Werbung. Im Play Store steht dann „Enthält Werbung“.

---

## 3. Werbe-ID (Advertising ID)

Play Console › App-Inhalte › **Werbe-ID**

| Frage | Antwort |
|---|---|
| Verwendet deine App eine Werbe-ID? (Does your app use advertising ID?) | **Ja** |
| Wofür? (Why does your app need to use advertising ID?) | ✅ **Werbung oder Marketing** (Advertising or marketing)<br>✅ **Analysen** (Analytics)<br>✅ **Betrugsprävention, Sicherheit und Compliance** (Fraud prevention, security, and compliance) |

Das ist dieselbe Auswahl wie bei der Datensicherheit, damit alles zusammenpasst. Wenn du es ganz einfach halten willst, reicht auch nur „Werbung oder Marketing“.

Technischer Hinweis: Die nötige Berechtigung `com.google.android.gms.permission.AD_ID` fügt das Google Mobile Ads SDK (ab Version 20.4) automatisch zur App hinzu. Du musst nichts ändern.

---

## 4. Zielgruppe und Inhalte (Target audience and content)

Play Console › App-Inhalte › **Zielgruppe und Inhalte**

### Empfehlung: Altersgruppen **13–15, 16–17 und 18 und älter** auswählen

**Nicht** auswählen: 5 und jünger, 6–8, 9–12.

Warum? Ganz einfach:
- Sobald du eine Altersgruppe **unter 13** auswählst, gilt die **Familienrichtlinie** von Google. Dann darfst du nur speziell zertifizierte Werbung zeigen, die Werbe-ID nicht für Kinder nutzen und musst viele Zusatzregeln einhalten. Das ist für eine kleine App viel Aufwand.
- In deiner Datenschutzerklärung steht schon: „MEOWDOKI ist ein Spiel für ein allgemeines Publikum und richtet sich nicht an Kinder unter 13 Jahren.“ Die Angaben in der Play Console passen so genau dazu.
- Wenn du ganz auf Nummer sicher gehen willst, kannst du auch **nur „18 und älter“** wählen. Das ist aber nicht nötig.

### Frage: Könnte deine App unbeabsichtigt Kinder ansprechen? (Could your store listing unintentionally appeal to children?)
Empfehlung: **Nein**. MEOWDOKI ist ein Logikrätsel (ähnlich wie Sudoku), die Texte sind für Jugendliche und Erwachsene geschrieben (z. B. „ruhige Rätsel vor dem Schlafengehen“).
Falls Google die App wegen der niedlichen Katzen trotzdem als „für Kinder ansprechend“ einstuft, bekommt sie im Store höchstens den Hinweis „Nicht für Kinder konzipiert“. Das ist kein Problem.

### Zusätzliche Empfehlung in AdMob
Stell in AdMob unter **Blockierungseinstellungen › Einstellungen für alle Apps › Maximale Altersfreigabe für Anzeigen** (Max ad content rating) **„T“ (Teen)** oder **„PG“** ein. Dann bekommen junge Spieler keine Werbung für Erwachsene.

---

## 5. Einstufung des Inhalts (Content rating, IARC-Fragebogen)

Play Console › App-Inhalte › **Einstufung des Inhalts** › Fragebogen starten

- **E-Mail-Adresse:** niklasrahm2301@gmail.com
- **Kategorie:** **Spiel** (Game)

Antworten:

| Thema | Antwort |
|---|---|
| Gewalt (Violence) | **Nein** |
| Angst / Horror (Fear) | **Nein** |
| Sexualität / Nacktheit (Sexuality) | **Nein** |
| Glücksspiel oder simuliertes Glücksspiel (Gambling) | **Nein** |
| Schimpfwörter / grobe Sprache (Language) | **Nein** |
| Drogen, Alkohol, Tabak (Controlled substances) | **Nein** |
| Derber Humor (Crude humor) | **Nein** |
| Können Nutzer miteinander kommunizieren oder Inhalte austauschen? (Users interact / chat / user-generated content) | **Nein** |
| Teilt die App den Standort des Nutzers mit anderen Nutzern? (Shares location) | **Nein** |
| Können Nutzer digitale Waren mit echtem Geld kaufen? (Purchases of digital goods) | **Nein** |
| Zufällige Belohnungen gegen Geld, „Lootboxen“ (Random items) | **Nein** |
| Uneingeschränkter Internetzugang / Webbrowser in der App (Unrestricted internet) | **Nein** |
| Enthält die App Werbung? (falls gefragt) | **Ja** |

Erwartetes Ergebnis: ungefähr **PEGI 3 / USK 0 / ESRB „Everyone“**.

**Zu den Käufen:** In MEOWDOKI gibt es **keine Käufe mit echtem Geld**. Im Spielstand gibt es zwar das Feld „Werbung entfernt“, aber das wird **nur mit einem kostenlosen Code** eingeschaltet (Einstellungen › Konto › Code einlösen). Es gibt keine Google-Play-Abrechnung (Billing) in der App. XP sind reine Spielwährung, die man durch Spielen verdient. Auch die Nutzungsbedingungen sagen: „Die App bietet keine Käufe mit echtem Geld an.“

---

## 6. App-Zugriff (App access)

Play Console › App-Inhalte › **App-Zugriff**

Antwort: **Alle Funktionen sind ohne besonderen Zugang verfügbar** (All functionality in my app is available without any special access).

Es gibt keine Anmeldung, kein Passwort und keine gesperrten Bereiche. (Der Code zum Entfernen der Werbung ist kein Zugang, die App funktioniert auch ohne ihn vollständig.)

---

## 7. Weitere Erklärungen (alle „Nein“)

| Erklärung | Antwort |
|---|---|
| **Behörden-Apps** (Government apps): Wurde die App von oder für eine Behörde entwickelt? | **Nein** |
| **Finanzfunktionen** (Financial features) | **Meine App bietet keine Finanzfunktionen** |
| **Gesundheit** (Health apps) | **Meine App hat keine Gesundheitsfunktionen** (die Schlaftipps beim Laden sind nur allgemeine Tipps, keine Gesundheitsfunktion) |
| **Nachrichten-Apps** (News apps), falls gefragt | **Nein** |
| **Datenschutzerklärung** (Privacy policy) | `https://niklas1323.github.io/meowdoki/privacy.html` |

---

## Checkliste zum Abhaken

- [ ] Website ist online und die Datenschutz-Links funktionieren
- [ ] `https://niklas1323.github.io/app-ads.txt` ist erreichbar
- [ ] Im Play-Store-Eintrag ist als Website `https://niklas1323.github.io/` eingetragen (AdMob findet app-ads.txt über diese Website; die Prüfung in AdMob kann bis zu 24 Stunden dauern)
- [ ] Datensicherheit ausgefüllt
- [ ] Werbung: Ja
- [ ] Werbe-ID: Ja
- [ ] Zielgruppe: 13–15, 16–17, 18+
- [ ] Einstufung des Inhalts ausgefüllt
- [ ] App-Zugriff: ohne besonderen Zugang
- [ ] Behörden-App, Finanzfunktionen, Gesundheit: Nein

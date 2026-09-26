# Stone Chat

Ein modulares Chat-Management-Plugin für Paper 26.2+ (Java 25) - ein Plugin von Stone Plugins.

## ⚠️ Wichtig: Das hier ist der Quellcode, nicht die fertige .jar
Dieses ZIP enthält den **Quellcode** des Plugins - er muss einmal kompiliert werden, bevor eine
einzelne `.jar`-Datei dabei herauskommt, die du in `plugins/` legen kannst. Ich konnte das leider
nicht für dich erledigen: Meine Umgebung hat keinen Zugriff auf die Maven-/PaperMC-Repositories, aus
denen die benötigten Abhängigkeiten geladen werden (von dort aus direkt getestet: beide antworten mit
"403 verboten"). Zwei Wege, wie du trotzdem in unter einer Minute an die eine `.jar`-Datei kommst:

**Weg 1 - Lokal, wenn Java + Maven installiert sind** (auf den meisten Servern, auf denen du eh schon
einen Minecraft-Server betreibst, oft schon vorhanden):
```
mvn clean package
```
Die fertige Datei liegt danach unter `target/StoneChat-1.1.0.jar` - das ist die eine Datei, die du
brauchst. In `plugins/` kopieren, Server neu starten, fertig.

**Weg 2 - Ohne eigene Java-Installation, über GitHub Actions** (baut die jar automatisch für dich):
1. Dieses ZIP in ein neues (auch privates) GitHub-Repository hochladen/pushen
2. GitHub baut automatisch über die mitgelieferte `.github/workflows/build.yml` - das dauert ca. 1 Minute
3. Im Reiter **Actions** auf den letzten Lauf klicken, unter **Artifacts** liegt `StoneChat` zum Download bereit - das ist wieder die eine `.jar`-Datei

## Versionierung
Die Version steht zentral in `pom.xml` (`<version>`) und wird automatisch in `plugin.yml` übernommen (Maven-Resource-Filtering). Das gebaute Jar heißt `StoneChat-<version>.jar` (z. B. `StoneChat-1.0.0.jar`), zu finden unter `target/` nach `mvn clean package`. Bei jeder gewünschten Änderung wird die Version in `pom.xml` erhöht - aktueller Stand: **1.1.0**.

## Features
- **Neu in Version 1.1.0:** **Chatformat-Bugfix** - öffentliche Chat-Nachrichten werden nicht mehr abgebrochen und per `Bukkit.broadcast` als Systemnachricht neu verschickt, sondern über Papers `ChatRenderer` als echte Spieler-Chatnachricht ausgeliefert. Dadurch greifen wieder Client-Chateinstellungen und das Ausblenden von Spielern über „Soziale Interaktionen", und Chat-Bridges (z. B. Discord-Plugins), die abgebrochene Chat-Events überspringen, bekommen die Nachrichten wieder mit. **Neu: `/chatlog <spieler> [seite]`** (Permission `stonechat.chatlog`, Standard OP) zeigt, was ein Spieler wann im Chat geschrieben hat - inkl. blockierter Versuche mit Grund (Wortfilter, Link, Caps, Mute, Cooldown, ...). Gespeichert pro Spieler unter `chatlogs/`, Aufbewahrungsdauer und Maximalanzahl in `config.yml` → `chat-log`. **Neu: automatische Nachrichten** (`config.yml` → `auto-messages`, standardmäßig aus) - postet im festen Intervall (z. B. alle 60 Sekunden einen Hinweis auf `/discord`) nacheinander oder zufällig eine Nachricht, per Chat/Actionbar/Title/Bossbar, auch mehrzeilig und klickbar per MiniMessage. Beide Features haben eine eigene Kategorie im In-Game-Editor (`/sc editor`)
- **Neu in Version 1.0.1:** Bug behoben - `chat-color-gui.enabled` und `use-permission` wurden geladen, aber nirgends geprüft, daher ließ sich das Farben-GUI weder für Spieler ausschalten noch per Permission einschränken; jetzt korrekt geprüft in `ChatColorGuiManager.open()`. Neuer vollständig anpassbarer `gui/`-Ordner (`gui/chatcolor-gui.yml`, `gui/settings-gui.yml`) - jeder Titel, jedes Material, jeder Name und jede Lore-Zeile im Farben-GUI und im Spieler-Settings-Menü ist jetzt ohne Code-Änderung konfigurierbar
- **Version 1.2.0 - Editor-Redesign:** neues, einheitliches Small-Caps-Header-System (echte Unicode-Small-Caps-Glyphen über neues `SmallCaps`-Utility) mit blau-cyanem Marken-Gradient (`#55A8FE` → `#00C9FF`) für alle Menü-Titel im Admin-Editor, Farben-GUI und Spieler-Settings-Menü; Tooltips überarbeitet - jetzt mit dezenten Trennlinien, aktuellem Wert in Gold-Gradient hervorgehoben, Default-Wert-Anzeige bei Schaltern, und klar strukturierten Klick-Hinweisen (▶ Left-Click / Right-Click)
- **Neu in Version 1.1.2:** Zurück-Button im Chat-Games-"Settings & Timers"-Menü ging fälschlich zum Hauptmenü statt zur Chat-Games-Übersicht - benutzte versehentlich das normale `renderMenu()` statt der Custom-Back-Variante, jetzt korrekt auf `openChatGamesMenu()` gesetzt
- **Neu in Version 1.1.1:** Compile-Fehler in `ChatGameManager.java` behoben (`addCustomGameEntry`) - `getMapList()` liefert `List<Map<?, ?>>`, das lässt sich nicht direkt in eine `ArrayList<Map<String, Object>>` konstruieren, jetzt wird die Liste sauber Element für Element umkopiert
- **Version 1.1.0 - Chat Games komplett neu gebaut (Breaking Change):** das gesamte Modul wurde von Grund auf gepurged und neu geschrieben. Fünf neue eingebaute Spieltypen (Math Solver, Unscramble Word, Fast Typing, Trivia/Quiz, Fill in the Blanks - ersetzt Reverse Typing), jeder als eigene Java-Klasse in `chatgame/games/`. Neue, komplett englisch dokumentierte `chatgames.yml` mit global konfigurierbarem Reward-System: Console-Commands, **Vault-Economy-Auszahlung** (optional, reflection-basiert wie LuckPerms), Sound und **Partikeleffekt**, jeweils pro Spiel überschreibbar. Komplett neues 4-Ebenen-Editor-GUI (Übersicht → Settings & Timers / Default Games / Custom Games → Detail-Menü pro Spiel), inklusive Erstellungs-Assistent und Löschen-mit-Bestätigung für Custom Games. **Achtung:** alte `chatgames.yml`- und `game-content.yml`-Dateien werden nicht automatisch migriert - beim Update manuell sichern und neu einrichten.
- **Neu in Version 1.0.3 (Performance-Pass für hohe Spielerzahlen):** `ConfigManager` cacht jetzt alle ~60 Config-Werte einmalig statt bei jedem Zugriff den YAML-Baum zu durchlaufen (relevant, da eine einzelne Chat-Nachricht 15-25+ solcher Lookups auslöst); `LuckPermsUtil` und `PlaceholderUtil` cachen jetzt die reflektiven `Method`-Referenzen einmalig statt sie bei jeder Chat-Nachricht neu über `Class.getMethod()` aufzulösen; zusätzliche Aufräumarbeiten beim Spieler-Verlassen (`PrivateMessageManager`, `AnvilInputManager`, `PendingBroadcastManager`) verhindern langsames Map-Wachstum auf lange laufenden Servern mit vielen unterschiedlichen Spielern über die Zeit. Siehe Analyse unten für Details.
- **Neu in Version 1.0.2:** Toggle-Icons zurück auf Farbstoff (grün = an, grau = aus) - der Stone-Button-Versuch aus 1.0.1 wurde wieder zurückgenommen
- **Neu in Version 1.0.1:** Der Hebel (Lever) als Toggle-Icon sah als Item nur wie ein dünner Stab aus, nicht wie ein Schalter - ersetzt durch einen sauberen Stone-Button, der als Icon deutlich besser aussieht und trotzdem klar als "Schalter" erkennbar ist
- **Version 1.0.0 - Erster öffentlicher Release:** interne Code-Kommentare aus allen Java-Dateien entfernt (Config-Kommentare in `config.yml`/`chatgames.yml`/`chatformat.yml` bleiben natürlich erhalten, die sind für dich als Server-Admin gedacht); Jar heißt jetzt `StoneChat-<version>.jar` statt `StoneChat-Test-<version>.jar`. Toggle-Schalter im Editor zeigen jetzt einheitlich einen Hebel (Lever) statt überall grünem/grauem Farbstoff - Zustand erkennst du am Namen (grün = an, grau = aus), nicht mehr an einem bunten Block; Update-Checker-Menü nochmal aufgeräumt (Status zuerst, größere Statuskarte); Unscramble und Reverse-Typing haben jetzt auch einen einstellbaren Wortlängen-Bereich (im jeweiligen Spiel selbst, nicht im allgemeinen Menü) - genau wie beim Math-Spiel
- **Neu in Version 1.0.10:** Math-Chat-Game-Einstellungen (Zahlenbereich, Operatoren) sind jetzt im Detail-Menü des Math-Games selbst statt im allgemeinen Chat-Games-Menü; alle 14 Premium-Farben überarbeitet (mehr Gradient-Stops, 4 neue: Galactic, Royalty, Toxic, Bloodmoon) plus 7 weitere normale Gradients (Mint Choc, Sky Dive, Watermelon, Grape Soda, Seafoam, Autumn); Spieler-Settings hat jetzt eine echte Ignore-Verwaltung mit zwei GUIs: "My Ignore List" (alle ignorierten Spieler als Spielerköpfe, ein Klick entfernt sie) und "Ignore Someone" (alle Online-Spieler als Köpfe, durchsuchbar per Chat-Eingabe, ein Klick ignoriert)
- **Neu in Version 1.0.9:** **Command-Cooldown-Bug behoben** - `cooldown-commands` war unsichtbar auf `[msg, r, tell]` beschränkt, jetzt standardmäßig "alle Befehle" und im Editor als Liste editierbar; Chat-Mute kann jetzt separat einstellen, ob Befehle mit blockiert werden ("Also Block Commands"); Zahlenfelder zeigen jetzt eindeutig Shift+Links/Shift+Rechts; Item-Hover ohne Standard-Kursivschrift, wirkt aufgeräumter; Update-Checker-Menü zeigt jetzt aktuelle/neueste Version plus "Check Now"-Button; Math-Chat-Game hat jetzt konfigurierbaren Zahlenbereich und Operatoren; Premium-Farben nochmal deutlich krasser (mehr Gradient-Stops, Fett+Unterstrichen, 2 neue: Chaos, Supernova); Custom-Games haben jetzt auch einen eigenen Notification-Type; `/chatcolor` und `/settings` als eigene Befehle entfernt - alles läuft jetzt über `/chat` (Aliase weiterhin über `/stonechat settings`)
- **Neu in Version 1.0.8:** Hauptmenü hat jetzt eine komplett aus Glas bestehende Zeile ganz unten für einen saubereren Abschluss; Broadcast zeigt jetzt eine private Vorschau (nur für dich sichtbar, im exakt gleichen Kanal) bevor er tatsächlich verschickt wird - erst "confirm" tippen, dann geht's raus; 8 neue Premium-Gradients (Nebula, Inferno, Void, Divine, Prismatic, Phoenix, Celestial, Unicorn) und eine dritte Kategorie "✦ Premium ✦" im Farben-GUI neben Normal und Gradient; neue brandingfreie Befehle `/settings` und `/chat` (mit `/chat settings|color|ignore|msg|r`) für alles, was normale Spieler brauchen, ohne "stonechat" tippen zu müssen
- **Neu in Version 1.0.7:** Item-Beschreibungen deutlich verbessert - Cycle-Felder zeigen jetzt alle verfügbaren Optionen (nicht nur den aktuellen Wert), Action-Buttons (Broadcast senden, Fragen hinzufügen/entfernen, Spiel löschen, ...) haben jetzt konkrete Erklärungen statt "Click to run", Hauptmenü-Kategorien zeigen eine Kurzbeschreibung; Wortfilter-Menü logischer sortiert (Verhalten → Erkennung → Benachrichtigung → Bestrafung → Nachrichten)
- **Neu in Version 1.0.6:** Zurück-Pfeil sitzt jetzt konsequent im allerletzten Slot jedes Menüs; neuer, brandingfreier Befehl `/settings` (Aliase `/preferences`, `/pref`, `/mysettings`) öffnet das Spieler-Settings-Menü direkt, ohne über `/stonechat` zu müssen; Chat-Games haben jetzt zusätzlich pro Spiel „Min. Spieler online" und „Hinweise aktiviert" (überschreibt die globale Einstellung); Farben-GUI ist jetzt seitenweise (18 Farben pro Seite mit Vor/Zurück-Navigation) statt einer langen Liste
- **Neu in Version 1.0.5:** Zentrierungs-Bug behoben - die letzte, unvollständige Reihe eines Menüs (z. B. das 15. Kategorie-Item im Hauptmenü) landete durch die Lücken-Logik schief statt wirklich mittig; betrifft alle Editor-Menüs und das Farben-GUI. Farben-GUI hat jetzt einen "Zurück zu den Einstellungen"-Button, wenn man über das Spieler-Settings-Menü dorthin gelangt ist (vorher: Sackgasse)
- **Neu in Version 1.0.3:**
  - 11 weitere Gradient-Farben (Sunset, Lagoon, Peach, Cosmic, Jade, Royal, Berry, Citrus, Frost, Volcano, Unicorn) - jetzt 18 Gradients insgesamt
  - Alle Editor-Menüs und das Farben-GUI sind jetzt geräumiger (6 statt 7 eng gepackte Spalten pro Reihe, Lücke in der Mitte, mehr Puffer-Reihen)
  - Join-/Leave-Nachrichten-Unterdrückung zusätzlich abgesichert (läuft jetzt auf zwei Event-Prioritäten gleichzeitig)
  - Editor-Befehl umbenannt zu `/sc editor` (`/stonechat edit` funktioniert als stiller Alias weiter)
  - **Alle** Text-/Zahlen-Eingaben im Editor laufen jetzt über den Chat statt über einen Amboss (kein 50-Zeichen-Limit mehr)
  - Neues Spieler-Settings-Menü (`/stonechat settings`, für **alle** Spieler ohne Berechtigung): Pings an/aus, direkter Sprung zu `/chatcolor`, Übersicht der ignorierten Spieler
- **Aus Version 1.0.2:** "Broadcast sent"-Bestätigung entfernt; Zurück-Button im Farben-GUI; `%world%` aus dem Standard-Chat-Format entfernt; Custom-Chat-Games-Namensvalidierung; Detail-Menü für Chat-Games deutlich erweitert (Auto-Start-Zugehörigkeit, Frage entfernen, Spiel löschen)
- **Aus Version 1.0.1:** Punish-Command bei Chat-Mute entfernt; Chat-Games komplett im Editor erstellbar/bearbeitbar; Farben-GUI zeigt erst eine Auswahl zwischen "Normal Colors" und "Gradient Colors"
- **Neu in dieser Version:** Editor-Titel nutzen jetzt echte, kräftige Mehrfarben-Gradients (vorher: Hauptmenü-Titel war gar kein Gradient, Untermenü-Gradients waren zu farbähnlich und wirkten wie eine Farbe); An/Aus-Schalter zeigen jetzt Farbstoff-Items (Dye) statt grüner/roter Beton-Blöcke, die zwischen den anderen Items wie Fremdkörper wirkten; alle Menüs zentrieren jetzt konsequent die letzte, unvollständige Reihe statt sie links kleben zu lassen (kein einsames Item mehr in der Ecke)
- **Neu in dieser Version:** Join-/Leave-Nachrichten sind jetzt komplett entfernt (weder Vanilla noch eigene Nachricht - Beitritt/Verlassen ist still); Broadcasts lassen sich jetzt direkt aus dem Editor heraus per Chat verschicken (`/stonechat edit` → Broadcast → "Send Broadcast Now" - kein Amboss-Zeichenlimit mehr für die Nachricht selbst); alle Editor-Menüs haben jetzt themenfarbige Glasscheiben-Füllung statt überall grau (Rot für Wortfilter, Orange für Mute/Broadcast, Lila für private Nachrichten, Gold für Chat-Games, ...) für ein deutlich aufgeräumteres, systematisches Erscheinungsbild
- **Neu in dieser Version:** Broadcast hat jetzt einen eigenen, per Gradient gestylten Prefix (an/abschaltbar); der Standard-Prefix aller Nachrichten nutzt jetzt ebenfalls einen Gradient; der Broadcast-Editor ist jetzt dynamisch - der gewählte Anzeigetyp (Chat/Actionbar/Title/Bossbar) bestimmt automatisch, welche Einstellfelder angezeigt werden; sämtliche Editor-Menüs (inkl. Chat-Color-GUI) haben jetzt ein zentriertes, symmetrisches Layout mit Rahmen statt Feldern direkt am Rand, Gradient-Titel und passendere Icons; die doppelte Join-/Leave-Nachricht wurde behoben (Listener laufen jetzt auf `MONITOR`-Priority, garantiert die letzte Priorität - nichts kann die Unterdrückung mehr überschreiben)
- **In-Game-Editor (`/sc editor`):** öffnet ein GUI-Menü mit einer Kategorie pro Feature (Wortfilter, Chat-Mute, Link-Blocker, Ping, Cooldown, Anti-Caps, Join-Delay, Max-Länge, Chat-Format, Chat-Games, Broadcast, Chat-Color-GUI, Private Nachrichten, Chat-Clear, Update-Checker). Schalter per Klick umschalten, Zahlenwerte per Links-/Rechtsklick (Shift = ×10) anpassen, Optionen (z. B. `notification-type`) per Klick durchschalten, kurze Texte **und Nachrichtentexte** (24+ Message-Felder über alle Kategorien verteilt) direkt im Chat eingeben (kein Zeichenlimit mehr). Jede Änderung wird sofort gespeichert und per vollständigem Reload aktiv - kein manuelles `/stonechat reload` nötig. Bei Chat-Games lässt sich zusätzlich jedes einzelne Game im Detail erstellen, bearbeiten und löschen.
- Konfigurationsbasierter Wortfilter (blockieren oder zensieren) mit Admin-Benachrichtigung (zeigt Spieler + Original-Nachricht, auch bei Zensur)
- **Unicode-Umgehungserkennung:** erkennt Versuche, den Wortfilter mit unsichtbaren Zeichen, Look-alike-Buchstaben aus anderen Alphabeten (Kyrillisch, Griechisch, Fullwidth) oder Akzenten zu umgehen (z. B. `b​a​d` mit Zero-Width-Spaces, oder `ѕсаm` mit kyrillischen Buchstaben) und normalisiert sie zurück, bevor geprüft wird. Optional zusätzlich Leetspeak (`4`→`a`, `3`→`e` usw.), standardmäßig aus wegen höherem False-Positive-Risiko. Admins bekommen bei einem erkannten Umgehungsversuch eine eigene Benachrichtigung
- Globale Chat-Stummschaltung (`/chatmute`) mit Bypass-Permission
- Link-Blocker mit Bypass-Permission für Team-Mitglieder
- Spieler-Pings/Erwähnungen (`@SpielerName`) mit voll editierbarem Anzeige-Template (nicht nur Farbe - beliebige Umrandung/Symbole möglich) + Sound, Immun-Permission, ignorierbar
- Chat- & Command-Cooldown-System mit Bypass-Permission
- Anti-Großschreibungs-System (blockieren oder automatisch korrigieren)
- Chat-Verzögerung nach Beitritt (konfigurierbare Sekunden)
- Konfigurierbare maximale Nachrichtenlänge
- Vollständiges, konfigurierbares Chat-Format (eigene `chatformat.yml`) mit vielen Placeholdern (`%player_name%`, `%player_displayname%`, `%player_uuid%`, `%player_health%`, `%player_max_health%`, `%player_level%`, `%player_ping%`, `%player_gamemode%`, `%world%`, `%world_players%`, `%online%`, `%max_players%`, `%luckperms_rank%`, `%luckperms_prefix%`, `%luckperms_suffix%`, `%prefix%`, `%suffix%`, `%date%`, `%time%`, `%message%`) und PlaceholderAPI-Unterstützung, falls installiert
- **Interaktive Spielernamen im Chat:** Hovern über einen Namen zeigt Rang (LuckPerms), Spielzeit, Tode und Kills; klicken bereitet automatisch `/msg <Name> ` in der eigenen Chatzeile vor. Beides einzeln in `chatformat.yml` abschaltbar
- **Chat Games (`/chatgame`), komplett neu gebaute Architektur:** fünf eingebaute Spieltypen - Math Solver, Unscramble Word, Fast Typing, Trivia/Quiz, Fill in the Blanks - jeweils als eigene Java-Klasse (`chatgame/games/`), plus voll unterstützte Custom Games. Alles konfigurierbar (englisch dokumentiert) in der eigenen `chatgames.yml`: globaler Toggle, Intervall, Antwortzeit, Cooldown zwischen Runden, Hinweis-System (erster Buchstabe + Länge nach X Sekunden), Broadcast-Nachrichten (Start/Gewinn/Timeout/Hinweis) und ein globaler Standard-Reward. Jedes Spiel kann seinen eigenen Reward überschreiben: Console-Commands, **Vault-Economy-Auszahlung**, Sound und **Partikeleffekt** beim Gewinn. Editor-GUI (`/sc editor` → Chat Games) mit vier Ebenen: Übersicht (Global-Toggle/Settings & Timers/Default Games/Custom Games), Einstellungen & Timer, Liste der 5 Default-Games, sowie Custom-Games-Verwaltung mit Erstellungs-Assistent und Löschen (mit Bestätigung)
- **Automatischer Chat-Game-Intervall:** in `chatgames.yml` unter `chat-games.interval-seconds` einstellbar (Standard: alle 5 Minuten ein zufälliges aktiviertes Spiel starten, wenn gerade keines läuft, der Cooldown abgelaufen ist und genug Spieler online sind) - admins können jederzeit trotzdem manuell per `/chatgame start [id]` eingreifen
- Broadcast-System (`/broadcast <chat|actionbar|title|bossbar> [sekunden] <nachricht>`) über Chat, Actionbar, Title oder Bossbar - **die Anzeigedauer ist jetzt direkt als optionaler Command-Parameter einstellbar**, überschreibt die konfigurierten Standardwerte für diesen einen Broadcast
- **Private Nachrichten (`/msg`, Aliase `/tell`, `/w`, `/whisper`, sowie `/r` zum Antworten):** ersetzt die vanilla Whisper-Befehle durch eigene, frei formatierbare Nachrichten für Sender und Empfänger, inkl. Sound und einem Hover-zum-Antworten auf der empfangenen Nachricht
- **Spieler ignorieren (`/ignore <spieler>`, `/ignore list`):** blendet Pings/Erwähnungen und private Nachrichten von einem bestimmten Spieler dauerhaft aus (persistiert in `ignorelist.yml`), unabhängig vom globalen Ping-Toggle (`/stonechat togglepings`). Spieler mit `stonechat.ignore.immune` (z. B. Staff) können von niemandem ignoriert werden
- **Spieler-Settings-Menü (`/chat` oder `/stonechat settings`, für alle):** kein Admin-Tool - jeder Spieler kann hier Pings an/aus schalten, direkt zur Chat-Farbe springen und sehen, wie viele Spieler er gerade ignoriert
- **`/chatclear`:** leert den Chat für alle Online-Spieler (konfigurierbare Zeilenanzahl + Sound), meldet wer geleert hat
- **Chat-Color-GUI (`/chat color`):** Spieler öffnen ein dreigeteiltes Menü (Normal / Gradient / ✦ Premium ✦) und wählen die Farbe ihrer eigenen Chat-Nachrichten selbst aus, seitenweise navigierbar. Jede Farbe ist frei in `config.yml` definierbar (Name, Material, Farbcode inkl. MiniMessage-Gradients, optional `premium: true`) und kann optional an eine Permission gekoppelt werden (z. B. braucht man für Rot `stonechat.color.red`, standardmäßig automatisch für OP) - die Wahl wird dauerhaft gespeichert
- **Straf-Befehle:** bei Wortfilter-, Link-Blocker-, Anti-Caps- und Mute-Verstößen kann ein Admin in `config.yml` je einen `punish-command` hinterlegen (z. B. für ein Warn- oder Mute-Plugin), der automatisch im Hintergrund als Konsole ausgeführt wird - `%player%` wird durch den Namen des Spielers ersetzt. Leer lassen deaktiviert es
- **Eingeschränkte Command-Sichtbarkeit:** normale Spieler sehen in Tab-Completion und im vanilla `/help` nur `/stonechat` (mit begrenztem Hilfe-Menü), `/chat`, `/msg`/`/r` und `/ignore` - `/chatmute`, `/chatgame`, `/broadcast` und `/chatclear` bleiben für sie komplett unsichtbar, da sie in `plugin.yml` an eine Admin-Permission gebunden sind
- **Wählbares Benachrichtigungssystem, jetzt überall:** praktisch jede spielerseitige Meldung (Mute-Umschaltung, Ping, Wortfilter, Link-Blocker, Cooldown, Anti-Caps, Join-Delay, Max-Länge, Chat-Games, Chat leeren) lässt sich in der jeweiligen Config einzeln auf **Chat, Actionbar, Title mit Subtitle oder Bossbar** umstellen (`notification-type`) - der passende Text für alle vier Kanäle liegt bereits fertig in `messages.yml`
- **Update-Checker:** prüft automatisch [modrinth.com/project/stone-chat](https://modrinth.com/project/stone-chat) auf neue Versionen, meldet sich einmal beim Start und danach im konfigurierbaren Intervall in der Konsole, und erinnert Spieler mit `stonechat.admin` (oder OP) beim Beitritt, bis aktualisiert wurde. Manuell prüfbar über `/stonechat checkupdate`
- Alle Farben unterstützen `&`-Codes, `&#RRGGBB`-Hex **und** MiniMessage-Tags (z. B. `<gradient>`) gleichzeitig, im selben String
- Vollständige Englisch-/Deutsch-Unterstützung (`languages/en/`, `languages/de/`), Neuinstallationen starten standardmäßig auf Englisch
- **Konfigurierbare Minecraft-Sounds:** an allen sinnvollen Stellen wird ein Sound gespielt - z. B. wenn ein Spieler gepingt wird, gegen eine Regel verstößt (Wortfilter, Link-Blocker, Cooldown, Anti-Caps, Join-Delay, Max-Länge, stummgeschalteter Chat), wenn die Chat-Stummschaltung umgeschaltet wird, wenn ein Chat-Game startet/gewonnen wird, oder im Chat-Color-GUI ausgewählt/verweigert wird. Jeder Sound ist einzeln in `config.yml` ein-/ausschaltbar, mit Sound-Name, Lautstärke und Tonhöhe

## Performance & Nebenläufigkeit
- Regex-Patterns (Wortfilter, Ping-Trigger) und Link-Muster werden **einmalig** beim Start bzw. bei `/stonechat reload` aufgebaut und danach nur noch wiederverwendet, statt bei jeder einzelnen Chat-Nachricht neu kompiliert/verarbeitet zu werden
- Aufgelöste Minecraft-Sounds werden pro Config-Pfad gecacht, damit die (relativ teure) String-zu-Enum-Auflösung nicht bei jeder Nachricht erneut passiert
- Da Paper Chat-Nachrichten asynchron verarbeitet, während Commands/Listener wie Join, Quit und die Verwaltungsbefehle synchron auf dem Hauptthread laufen, sind alle zwischen diesen Threads geteilten Zustände entweder `volatile`, über `ConcurrentHashMap`/`ConcurrentHashMap.newKeySet()` abgesichert, oder (beim Chat-Game-Rundenstatus) über ein explizites Lock synchronisiert - das verhindert u. a. eine Race Condition, bei der zwei Spieler gleichzeitig als Chat-Game-Gewinner durchgehen könnten

## Projektstruktur
```
src/main/java/dev/stonechat/plugin/
  StoneChat.java          -> Hauptklasse, nur Initialisierung
  manager/                 -> gesamte Feature-Logik (ein Manager pro Feature, inkl. SettingsEditorManager, PlayerSettingsGuiManager & AnvilInputManager für /sc editor)
  command/                 -> Command-Executor + Tab-Completer
  listener/                -> Bukkit/Paper Event-Listener
  config/                  -> ConfigUpdater (sicheres automatisches Nachziehen von yml-Dateien)
  model/                   -> reine Datenklassen (MessageDisplayType, ChatColorGuiHolder, EditorGuiHolder)
  chatgame/                -> komplett eigenständiges Chat-Games-Modul: ChatGameManager, GameType, GameRound, RewardConfig, abstrakte ChatGame-Basisklasse
  chatgame/games/          -> die sechs Spiel-Implementierungen: MathGame, UnscrambleGame, FastTypingGame, TriviaGame, FillBlanksGame, CustomGame
  util/                    -> ColorUtil (gemischtes Farb-Parsing), PlaceholderUtil, LuckPermsUtil, VaultEconomyUtil, TextNormalizer (Unicode-Umgehungserkennung)
src/main/resources/
  plugin.yml
  config.yml
  chatformat.yml           -> Chat-Format-Settings + Placeholder-Doku (eigene Datei)
  chatgames.yml            -> komplettes Chat-Games-Modul: globale Einstellungen, Broadcast-Texte, Standard-Reward, alle 5 Default-Games + Custom Games (eigene Datei, vollständig englisch dokumentiert)
  languages/en/messages.yml
  languages/de/messages.yml
```

## Kompilieren
Benötigt JDK 25 und Maven.

```
mvn clean package
```

Die fertige jar liegt danach unter `target/StoneChat-1.1.0.jar`. Einfach in den `plugins/`-Ordner deines Paper-Servers legen und neu starten.

> Hinweis: Dieses Projekt wurde von Hand geschrieben und geprüft, konnte in der Umgebung, in der es
> entstanden ist, aber nicht selbst kompiliert werden (kein Zugriff auf Maven Central / das
> PaperMC-Repository). Bitte führe `mvn clean package` einmal selbst aus und melde dich, falls dabei
> Compile-Fehler auftauchen - die behebe ich dann sofort.

## Berechtigungen
Die vollständige Liste steht in `plugin.yml`. Alle Admin-Permissions sind standardmäßig auf `op`
gesetzt; `stonechat.ping.immune` ist standardmäßig `false` (muss pro Team-Mitglied vergeben werden).
Für das Chat-Color-GUI gilt dasselbe Prinzip: `stonechat.color.gold`, `stonechat.color.red` und
`stonechat.color.rainbow` sind als Beispiele standardmäßig `false` - jede Farbe in `config.yml` kann
über ihr `permission`-Feld frei mit einer eigenen (oder gar keiner) Berechtigung versehen werden.

## Soft-Abhängigkeiten
- **LuckPerms** wird rein über Reflection angebunden (`util/LuckPermsUtil.java`) - keine harte Abhängigkeit, kein zusätzlicher Eintrag in `pom.xml` nötig. Ist LuckPerms nicht installiert, liefern die `%luckperms_*%`-Placeholder einfach einen leeren String.
- **PlaceholderAPI** wird ebenfalls nur per Reflection genutzt (`util/PlaceholderUtil.java`).
- `/msg`, `/tell`, `/w`, `/whisper` und `/r` werden von Stone Chat als eigene Commands in `plugin.yml` registriert und überschreiben damit die vanilla-Whisper-Befehle (Standardverhalten für Chat-Plugins). Ist bereits ein anderes Plugin mit denselben Befehlen aktiv, gewinnt je nach Ladereihenfolge eines der beiden - im Zweifel das jeweils andere Plugin deaktivieren.

## Neue Chat-Games oder Farben ergänzen
- **Wortliste/Trivia/Custom Games:** komplett ohne Code-Änderung über `/sc editor` → Chat Games möglich (Default Games Management für die 5 eingebauten Typen, Custom Games Management für eigene). Direkt in `chatgames.yml` geht's genauso, unter `default-games.<id>.words`/`phrases`/`questions` bzw. `custom-games.<id>.questions`.
- **Neuer Chat-Game-Typ:** in `chatgame/GameType.java` ergänzen, eine neue Klasse in `chatgame/games/` anlegen (erbt von `chatgame/ChatGame.java`, implementiert `generateRound(Random)`), und in `chatgame/ChatGameManager.java#parseGame()` einen neuen `case` im Switch hinzufügen.
- **Neue Farbe im Chat-Color-GUI:** einfach einen neuen Eintrag unter `chat-color-gui.colors` in
  `config.yml` anlegen - kein Code-Änderung nötig.

## Update-Checker & Notification-System personalisieren
- Der Update-Checker sucht aktuell nach dem Modrinth-Projekt `stone-chat`
  (`https://modrinth.com/project/stone-chat`). Falls dein Projekt dort unter einem anderen Slug liegt,
  einfach `MODRINTH_PROJECT_SLUG` in `manager/UpdateChecker.java` anpassen.
- Das wählbare Benachrichtigungssystem (`manager/NotificationManager.java` + `model/MessageDisplayType.java`)
  lässt sich für weitere Features wiederverwenden: einfach in `messages.yml` einen Textblock mit
  `<basePath>.chat`, `.actionbar`, `.title` und `.subtitle` anlegen und in `config.yml` ein passendes
  `notification-type: CHAT` hinzufügen - der Rest übernimmt der `NotificationManager`.

## Erweitern
Neue Features werden über eine neue Manager-Klasse in `manager/`, einen Einstellungsblock in `config.yml`,
Text-Keys in beiden `languages/*/messages.yml` und (falls nötig) einen Listener/Command hinzugefügt, der
den Manager aufruft. Bestehender Code muss dafür nie umgebaut werden.

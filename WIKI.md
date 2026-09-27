# Stone Chat – Wiki

Stone Chat ist ein Chat-Plugin für **Paper 26.2+** (Java 25). Es bündelt alles rund um den Chat in einem
Plugin: Chat-Format, Wortfilter, Link-Blocker, Anti-Spam, Pings, private Nachrichten, Chat-Games,
Broadcasts, automatische Nachrichten, einen Chatlog für Admins und einen Einstellungs-Editor im Spiel.

Diese Seite erklärt jedes Feature, jeden Befehl und jede Berechtigung. Sie gilt für **Version 1.1.1**.
Die Versionsgeschichte steht in der [README](README.md#features).

---

## Inhalt

- [Überblick](#überblick)
- [Installation und Update](#installation-und-update)
- [Dateien im Plugin-Ordner](#dateien-im-plugin-ordner)
- [Befehle](#befehle)
- [Berechtigungen](#berechtigungen)
- [Der Weg einer Chat-Nachricht](#der-weg-einer-chat-nachricht)
- [Features im Detail](#features-im-detail)
  - [Chat-Format und Placeholder](#chat-format-und-placeholder) ·
    [Chat-Farben](#chat-farben) ·
    [Wortfilter](#wortfilter) ·
    [Link-Blocker](#link-blocker) ·
    [Anti-Caps](#anti-caps) ·
    [Cooldown](#cooldown) ·
    [Join-Delay](#join-delay) ·
    [Maximale Nachrichtenlänge](#maximale-nachrichtenlänge)
  - [Chat-Mute](#chat-mute) ·
    [Straf-Befehle](#straf-befehle) ·
    [Pings](#pings) ·
    [Private Nachrichten](#private-nachrichten) ·
    [Ignorieren](#ignorieren) ·
    [Spieler-Einstellungsmenü](#spieler-einstellungsmenü)
  - [Chat-Games](#chat-games) ·
    [Broadcasts](#broadcasts) ·
    [Automatische Nachrichten](#automatische-nachrichten) ·
    [Chat leeren](#chat-leeren) ·
    [Chatlog](#chatlog) ·
    [Update-Checker](#update-checker) ·
    [Stille Joins und Leaves](#stille-joins-und-leaves)
- [Anzeigekanäle (notification-type)](#anzeigekanäle-notification-type)
- [Sounds](#sounds)
- [Farben und Formatierung](#farben-und-formatierung)
- [Sprachen und Texte](#sprachen-und-texte)
- [In-Game-Editor](#in-game-editor)
- [Zusammenspiel mit anderen Plugins](#zusammenspiel-mit-anderen-plugins)
- [Was einen Neustart überlebt](#was-einen-neustart-überlebt)
- [Häufige Fragen und Probleme](#häufige-fragen-und-probleme)
- [Für Entwickler](#für-entwickler)

---

## Überblick

| Bereich | Was Stone Chat macht |
|---|---|
| Aussehen | Frei gestaltbares Chat-Format mit Placeholdern, LuckPerms- und PlaceholderAPI-Unterstützung, klickbare Spielernamen mit Statistik-Hover, Chat-Farben per Menü |
| Moderation | Wortfilter (inkl. Umgehungserkennung), Link-Blocker, Anti-Caps, Cooldowns, Join-Delay, Längenlimit, globaler Chat-Mute, automatische Straf-Befehle, Chatlog |
| Kommunikation | `@Name`-Pings, private Nachrichten mit `/msg` und `/r`, Ignorieren von Spielern |
| Unterhaltung | Chat-Games (Mathe, Wörter entwirren, Schnelltippen, Quiz, Lückentext, eigene Spiele) mit Belohnungen |
| Ansagen | Broadcasts über Chat, Actionbar, Title oder Bossbar, automatische Nachrichten im Intervall, Chat leeren |
| Verwaltung | Kompletter Einstellungs-Editor im Spiel (`/sc editor`), Deutsch/Englisch, Update-Hinweise über Modrinth |

Grundprinzipien:

- **Einstellungen und Texte sind getrennt.** Schalter, Zeiten und Sounds stehen in `config.yml`, alle
  Texte, die Spieler sehen, in `languages/<sprache>/messages.yml`.
- **Updates überschreiben nichts.** Neue Optionen werden beim Start automatisch ergänzt, geänderte Werte
  bleiben erhalten.
- **Fast alles lässt sich ohne Datei-Bearbeitung einstellen**, über `/sc editor` im Spiel.

---

## Installation und Update

**Voraussetzungen:** Paper-Server ab 26.2 und Java 25. Folia wird nicht unterstützt.
Optional: [PlaceholderAPI](#zusammenspiel-mit-anderen-plugins), LuckPerms, Vault.

**Installation**

1. `StoneChat-1.1.1.jar` in den Ordner `plugins/` legen.
2. Server starten. Stone Chat legt `plugins/StoneChat/` mit allen Dateien an.
3. Einstellungen im Spiel mit `/sc editor` anpassen oder die Dateien bearbeiten und `/stonechat reload` ausführen.

**Update**

1. Alte jar durch die neue ersetzen und den Server neu starten.
2. Fehlende neue Optionen werden automatisch in die Dateien eingetragen. In der Konsole steht dann
   `Added missing options to <datei> (existing values were kept untouched).`
3. Deine eigenen Werte, Farben und Custom-Games bleiben unverändert. Gelöschte Chat-Farben und
   Custom-Games kommen **nicht** zurück.

> **Kaputte YAML-Datei?** Enthält eine Config einen Syntaxfehler (z. B. falsche Einrückung), wird sie
> **nicht** überschrieben. Stone Chat legt eine Kopie `<datei>.broken-<datum-uhrzeit>` an, meldet den
> Fehler in der Konsole und nutzt bis zur Korrektur die Standardwerte. Datei reparieren, dann
> `/stonechat reload`.

---

## Dateien im Plugin-Ordner

Alles liegt unter `plugins/StoneChat/`:

| Datei / Ordner | Inhalt | Von Hand bearbeiten? |
|---|---|---|
| `config.yml` | Alle Einstellungen: Filter, Cooldowns, Pings, Broadcast, Chat-Farben, Chatlog, automatische Nachrichten, Update-Checker | Ja |
| `chatformat.yml` | Aussehen einer öffentlichen Chat-Nachricht, Datums-/Zeitformat, klickbare Namen | Ja |
| `chatgames.yml` | Chat-Games: Intervalle, Texte, Belohnungen, die fünf eingebauten Spiele, eigene Spiele | Ja |
| `languages/en/messages.yml` | Englische Texte | Ja |
| `languages/de/messages.yml` | Deutsche Texte | Ja |
| `gui/chatcolor-gui.yml` | Titel, Items, Namen und Beschreibungen des Chat-Farben-Menüs | Ja |
| `gui/settings-gui.yml` | Titel, Items, Namen und Beschreibungen des Spieler-Einstellungsmenüs | Ja |
| `playercolors.yml` | Gewählte Chat-Farbe pro Spieler | Nein (wird vom Plugin geschrieben) |
| `ignorelist.yml` | Wer wen ignoriert | Nein (wird vom Plugin geschrieben) |
| `chatlogs/<uuid>.log` | Chatverlauf pro Spieler für `/chatlog` | Nein (wird vom Plugin geschrieben) |

`playercolors.yml` und `ignorelist.yml` werden im Hintergrund und absturzsicher gespeichert. Ist eine
davon beschädigt, wird sie als `<datei>.corrupt-<zeitstempel>` beiseitegelegt und leer neu begonnen, statt
die kaputte Datei zu überschreiben.

---

## Befehle

„Konsole" gibt an, ob der Befehl auch aus der Server-Konsole funktioniert.

| Befehl | Aliase | Berechtigung | Konsole | Was er tut |
|---|---|---|---|---|
| `/stonechat` oder `/stonechat help` | `/sc`, `/stonechatplugin` | – | ja | Zeigt die Hilfe. Wer `stonechat.admin` hat, sieht zusätzlich alle Admin-Befehle, für die er Rechte hat. |
| `/stonechat settings` | `/stonechat menu` | – | nein | Öffnet das [Spieler-Einstellungsmenü](#spieler-einstellungsmenü), wie `/chat`. |
| `/stonechat togglepings` | – | – | nein | Schaltet Ping-Benachrichtigungen für dich an oder aus (nur wenn `ping.ignorable: true`). |
| `/stonechat reload` | – | `stonechat.admin` | ja | Lädt alle Configs, Texte und GUI-Dateien neu. |
| `/stonechat editor` | `/sc editor`, `/stonechat edit` | `stonechat.admin` | nein | Öffnet den [In-Game-Editor](#in-game-editor). |
| `/stonechat checkupdate` | – | `stonechat.admin` | ja | Fragt sofort Modrinth nach einer neuen Version. Das Ergebnis steht in der Konsole. |
| `/chat` | `/chat settings`, `/chat menu` | – | nein | Öffnet das Spieler-Einstellungsmenü. |
| `/chat color` | `/chat colour` | optional, siehe [Chat-Farben](#chat-farben) | nein | Öffnet das Chat-Farben-Menü. |
| `/chat ignore …`, `/chat msg …`, `/chat r …` | – | wie `/ignore`, `/msg`, `/r` | nein | Kurzformen, die an `/ignore`, `/msg` und `/r` weitergeben. |
| `/msg <spieler> <nachricht>` | `/tell`, `/w`, `/whisper` | `stonechat.msg` | nein | Private Nachricht senden. |
| `/r <nachricht>` | `/reply` | `stonechat.msg` | nein | Auf die letzte private Nachricht antworten. |
| `/ignore <spieler>` | – | `stonechat.msg` | nein | Spieler ignorieren bzw. nicht mehr ignorieren (Umschalter). |
| `/ignore list` | – | `stonechat.msg` | nein | Zeigt, wen du ignorierst. |
| `/chatmute` | – | `stonechat.mute` | ja | Schaltet den Chat für alle stumm bzw. wieder frei. |
| `/chatclear` | `/clearchat` | `stonechat.clear` | ja | Leert den Chat aller Online-Spieler. |
| `/broadcast <chat\|actionbar\|title\|bossbar> [sekunden] <nachricht>` | – | `stonechat.broadcast` | ja | Sendet eine Ansage an alle. |
| `/chatgame start [id]` | – | `stonechat.chatgame` | ja | Startet ein bestimmtes oder ein zufälliges Chat-Game. |
| `/chatgame stop` | – | `stonechat.chatgame` | ja | Beendet die laufende Runde. |
| `/chatgame list` | – | `stonechat.chatgame` | ja | Listet alle Chat-Games und ob sie aktiv sind. |
| `/chatlog <spieler> [seite]` | – | `stonechat.chatlog` | ja | Zeigt, was ein Spieler wann im Chat geschrieben hat. Die Blätter-Links gibt es nur im Spiel. |

Befehle mit Berechtigung sind für Spieler ohne diese Berechtigung in der Tab-Vervollständigung unsichtbar.
Normale Spieler sehen also nur `/stonechat`, `/chat`, `/msg`, `/r` und `/ignore`.

---

## Berechtigungen

### Übersicht

| Berechtigung | Standard | Wofür |
|---|---|---|
| `stonechat.admin` | OP | `/stonechat reload`, `/sc editor`, `/stonechat checkupdate`, erweiterte Hilfe, Update-Hinweis beim Betreten des Servers |
| `stonechat.mute` | OP | `/chatmute` benutzen |
| `stonechat.mute.bypass` | OP | Trotz Chat-Mute schreiben und Befehle nutzen |
| `stonechat.wordfilter.notify` | OP | Hinweise erhalten, wenn jemand den Wortfilter auslöst |
| `stonechat.links.bypass` | OP | Links schreiben, obwohl der Link-Blocker aktiv ist |
| `stonechat.cooldown.bypass` | OP | Kein Chat- und Befehls-Cooldown |
| `stonechat.caps.bypass` | OP | Anti-Caps gilt nicht |
| `stonechat.joindelay.bypass` | OP | Sofort nach dem Betreten schreiben |
| `stonechat.ping.immune` | **niemand** | Kann von anderen nicht gepingt werden |
| `stonechat.ignore.immune` | **niemand** | Kann von niemandem ignoriert werden (z. B. für Teammitglieder) |
| `stonechat.msg` | **alle** | `/msg`, `/tell`, `/w`, `/whisper`, `/r`, `/ignore` |
| `stonechat.chatgame` | OP | `/chatgame` |
| `stonechat.broadcast` | OP | `/broadcast` |
| `stonechat.clear` | OP | `/chatclear` |
| `stonechat.chatlog` | OP | `/chatlog` |
| `stonechat.color.gold` | OP | Chat-Farbe „Gold" |
| `stonechat.color.red` | OP | Chat-Farben „Red" und „Dark Red" |
| `stonechat.color.rainbow` | OP | Die meisten Farbverläufe und alle Premium-Farben |

Den Update-Hinweis beim Betreten bekommen OPs immer, auch ohne `stonechat.admin`.

### Einstellbare Berechtigungen

Einige Berechtigungsnamen sind nicht fest, sondern stehen in `config.yml`. Die Standardwerte entsprechen
der Tabelle oben. Du kannst sie auf eigene Namen ändern:

| Einstellung in `config.yml` | Standard |
|---|---|
| `chat-mute.bypass-permission` | `stonechat.mute.bypass` |
| `link-blocker.bypass-permission` | `stonechat.links.bypass` |
| `ping.immune-permission` | `stonechat.ping.immune` |
| `cooldown.bypass-permission` | `stonechat.cooldown.bypass` |
| `anti-caps.bypass-permission` | `stonechat.caps.bypass` |
| `join-delay.bypass-permission` | `stonechat.joindelay.bypass` |
| `chat-color-gui.use-permission` | leer = jeder darf `/chat color` öffnen |
| `chat-color-gui.colors.<farbe>.permission` | pro Farbe, leer = frei für alle |

In `chatgames.yml` kann jedes Spiel zusätzlich eine eigene `permission` haben. Sie wird **zusätzlich**
zu `stonechat.chatgame` gebraucht, um dieses Spiel von Hand zu starten.

> **Wichtig beim Testen:** Alle Bypass-Berechtigungen stehen standardmäßig auf OP. Als OP greifen
> Cooldown, Anti-Caps, Link-Blocker, Join-Delay und Chat-Mute deshalb **nicht**. Nur der Wortfilter gilt
> für alle. Zum Testen einen Account ohne OP nutzen oder die Bypass-Rechte gezielt entziehen, z. B.
> `/lp user <name> permission set stonechat.cooldown.bypass false`.

Beispiele mit LuckPerms:

```
/lp group vip permission set stonechat.color.gold true
/lp group supporter permission set stonechat.mute true
/lp group supporter permission set stonechat.chatlog true
/lp group team permission set stonechat.ignore.immune true
```

---

## Der Weg einer Chat-Nachricht

Jede öffentliche Chat-Nachricht läuft in dieser Reihenfolge durch die Prüfungen. Die **erste** Prüfung,
die anschlägt, stoppt die Nachricht. Der Spieler bekommt dann den passenden Hinweis und Sound.

| # | Schritt | Was passiert |
|---|---|---|
| 1 | [Join-Delay](#join-delay) | Spieler ist gerade erst beigetreten → blockiert |
| 2 | [Chat-Mute](#chat-mute) | Chat ist stumm geschaltet → blockiert |
| 3 | [Maximale Länge](#maximale-nachrichtenlänge) | Nachricht zu lang → blockiert |
| 4 | [Cooldown](#cooldown) | Letzte Nachricht zu kurz her → blockiert |
| 5 | [Chat-Game](#chat-games) | Nachricht ist die richtige Antwort → Spieler gewinnt, die Antwort erscheint **nicht** im Chat |
| 6 | [Anti-Caps](#anti-caps) | Zu viele Großbuchstaben → blockiert oder in Kleinbuchstaben umgewandelt |
| 7 | [Wortfilter](#wortfilter) | Verbotenes Wort → blockiert oder mit `*` zensiert |
| 8 | [Link-Blocker](#link-blocker) | Link gefunden → blockiert |
| 9 | [Pings](#pings) | `@Name` wird hervorgehoben, der Spieler benachrichtigt |
| 10 | [Chat-Format](#chat-format-und-placeholder) | Format und [Chat-Farbe](#chat-farben) werden angewendet, die Nachricht wird verschickt |

Danach wird das Ergebnis im [Chatlog](#chatlog) gespeichert, auch bei blockierten Nachrichten.

Gut zu wissen:

- **Spieler können keine Farbcodes benutzen.** Tippt ein Spieler `&c` oder `<red>`, erscheint das
  wörtlich im Chat. So kann niemand fremde Formatierung, Klick-Aktionen oder unsichtbaren Text
  einschleusen. Farbe gibt es nur über [`/chat color`](#chat-farben).
- **Die Nachricht bleibt eine echte Spieler-Nachricht.** Stone Chat bricht die Nachricht nicht ab und
  verschickt sie neu, sondern gestaltet sie über Papers Chat-Renderer. Dadurch funktionieren das
  Ausblenden von Spielern über Minecrafts „Soziale Interaktionen", die Chat-Einstellungen der Spieler
  und Chat-Brücken wie Discord-Plugins.
- Der Chat-Cooldown zählt ab Schritt 4. Eine Nachricht, die erst danach blockiert wird (z. B. vom
  Wortfilter), startet den Cooldown trotzdem.

---

## Features im Detail

### Chat-Format und Placeholder

**Datei:** `chatformat.yml` · **Editor:** `/sc editor` → Chat Format

| Einstellung | Standard | Bedeutung |
|---|---|---|
| `format` | `"%prefix%<white>%player_name%</white>%suffix% <dark_gray>»</dark_gray> <white>%message%</white>"` | Aufbau einer Chat-Zeile |
| `use-placeholderapi` | `true` | PlaceholderAPI-Placeholder im Format auflösen, wenn PlaceholderAPI installiert ist |
| `date-format` | `"dd.MM.yyyy"` | Format für `%date%` (Java-DateTimeFormatter) |
| `time-format` | `"HH:mm"` | Format für `%time%` |
| `name-interactions.click-to-message` | `true` | Klick auf einen Namen im Chat schreibt `/msg <name> ` in die eigene Chatzeile |
| `name-interactions.hover-stats` | `true` | Maus über einen Namen zeigt Rang, Spielzeit, Tode und Kills |

**Placeholder im Format:**

| Placeholder | Ergebnis |
|---|---|
| `%player_name%` | Spielername (klickbar und mit Hover, siehe oben) |
| `%player_displayname%` | Anzeigename (z. B. von Nick-Plugins) |
| `%player_uuid%` | UUID |
| `%player_health%` / `%player_max_health%` | Aktuelle / maximale Gesundheit, gerundet |
| `%player_level%` | XP-Level |
| `%player_ping%` | Ping in ms |
| `%player_gamemode%` | `SURVIVAL`, `CREATIVE`, `ADVENTURE` oder `SPECTATOR` |
| `%world%` / `%world_players%` | Name der Welt / Spieler in dieser Welt |
| `%online%` / `%max_players%` | Spieler online / Slots |
| `%luckperms_rank%` | Primäre LuckPerms-Gruppe |
| `%luckperms_prefix%` / `%prefix%` | LuckPerms-Prefix |
| `%luckperms_suffix%` / `%suffix%` | LuckPerms-Suffix |
| `%date%` / `%time%` | Aktuelles Datum / Uhrzeit |
| `%message%` | Die Nachricht selbst, mit Chat-Farbe |

Dazu kommt jeder PlaceholderAPI-Placeholder (z. B. `%vault_prefix%`), wenn PlaceholderAPI installiert
und `use-placeholderapi: true` ist. Ohne LuckPerms bleiben die `%luckperms_*%`-, `%prefix%`- und
`%suffix%`-Placeholder einfach leer.

Die Texte im Namens-Hover stehen in `messages.yml` unter `chat-format.hover-rank`, `hover-playtime`,
`hover-deaths`, `hover-kills` und `hover-no-rank` (Text für Spieler ohne Rang).

Beispiel mit Rang in Klammern und Uhrzeit:

```yaml
format: "<dark_gray>%time%</dark_gray> <gray>[</gray>%luckperms_rank%<gray>]</gray> %prefix%%player_name% <dark_gray>»</dark_gray> %message%"
```

---

### Chat-Farben

**Befehl:** `/chat color` · **Datei:** `config.yml` → `chat-color-gui` · **Aussehen:** `gui/chatcolor-gui.yml`

Spieler wählen in einem Menü die Farbe ihrer eigenen Chat-Nachrichten. Das Menü hat drei Bereiche:
**Normal Colors**, **Gradient Colors** und **✦ Premium ✦**, je 18 Farben pro Seite. Die Wahl wird
dauerhaft in `playercolors.yml` gespeichert.

| Einstellung | Standard | Bedeutung |
|---|---|---|
| `enabled` | `true` | Menü ein/aus |
| `use-permission` | `""` | Berechtigung zum Öffnen des Menüs, leer = alle |
| `sound-select` / `sound-denied` | an | Sound beim Auswählen / bei gesperrter Farbe |
| `colors` | 64 Farben | Die Liste aller Farben (nur in der Datei bearbeitbar) |

Jede Farbe ist ein eigener Eintrag:

```yaml
chat-color-gui:
  colors:
    himmelblau:                        # interne ID, frei wählbar
      display-name: "&#3399FFHimmelblau" # Name im Menü
      color-code: "&#3399FF"           # &-Code, Hex oder MiniMessage-Tag
      material: LIGHT_BLUE_DYE         # Item im Menü
      permission: "stonechat.color.vip" # leer = frei für alle
      premium: false                   # true = erscheint unter Premium
```

- **In welchem Bereich eine Farbe erscheint:** `premium: true` → Premium. Sonst: enthält `color-code`
  `<gradient`, → Gradient. Alles andere → Normal.
- **Leerer `color-code`** macht den Eintrag zu „Zurücksetzen auf Standard". Er erscheint in allen Bereichen.
- **Berechtigung:** Eigene Berechtigungsnamen wie `stonechat.color.vip` müssen nirgends angelegt werden,
  einfach per LuckPerms vergeben. OPs haben sie automatisch.
- **Rechte verloren?** Verliert ein Spieler die Berechtigung einer Farbe, wird sie nicht mehr angewendet.
  Seine Nachrichten erscheinen wieder in der Standardfarbe.
- **Farben löschen:** Einen Eintrag aus `colors` löschen und `/stonechat reload`. Gelöschte Farben
  kommen bei Updates nicht zurück.

---

### Wortfilter

**Datei:** `config.yml` → `word-filter` · **Editor:** Word Filter

| Einstellung | Standard | Bedeutung |
|---|---|---|
| `enabled` | `true` | Filter an/aus |
| `filtered-words` | `badword1`, `badword2` | Verbotene Wörter |
| `action` | `block` | `block` = Nachricht wird nicht gesendet, `censor` = das Wort wird durch `*` ersetzt |
| `notify-admins` | `true` | Spieler mit `stonechat.wordfilter.notify` sehen den Verstoß mit der Original-Nachricht, zusätzlich steht er in der Konsole |
| `evasion-detection.enabled` | `true` | Erkennt Umgehungsversuche (siehe unten) |
| `evasion-detection.normalize-leetspeak` | `false` | Erkennt zusätzlich Leetspeak (`4`→a, `3`→e, `1`→i, `0`→o, `5`/`$`→s, `@`→a, `7`/`+`→t) |
| `punish-command` | `""` | Siehe [Straf-Befehle](#straf-befehle) |
| `notification-type` | `CHAT` | Wo der Spieler den Hinweis sieht |
| `sound` | `ENTITY_VILLAGER_NO` | Sound für den Spieler |

- Wörter werden **als ganzes Wort** und ohne Beachtung der Groß-/Kleinschreibung gefunden:
  `hans` trifft `Hans` und `HANS`, aber nicht `Hansa`.
- **Umgehungserkennung:** Findet verbotene Wörter auch, wenn jemand unsichtbare Zeichen dazwischen setzt,
  gleich aussehende Buchstaben aus anderen Alphabeten nutzt (kyrillisches `а`, griechisches `α`,
  Vollbreiten-`Ａ`) oder Akzente benutzt (`café` → `cafe`). Admins bekommen dafür einen eigenen Hinweis.
- Wird ein Wort nur über die Umgehungserkennung gefunden und steht `action` auf `censor`, wird die
  **gesamte** Nachricht durch Sterne ersetzt, weil sich die Stelle nicht sauber bestimmen lässt.
- Es gibt **keine** Bypass-Berechtigung: Der Wortfilter gilt auch für OPs.
- Die Wortliste wird in der `config.yml` gepflegt, der Editor stellt nur das Verhalten ein. Danach
  `/stonechat reload`.

---

### Link-Blocker

**Datei:** `config.yml` → `link-blocker` · **Editor:** Link Blocker

| Einstellung | Standard | Bedeutung |
|---|---|---|
| `enabled` | `true` | An/aus |
| `bypass-permission` | `stonechat.links.bypass` | Wer Links schreiben darf |
| `patterns` | `http://`, `https://`, `www.`, `.com`, `.net`, `.org`, `.de`, `.gg`, `.io`, `.to` | Textstücke, die als Link gelten |
| `punish-command` | `""` | Siehe [Straf-Befehle](#straf-befehle) |
| `notification-type` / `sound` | `CHAT` / `ENTITY_VILLAGER_NO` | Hinweis und Sound für den Spieler |

Die Muster sind einfache Textstücke, keine regulären Ausdrücke. Sie werden ohne Beachtung der
Groß-/Kleinschreibung **irgendwo** in der Nachricht gesucht. Kurze Endungen wie `.de` treffen deshalb
auch Wörter wie `datei.dev`. Bei Fehlalarmen ein Muster entfernen oder genauer machen. Die Muster werden
nur in der `config.yml` gepflegt, danach `/stonechat reload`.

---

### Anti-Caps

**Datei:** `config.yml` → `anti-caps` · **Editor:** Anti-Caps

| Einstellung | Standard | Bedeutung |
|---|---|---|
| `enabled` | `true` | An/aus |
| `max-percentage` | `60` | Erlaubter Anteil Großbuchstaben in Prozent |
| `min-length` | `5` | Kürzere Nachrichten werden nicht geprüft (z. B. „OK", „GG") |
| `mode` | `block` | `block` = Nachricht blockieren, `autocorrect` = ganze Nachricht in Kleinbuchstaben umwandeln |
| `bypass-permission` | `stonechat.caps.bypass` | Wer ausgenommen ist |
| `punish-command` | `""` | Nur im Modus `block` |
| `notification-type` / `sound` | `CHAT` / `ENTITY_VILLAGER_NO` | Nur im Modus `block` |

Gezählt werden nur Buchstaben. Zahlen, Leerzeichen und Satzzeichen verändern den Anteil nicht.

---

### Cooldown

**Datei:** `config.yml` → `cooldown` · **Editor:** Cooldown

| Einstellung | Standard | Bedeutung |
|---|---|---|
| `enabled` | `true` | An/aus |
| `chat-seconds` | `5` | Mindestabstand zwischen zwei Chat-Nachrichten |
| `command-seconds` | `3` | Mindestabstand zwischen zwei Befehlen |
| `bypass-permission` | `stonechat.cooldown.bypass` | Wer ausgenommen ist |
| `cooldown-commands` | `[]` | Nur diese Befehle (ohne `/`) haben einen Cooldown. Leer = **alle** Befehle |
| `notification-type` / `sound` | `CHAT` / `BLOCK_NOTE_BLOCK_BASS` | Hinweis „bitte warte X Sekunden" |

- Der Befehls-Cooldown ist ein gemeinsamer Timer für alle betroffenen Befehle, nicht einer pro Befehl.
- Schreibweisen mit Plugin-Namen wie `/essentials:home` zählen wie `/home`.
- `/stonechat` (mit `/sc`) und `/chatmute` haben nie einen Cooldown.

---

### Join-Delay

**Datei:** `config.yml` → `join-delay` · **Editor:** Join Delay

Neue Spieler dürfen erst `seconds` Sekunden (Standard: `5`) nach dem Betreten schreiben. Das bremst
Spam-Bots, die direkt nach dem Join Werbung posten. Ausnahme: `stonechat.joindelay.bypass`.
Hinweis und Sound: `notification-type` (`CHAT`) und `sound` (`BLOCK_NOTE_BLOCK_BASS`).

---

### Maximale Nachrichtenlänge

**Datei:** `config.yml` → `max-message-length` · **Editor:** Max Length

Nachrichten mit mehr als `max-length` Zeichen (Standard: `100`) werden blockiert. Gezählt wird der Text,
den der Spieler getippt hat. Abschaltbar mit `enabled: false`. Es gibt keine Bypass-Berechtigung.

---

### Chat-Mute

**Befehl:** `/chatmute` · **Datei:** `config.yml` → `chat-mute` · **Editor:** Chat Mute

`/chatmute` schaltet den öffentlichen Chat für alle stumm, erneutes `/chatmute` gibt ihn wieder frei.
Alle Online-Spieler sehen, wer den Chat stumm geschaltet hat.

| Einstellung | Standard | Bedeutung |
|---|---|---|
| `bypass-permission` | `stonechat.mute.bypass` | Wer trotzdem schreiben darf |
| `block-commands` | `true` | Auch Befehle sind gesperrt (außer `/chatmute` und `/stonechat`) |
| `notification-type` | `CHAT` | Kanal für die Ansage und den Hinweis an blockierte Spieler |
| `sound-toggle` | `BLOCK_ANVIL_LAND` | Sound für alle beim Umschalten |
| `sound-blocked` | `ENTITY_VILLAGER_NO` | Sound für einen Spieler, der während des Mutes schreibt |

Der Mute gilt bis zum Neustart. Nach einem Neustart ist der Chat wieder offen.

---

### Straf-Befehle

Wortfilter, Link-Blocker und Anti-Caps haben jeweils eine Einstellung `punish-command`. Ist sie
ausgefüllt, führt die Konsole diesen Befehl bei jedem Verstoß im Hintergrund aus. `%player%` wird durch
den Spielernamen ersetzt. Leer lassen schaltet es ab.

```yaml
word-filter:
  punish-command: "mute %player% 10m Wortfilter"   # braucht ein Mute-Plugin
link-blocker:
  punish-command: "warn %player% Bitte keine Links" # braucht ein Warn-Plugin
```

Beim Anti-Caps-Modus `autocorrect` wird kein Straf-Befehl ausgeführt.

---

### Pings

**Datei:** `config.yml` → `ping` · **Editor:** Ping

Schreibt jemand `@Name`, wird der Name hervorgehoben und der Spieler bekommt einen Hinweis mit Sound.

| Einstellung | Standard | Bedeutung |
|---|---|---|
| `enabled` | `true` | An/aus |
| `trigger-symbol` | `"@"` | Zeichen vor dem Namen |
| `highlight-template` | `"<yellow><bold>%player%</bold></yellow>"` | Aussehen des erwähnten Namens. `%player%` = Symbol + Name, z. B. `@Steve` |
| `allow-self-ping` | `false` | Darf man sich selbst pingen? |
| `immune-permission` | `stonechat.ping.immune` | Wer nicht gepingt werden kann |
| `ignorable` | `true` | Spieler dürfen Ping-Hinweise mit `/stonechat togglepings` oder im Einstellungsmenü abschalten |
| `notification-type` / `sound` | `CHAT` / `ENTITY_EXPERIENCE_ORB_PICKUP` | Hinweis an den gepingten Spieler |

- Erkannt werden Namen von **Online-Spielern** (2–16 Zeichen, Groß-/Kleinschreibung egal).
- Wird ein Spieler mehrfach in derselben Nachricht erwähnt, bekommt er trotzdem nur einen Hinweis.
- Hat ein Spieler Pings abgeschaltet oder ignoriert er den Absender, wird sein Name weiterhin
  hervorgehoben, er bekommt aber keinen Hinweis und keinen Sound.
- Ist der Spieler immun (`stonechat.ping.immune`), wird sein Name gar nicht hervorgehoben.

Beispiel für ein anderes Aussehen:

```yaml
highlight-template: "<gray>[</gray><gradient:#55A8FE:#00C9FF>%player%</gradient><gray>]</gray>"
```

---

### Private Nachrichten

**Befehle:** `/msg`, `/tell`, `/w`, `/whisper`, `/r`, `/reply` · **Datei:** `config.yml` → `private-messages`

Stone Chat ersetzt die Vanilla-Flüsterbefehle durch eigene, formatierbare Nachrichten.

- `/msg <spieler> <nachricht>` schreibt einem Online-Spieler.
- `/r <nachricht>` antwortet der Person, der du zuletzt geschrieben hast oder die dir zuletzt geschrieben
  hat. Beim Verlassen des Servers wird das vergessen.
- Fährt man mit der Maus über eine empfangene Nachricht, erscheint ein Antwort-Hinweis. Ein Klick
  schreibt `/r ` in die Chatzeile.
- Der Empfänger hört einen Sound (`private-messages.sound`, Standard `BLOCK_NOTE_BLOCK_BELL`).
- Ignoriert der Empfänger dich, kommt die Nachricht nicht an und du bekommst einen Hinweis.

Texte in `messages.yml` → `private-message`: `sender-format`, `receiver-format` (Platzhalter `%player%`
und `%message%`) und `reply-hover`.

Gut zu wissen:

- Mit `private-messages.enabled: false` antworten `/msg` und `/r` mit „Unbekannter Unterbefehl". Die
  Vanilla-Befehle kommen dadurch **nicht** zurück, siehe [Befehlskonflikte](#zusammenspiel-mit-anderen-plugins).
- Private Nachrichten werden derzeit **nicht** vom Wortfilter, Link-Blocker oder Anti-Caps geprüft.
- Während eines Chat-Mutes mit `block-commands: true` ist auch `/msg` gesperrt. Der Befehls-Cooldown gilt
  auch für `/msg`.

---

### Ignorieren

**Befehle:** `/ignore <spieler>`, `/ignore list` · **Menü:** `/chat` → Manage Ignored Players

Ignorierst du jemanden, bekommst du von dieser Person **keine privaten Nachrichten und keine
Ping-Hinweise** mehr. Erneutes `/ignore <spieler>` hebt das auf. Die Liste wird in `ignorelist.yml`
gespeichert und bleibt nach Neustarts erhalten.

- Hinzufügen per Befehl geht nur bei Spielern, die gerade online sind. Entfernen geht auch über das Menü.
- Spieler mit `stonechat.ignore.immune` können nicht ignoriert werden.
- **Öffentliche Chat-Nachrichten** blendet `/ignore` nicht aus. Dafür hat Minecraft selbst eine Funktion:
  Im Menü „Soziale Interaktionen" (Pause-Menü) lassen sich einzelne Spieler im Chat verbergen.

---

### Spieler-Einstellungsmenü

**Befehl:** `/chat` oder `/stonechat settings` · **Aussehen:** `gui/settings-gui.yml`

Ein Menü für alle Spieler, ohne Berechtigung:

- **Receive Pings:** Ping-Hinweise an/aus (wie `/stonechat togglepings`).
- **Chat Color:** Öffnet das [Chat-Farben-Menü](#chat-farben).
- **Manage Ignored Players:**
  - **My Ignore List:** alle ignorierten Spieler als Köpfe, ein Klick entfernt sie.
  - **Ignore Someone:** alle Online-Spieler als Köpfe, mit Namenssuche über den Chat, ein Klick ignoriert.

Alle Titel, Items, Namen und Beschreibungen lassen sich in `gui/settings-gui.yml` ändern.

---

### Chat-Games

**Befehl:** `/chatgame` · **Datei:** `chatgames.yml` · **Editor:** Chat Games

Das Plugin stellt eine Frage im Chat. Wer zuerst richtig antwortet, gewinnt. Die richtige Antwort erscheint
nicht im Chat, stattdessen verkündet Stone Chat den Gewinner.

**Ablauf einer Runde**

1. Eine Runde startet automatisch alle `interval-seconds` (Standard: 300 s = 5 Minuten) oder von Hand mit
   `/chatgame start [id]`.
2. Die Frage wird angekündigt (`start-message`).
3. Ist nach `hint.after-seconds` (Standard: 20 s) noch niemand richtig, kommt ein Hinweis: erster
   Buchstabe und Länge der Antwort.
4. Die erste richtige Antwort gewinnt (`win-message`) und bekommt die Belohnung.
5. Antwortet niemand innerhalb der Antwortzeit, kommt die `timeout-message`.

**Globale Einstellungen** (`chat-games`)

| Einstellung | Standard | Bedeutung |
|---|---|---|
| `enabled` | `true` | Hauptschalter für alle Chat-Games |
| `interval-seconds` | `300` | Abstand der automatischen Runden |
| `answer-time-seconds` | `60` | Antwortzeit, wenn ein Spiel keine eigene hat |
| `cooldown-after-game-seconds` | `0` | Pause nach einer Runde, bevor die nächste starten darf |
| `min-players-online` | `1` | Automatische Runden nur ab so vielen Spielern (gilt nicht für `/chatgame start`) |
| `hint.enabled` / `hint.after-seconds` | `true` / `20` | Hinweis an/aus und wann er kommt |
| `notification-type` | `CHAT` | Kanal für alle Chat-Game-Ansagen |
| `broadcast.*` | – | Texte: `prefix`, `start-message` (`%question%`), `win-message` (`%player%`, `%answer%`), `timeout-message`, `stop-message` (`%player%`), `hint-message` (`%firstletter%`, `%length%`) |
| `default-reward` | siehe unten | Belohnung für alle Spiele ohne eigene |

**Belohnung** (`default-reward` oder pro Spiel unter `reward` mit `override: true`)

| Einstellung | Bedeutung |
|---|---|
| `console-commands` | Befehle, die die Konsole ausführt. `%player%` = Gewinner |
| `vault-money` | Geld über Vault. Braucht Vault und ein Economy-Plugin. `0` = aus |
| `sound` | Sound für den Gewinner (`enabled`, `name`, `volume`, `pitch`) |
| `particle` | Partikel am Gewinner (`enabled`, `name`, `count`). Jeder Partikel ohne Zusatzdaten, z. B. `TOTEM_OF_UNDYING`, `HAPPY_VILLAGER`, `FIREWORK`, `HEART` |

**Die fünf eingebauten Spiele** (`default-games`)

| ID | Typ | Was der Spieler tun muss | Eigene Einstellungen |
|---|---|---|---|
| `math_solver` | `MATH` | Eine Rechenaufgabe lösen | `min-number`, `max-number`, `operators` (`+`, `-`, `*`, `/`). Ginge `/` nicht glatt auf, wird daraus `*`, die Lösung ist immer eine ganze Zahl |
| `unscramble_word` | `UNSCRAMBLE` | Ein durcheinandergewürfeltes Wort erkennen (`e-l-p-p-a` → `apple`) | `words` |
| `fast_typing` | `FAST_TYPING` | Einen Text als Erster genau abtippen | `phrases` |
| `trivia_quiz` | `TRIVIA` | Eine Quizfrage beantworten | `questions` (je `question` und eine Liste `answers`) |
| `fill_blanks` | `FILL_BLANKS` | Ein Wort mit Lücken vervollständigen | `blank-percentage` (10–90), `words` |

**Einstellungen, die jedes Spiel hat**

| Einstellung | Bedeutung |
|---|---|
| `enabled` | Spiel an/aus |
| `duration-seconds` | Eigene Antwortzeit, `0` = globale Antwortzeit |
| `permission` | Zusätzliche Berechtigung, um genau dieses Spiel von Hand zu starten |
| `case-sensitive` | Groß-/Kleinschreibung der Antwort beachten |
| `min-players-online` | Mindestspieler für dieses Spiel, `0` = egal |
| `hint-enabled` | Hinweise für dieses Spiel |
| `notification-type` | `DEFAULT` = globaler Kanal, sonst `CHAT`, `ACTIONBAR`, `TITLE` oder `BOSSBAR` |
| `reward` | Eigene Belohnung mit `override: true`, sonst gilt `default-reward` |

**Eigene Spiele** funktionieren wie das Quiz: eine Sammlung von Fragen mit erlaubten Antworten, pro Runde
wird eine zufällig gewählt. Am einfachsten legt man sie im Editor an (`/sc editor` → Chat Games →
Custom Games Management). Von Hand in `chatgames.yml`:

```yaml
custom-games:
  server_quiz:
    type: CUSTOM
    enabled: true
    duration-seconds: 0
    permission: ""
    case-sensitive: false
    min-players-online: 0
    hint-enabled: true
    notification-type: "DEFAULT"
    reward:
      override: false
    questions:
      - question: "Wie heißt unser Server?"
        answers: ["stonecraft", "stone craft"]
      - question: "In welchem Jahr wurde der Server gegründet?"
        answers: ["2024"]
```

Antworten werden ohne führende und folgende Leerzeichen verglichen. Sonderzeichen wie `&` oder `<` in
Antworten funktionieren.

---

### Broadcasts

**Befehl:** `/broadcast <chat|actionbar|title|bossbar> [sekunden] <nachricht>` · **Datei:** `config.yml` → `broadcast`

Sendet eine Ansage an alle Online-Spieler. Admins dürfen darin alle [Farben und Formatierungen](#farben-und-formatierung)
nutzen.

| Kanal | Was passiert | Ohne `[sekunden]` |
|---|---|---|
| `chat` | Nachricht im Chat, auf Wunsch mit Prefix | – |
| `actionbar` | Text über der Hotbar | 3 Sekunden |
| `title` | Großer Text in der Bildschirmmitte | `title-timing.stay-ticks` |
| `bossbar` | Leiste oben am Bildschirm, läuft ab | `broadcast.bossbar.duration-seconds` |

`[sekunden]` ist optional (1 bis 3600) und gilt nur für diesen einen Broadcast. Beispiele:

```
/broadcast chat &aServer-Neustart in 10 Minuten!
/broadcast title 5 <gradient:#ff512f:#dd2476>Event startet jetzt!</gradient>
/broadcast bossbar 60 &eDoppelte XP für eine Minute
```

| Einstellung | Standard | Bedeutung |
|---|---|---|
| `use-prefix` | `true` | `[Broadcast]`-Prefix vor Chat-Broadcasts. Der Text steht in `messages.yml` → `broadcast.prefix` |
| `bossbar.color` | `BLUE` | `PINK`, `BLUE`, `RED`, `GREEN`, `YELLOW`, `PURPLE`, `WHITE` |
| `bossbar.style` | `PROGRESS` | `PROGRESS`, `NOTCHED_6`, `NOTCHED_10`, `NOTCHED_12`, `NOTCHED_20` |
| `bossbar.duration-seconds` | `10` | Standarddauer der Bossbar |
| `sound` | aus | Sound für alle bei jedem Broadcast |
| `default-type` | `CHAT` | Kanal für „Send Broadcast Now" im Editor. `/broadcast` verlangt den Kanal immer ausdrücklich |

**Broadcast aus dem Editor:** `/sc editor` → Broadcast → Kanal bei „Display Type" wählen → „Send Broadcast
Now". Dann die Nachricht in den Chat tippen. Die Dauer ist hier immer der konfigurierte Standard. Du siehst zuerst eine **private Vorschau**. `confirm` sendet sie an
alle, `cancel` bricht ab.

---

### Automatische Nachrichten

**Datei:** `config.yml` → `auto-messages` · **Editor:** Auto Messages

Postet im festen Abstand automatisch eine Nachricht, z. B. „Komm auf unseren Discord: /discord".

| Einstellung | Standard | Bedeutung |
|---|---|---|
| `enabled` | `false` | Standardmäßig aus, damit ein Update nie von selbst anfängt zu posten |
| `interval-seconds` | `60` | Abstand zwischen zwei Nachrichten |
| `order` | `SEQUENTIAL` | `SEQUENTIAL` = der Reihe nach, `RANDOM` = zufällig (nie zweimal dieselbe hintereinander) |
| `min-players-online` | `1` | Unter dieser Spielerzahl wird nichts gepostet |
| `display-type` | `CHAT` | `CHAT`, `ACTIONBAR`, `TITLE` oder `BOSSBAR`. Außer bei `CHAT` wird nur die erste Zeile gezeigt |
| `prefix` | `[Info]` mit Farbverlauf | Vor der ersten Zeile jeder Chat-Nachricht, leer = kein Prefix |
| `sound` | aus | Sound für alle bei jeder Nachricht |
| `messages` | Discord-Beispiel | Liste der Nachrichten, jede mit einer oder mehreren Zeilen |

```yaml
auto-messages:
  enabled: true
  interval-seconds: 300
  messages:
    - lines:
        - "&7Komm auf unseren Discord: <click:open_url:'https://discord.gg/deinlink'><hover:show_text:'&7Klicken zum Öffnen'>&bdiscord.gg/deinlink</hover></click>"
    - lines:
        - "&8&m                              "
        - "&7Vote täglich mit <click:run_command:'/vote'>&e/vote</click> &7für Belohnungen!"
        - "&8&m                              "
```

Im Editor kann man Nachrichten anhängen und die letzte entfernen. Zum Umsortieren oder für mehrzeilige
Nachrichten die `config.yml` bearbeiten.

> Ein Klick auf `<click:run_command:'/discord'>` führt `/discord` aus. **Stone Chat bringt diesen Befehl
> nicht mit.** Er muss von einem anderen Plugin kommen (z. B. einem Discord-Plugin). Ohne so ein Plugin
> lieber direkt einen Link mit `<click:open_url:'…'>` verwenden, wie im Beispiel oben.

---

### Chat leeren

**Befehl:** `/chatclear` (`/clearchat`) · **Datei:** `config.yml` → `chat-clear`

Schickt jedem Online-Spieler `lines` Leerzeilen (Standard und Maximum: `100`, mehr Zeilen merkt sich der
Minecraft-Client ohnehin nicht). Danach sehen alle, wer den Chat geleert hat. Es gibt keine Ausnahme,
auch der Chat von Admins wird geleert. Kanal: `notification-type` (`CHAT`), Sound: `ENTITY_ITEM_BREAK`.

---

### Chatlog

**Befehl:** `/chatlog <spieler> [seite]` · **Berechtigung:** `stonechat.chatlog` · **Datei:** `config.yml` → `chat-log`

Zeigt, was ein Spieler wann im öffentlichen Chat geschrieben hat, mit Datum und Uhrzeit.

- Seite 1 zeigt die neuesten Einträge. Mit **« Ältere | Neuere »** unter der Liste blättert man per Klick.
- Funktioniert auch für Spieler, die gerade offline sind, solange der Server sie kennt.
- Blockierte Nachrichten stehen mit Grund drin, z. B. `[Blockiert: Wortfilter]`, `[Blockiert: Link]`,
  `[Blockiert: Großschreibung]`, `[Blockiert: Chat stumm]`, `[Blockiert: Cooldown]`,
  `[Blockiert: Join-Delay]` oder `[Blockiert: zu lang]`. Außerdem markiert: `[Zensiert]` und
  `[Chat-Game-Antwort]`. Gespeichert wird immer der Text, den der Spieler **getippt** hat, auch bei
  zensierten Nachrichten.
- Nur der öffentliche Chat wird geloggt, keine privaten Nachrichten und keine Befehle.

| Einstellung | Standard | Bedeutung |
|---|---|---|
| `enabled` | `true` | An/aus |
| `log-blocked-messages` | `true` | Auch blockierte Nachrichten speichern |
| `retention-days` | `30` | Ältere Einträge werden automatisch gelöscht, `0` = für immer behalten |
| `max-entries-per-player` | `1000` | Höchstens so viele Einträge pro Spieler, die ältesten fliegen zuerst raus |
| `entries-per-page` | `10` | Einträge pro Seite |
| `date-format` | `"dd.MM.yyyy HH:mm:ss"` | Format der Zeitangabe |

Gespeichert wird pro Spieler in `chatlogs/<uuid>.log` (eine Zeile pro Nachricht, durch Tabs getrennt).
Das Schreiben passiert im Hintergrund und bremst den Server nicht. Die Texte der Anzeige stehen in
`messages.yml` → `chat-log`.

---

### Update-Checker

**Datei:** `config.yml` → `update-checker` · **Editor:** Update Checker

Prüft auf [modrinth.com/project/stone-chat](https://modrinth.com/project/stone-chat), ob es eine neuere
Version gibt: einmal beim Start und danach alle `check-interval-minutes` (Standard: 60). Das Ergebnis steht
in der Konsole. Gibt es ein Update, bekommen OPs und Spieler mit `stonechat.admin` beim Betreten des
Servers einen Hinweis, bis das Plugin aktualisiert ist. Sofort prüfen: `/stonechat checkupdate` oder im
Editor „Check Now". Abschalten mit `enabled: false`.

---

### Stille Joins und Leaves

Stone Chat entfernt immer die Meldungen „X hat das Spiel betreten/verlassen". Beitritt und Verlassen sind
komplett still. Das lässt sich nicht abschalten und betrifft auch Join-Nachrichten anderer Plugins.

---

## Anzeigekanäle (notification-type)

Viele Hinweise lassen sich auf einen von vier Kanälen legen:

| Wert | Anzeige |
|---|---|
| `CHAT` | Nachricht im Chat, mit dem Plugin-Prefix aus `messages.yml` → `general.prefix` |
| `ACTIONBAR` | Text über der Hotbar |
| `TITLE` | Title mit Subtitle in der Bildschirmmitte, Zeiten aus `title-timing` |
| `BOSSBAR` | Leiste oben am Bildschirm. Farbe, Stil und Dauer kommen aus `broadcast.bossbar`. Pro Spieler gibt es höchstens eine, ein neuer Hinweis ersetzt den alten |

In `messages.yml` hat jeder dieser Hinweise schon fertige Texte für alle Kanäle (`chat`, `actionbar`,
`title`, `subtitle`, `bossbar`). Man kann also jederzeit umschalten, ohne neue Texte zu schreiben.

| Einstellung | Betrifft |
|---|---|
| `word-filter.notification-type` | „Nachricht blockiert" |
| `chat-mute.notification-type` | Mute-Ansage und Hinweis an blockierte Spieler |
| `link-blocker.notification-type` | „Link blockiert" |
| `ping.notification-type` | Ping-Hinweis |
| `cooldown.notification-type` | „Bitte warte X Sekunden" |
| `anti-caps.notification-type` | „Zu viele Großbuchstaben" |
| `join-delay.notification-type` | „Du kannst in X Sekunden schreiben" |
| `max-message-length.notification-type` | „Nachricht zu lang" |
| `chat-clear.notification-type` | „Chat wurde geleert von X" |
| `auto-messages.display-type` | Automatische Nachrichten |
| `chatgames.yml` → `chat-games.notification-type` | Alle Chat-Game-Ansagen (pro Spiel überschreibbar) |

Die Title-Zeiten in `config.yml` → `title-timing` (in Ticks, 20 Ticks = 1 Sekunde) gelten für alle Titles:
`fade-in-ticks: 10`, `stay-ticks: 60`, `fade-out-ticks: 10`.

---

## Sounds

Jeder Sound hat denselben Aufbau:

```yaml
sound:
  enabled: true
  sound-name: "ENTITY_VILLAGER_NO"   # Name aus der Bukkit-Sound-Liste
  volume: 1.0
  pitch: 1.0
```

Gültige Namen stehen in der [Bukkit-Sound-Liste](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Sound.html).
Ist ein Name ungültig, erscheint beim Start eine Warnung in der Konsole und an dieser Stelle wird kein
Sound gespielt.

| Einstellung | Standard | Wer hört ihn, wann |
|---|---|---|
| `word-filter.sound` | `ENTITY_VILLAGER_NO` | Spieler, dessen Nachricht blockiert oder zensiert wurde |
| `chat-mute.sound-toggle` | `BLOCK_ANVIL_LAND` | Alle, wenn der Mute umgeschaltet wird |
| `chat-mute.sound-blocked` | `ENTITY_VILLAGER_NO` | Spieler, der während des Mutes schreibt |
| `link-blocker.sound` | `ENTITY_VILLAGER_NO` | Spieler, dessen Link blockiert wurde |
| `ping.sound` | `ENTITY_EXPERIENCE_ORB_PICKUP` | Gepingter Spieler |
| `cooldown.sound` | `BLOCK_NOTE_BLOCK_BASS` | Spieler im Cooldown |
| `anti-caps.sound` | `ENTITY_VILLAGER_NO` | Spieler mit zu vielen Großbuchstaben (nur Modus `block`) |
| `join-delay.sound` | `BLOCK_NOTE_BLOCK_BASS` | Spieler, der zu früh schreibt |
| `max-message-length.sound` | `ENTITY_VILLAGER_NO` | Spieler mit zu langer Nachricht |
| `broadcast.sound` | aus | Alle, bei jedem Broadcast |
| `chat-color-gui.sound-select` | `UI_BUTTON_CLICK` | Spieler, der eine Farbe wählt |
| `chat-color-gui.sound-denied` | `ENTITY_VILLAGER_NO` | Spieler, der eine gesperrte Farbe anklickt |
| `private-messages.sound` | `BLOCK_NOTE_BLOCK_BELL` | Empfänger einer privaten Nachricht |
| `chat-clear.sound` | `ENTITY_ITEM_BREAK` | Alle, wenn der Chat geleert wird |
| `auto-messages.sound` | aus | Alle, bei jeder automatischen Nachricht |
| `chatgames.yml` → `reward.sound` | `ENTITY_PLAYER_LEVELUP` | Gewinner eines Chat-Games (hier heißt der Schlüssel `name` statt `sound-name`) |

---

## Farben und Formatierung

In **allen** Texten der Config- und Sprachdateien (und in Broadcasts) lassen sich drei Schreibweisen
mischen, auch im selben Text:

| Schreibweise | Beispiel | Ergebnis |
|---|---|---|
| `&`-Codes | `&aGrün &lFett` | `&0`–`&9`, `&a`–`&f` Farben, `&l` fett, `&o` kursiv, `&n` unterstrichen, `&m` durchgestrichen, `&k` verschleiert, `&r` zurücksetzen |
| Hex-Farben | `&#FF8800Orange` | Beliebige Farbe als `&#RRGGBB` |
| MiniMessage | `<gradient:#ff0000:#0000ff>Text</gradient>` | Farbverläufe, Klicks, Hover und mehr |

Nützliche MiniMessage-Tags:

| Tag | Wirkung |
|---|---|
| `<red>`, `<#55A8FE>` | Farbe |
| `<gradient:#farbe1:#farbe2>…</gradient>` | Farbverlauf (auch mit mehr als zwei Farben) |
| `<rainbow>…</rainbow>` | Regenbogen |
| `<bold>`, `<italic>`, `<underlined>`, `<strikethrough>`, `<obfuscated>` | Stil |
| `<click:run_command:'/befehl'>…</click>` | Klick führt einen Befehl aus |
| `<click:suggest_command:'/befehl '>…</click>` | Klick schreibt den Befehl in die Chatzeile |
| `<click:open_url:'https://…'>…</click>` | Klick öffnet einen Link |
| `<hover:show_text:'Text'>…</hover>` | Text beim Drüberfahren mit der Maus |
| `<newline>` | Zeilenumbruch |

Tipp für YAML: den ganzen Text in doppelte Anführungszeichen setzen und in MiniMessage-Tags einfache
verwenden, z. B. `"<click:run_command:'/spawn'>&aZum Spawn</click>"`.

Wie oben beschrieben gilt das **nicht** für Text, den Spieler selbst in den Chat oder in `/msg` tippen.

---

## Sprachen und Texte

- Sprache einstellen: `config.yml` → `general.default-language` (`en` oder `de`), dann `/stonechat reload`.
  Neuinstallationen starten auf Englisch.
- Texte ändern: `plugins/StoneChat/languages/<sprache>/messages.yml` bearbeiten und `/stonechat reload`.
  Die Platzhalter, die ein Text kennt (z. B. `%player%`, `%seconds%`, `%max%`), stehen in der Standarddatei.
- Das Prefix vor allen Chat-Hinweisen steht in `general.prefix`.
- **Eigene Sprache:** den Ordner `languages/en` z. B. nach `languages/fr` kopieren, die Texte übersetzen,
  `default-language: fr` setzen und neu laden. Fehlende Texte werden bei eigenen Sprachen **nicht**
  automatisch ergänzt. Stattdessen erscheint dort der Schlüssel (z. B. `chat-log.usage`). Nach einem Update
  deshalb neue Schlüssel aus der englischen Datei übernehmen.

Nicht in `messages.yml` liegen:

| Texte | Wo |
|---|---|
| Chat-Game-Ansagen | `chatgames.yml` → `chat-games.broadcast` |
| Menü-Titel, Item-Namen und Beschreibungen | `gui/chatcolor-gui.yml`, `gui/settings-gui.yml` |
| Automatische Nachrichten | `config.yml` → `auto-messages` |
| Rückmeldungen von `/chatgame`, der Broadcast-Vorschau und der Admin-Editor | fest auf Englisch |

---

## In-Game-Editor

**Befehl:** `/sc editor` · **Berechtigung:** `stonechat.admin`

Ein Menü mit einer Kategorie pro Feature:

> Word Filter · Chat Mute · Link Blocker · Ping · Cooldown · Anti-Caps · Join Delay · Max Length ·
> Chat Format · Chat Games · Broadcast · Chat Color GUI · Private Messages · Chat Clear ·
> Update Checker · Chat Log · Auto Messages

Bedienung:

| Feldtyp | So ändert man ihn |
|---|---|
| Schalter (an/aus) | Klicken |
| Zahl | Linksklick = weniger, Rechtsklick = mehr. Mit Shift jeweils der zehnfache Schritt |
| Auswahl (z. B. `notification-type`) | Klicken schaltet zur nächsten Option |
| Text und Nachrichtentexte | Klicken, dann den neuen Text in den Chat tippen. `cancel` bricht ab |
| Listen (z. B. Befehle mit Cooldown, Wortlisten der Chat-Games) | Klicken, dann die komplette Liste kommagetrennt in den Chat tippen |

Jede Änderung wird **sofort gespeichert und aktiv**. `/stonechat reload` ist danach nicht nötig.

Nur in den Dateien lassen sich ändern: die verbotenen Wörter, die Link-Muster, die Liste der Chat-Farben,
die Zeile `format` in `chatformat.yml` (der Editor stellt dort nur die Optionen ein) und das Aussehen der
Menüs in `gui/`.
Unter „Chat Games" gibt es Untermenüs für die globalen Einstellungen, die fünf eingebauten Spiele und die
eigenen Spiele (anlegen mit Assistent, Fragen hinzufügen, die letzte Frage entfernen, löschen durch
Eintippen von `DELETE`). Eingebaute Spiele lassen sich abschalten, aber nicht löschen.

---

## Zusammenspiel mit anderen Plugins

Alle drei Plugins sind optional. Stone Chat läuft ohne sie.

| Plugin | Wofür |
|---|---|
| **PlaceholderAPI** | Alle PAPI-Placeholder im Chat-Format (`chatformat.yml` → `use-placeholderapi`) |
| **LuckPerms** | `%luckperms_rank%`, `%prefix%`, `%suffix%` im Format und der Rang im Namens-Hover |
| **Vault** + Economy-Plugin | Geld-Belohnungen für Chat-Games (`vault-money`) |

**Discord- und andere Chat-Brücken** bekommen öffentliche Nachrichten mit, weil Stone Chat die Nachricht
als normale Spieler-Nachricht durchlässt. Blockierte Nachrichten (Wortfilter, Link, Mute, …) werden
abgebrochen und deshalb von Brücken, die abgebrochene Nachrichten überspringen, nicht weitergeleitet.

**Befehlskonflikte:** Stone Chat registriert `/msg`, `/tell`, `/w`, `/whisper`, `/r`, `/reply`, `/ignore`,
`/broadcast`, `/chat` und `/clearchat`. Andere Plugins (z. B. EssentialsX) haben teils dieselben Befehle.
Welcher gewinnt, hängt von den Plugins ab. Die Stone-Chat-Version ist immer mit Plugin-Namen erreichbar,
z. B. `/stonechat:msg`. Fest umbiegen lässt es sich in der `commands.yml` des Servers:

```yaml
aliases:
  msg:
    - "stonechat:msg $1-"
```

Umgekehrt: Soll ein anderes Plugin die privaten Nachrichten übernehmen, dort per Alias auf dessen Befehl
zeigen, z. B. `"essentials:msg $1-"`.

---

## Was einen Neustart überlebt

| Bleibt erhalten | Wird zurückgesetzt |
|---|---|
| Gewählte Chat-Farben (`playercolors.yml`) | Chat-Mute (nach dem Neustart ist der Chat offen) |
| Ignorier-Listen (`ignorelist.yml`) | Ping an/aus pro Spieler (`/stonechat togglepings`) |
| Chatlog (`chatlogs/`) | Antwort-Ziel für `/r` |
| Alle Einstellungen und Texte | Laufende Cooldowns, Join-Delays und Chat-Game-Runden |

---

## Häufige Fragen und Probleme

**Cooldown, Anti-Caps oder der Link-Blocker greifen bei mir nicht.**
Du bist vermutlich OP. Alle Bypass-Rechte stehen standardmäßig auf OP, siehe
[Berechtigungen](#berechtigungen). Mit einem Account ohne OP testen.

**Meine Änderungen in der Datei wirken nicht.**
Nach dem Bearbeiten `/stonechat reload` ausführen. Steht in der Konsole ein Fehler mit
`could not be read and is ignored until it is fixed`, enthält die Datei einen YAML-Fehler (meist Einrückung
oder ein fehlendes Anführungszeichen). Die Datei wird nicht überschrieben, eine Kopie liegt als
`*.broken-<datum>` daneben.

**Im Plugin-Ordner liegt eine `playercolors.yml.corrupt-…` oder `ignorelist.yml.corrupt-…`.**
Die Datei war beschädigt (z. B. nach einem Absturz beim Speichern mit einer älteren Version). Stone Chat hat
sie beiseitegelegt und leer neu angefangen. Aus der Kopie lassen sich Einträge von Hand zurückholen.

**Spieler können keine Farben im Chat benutzen.**
Das ist gewollt. Chat-Farben gibt es über `/chat color`.

**Es gibt keine Join- und Leave-Nachrichten mehr.**
Ebenfalls gewollt, siehe [Stille Joins und Leaves](#stille-joins-und-leaves).

**Automatische Nachrichten erscheinen nicht.**
`auto-messages.enabled` ist standardmäßig `false`. Außerdem müssen mindestens `min-players-online` Spieler
online sein.

**Der Klick auf `/discord` in der automatischen Nachricht macht nichts.**
Stone Chat hat keinen `/discord`-Befehl. Entweder ein Plugin installieren, das ihn anbietet, oder einen Link
mit `<click:open_url:'https://…'>` benutzen, siehe [Automatische Nachrichten](#automatische-nachrichten).

**Chat-Game-Gewinner bekommen kein Geld.**
`vault-money` braucht Vault **und** ein Economy-Plugin (z. B. EssentialsX). Ohne beides wird die
Geld-Belohnung übersprungen, die anderen Belohnungen laufen normal.

**Der Belohnungs-Partikel ist nicht zu sehen.**
`particle.enabled` muss `true` sein und der Name gültig. Ältere Configs enthalten noch `TOTEM`; das wird
automatisch als `TOTEM_OF_UNDYING` erkannt. Bei einem ungültigen Namen steht eine Warnung in der Konsole.
Partikel, die Zusatzdaten brauchen (z. B. `DUST`, `ITEM`, `BLOCK`), funktionieren hier nicht.

**`/chatlog` zeigt „keine Einträge".**
Der Spieler hat seit dem Einschalten des Chatlogs nichts geschrieben, oder seine Einträge sind älter als
`retention-days`.

**Ein Chat-Game startet nicht automatisch.**
Prüfen: `chat-games.enabled`, mindestens ein Spiel mit `enabled: true`, genug Spieler online
(`min-players-online` global und pro Spiel), keine laufende Runde und `cooldown-after-game-seconds`
abgelaufen.

**Der Server hat keinen Internetzugang, der Update-Checker meldet Fehler.**
`update-checker.enabled: false` setzen.

---

## Für Entwickler

**Bauen:** JDK 25 und Maven.

```
mvn clean package
```

Das erzeugt `target/StoneChat-1.1.1.jar` und führt dabei alle automatischen Tests aus. Ohne eigene
Java-Installation baut GitHub Actions (`.github/workflows/build.yml`) die jar bei jedem Push. Sie liegt dann
im Reiter **Actions** beim jeweiligen Lauf unter **Artifacts**.

**Version:** steht nur in `pom.xml` (`<version>`) und wird beim Bauen automatisch in `plugin.yml`
übernommen.

**Tests:** `src/test/` enthält 60 Tests mit [MockBukkit](https://github.com/MockBukkit/MockBukkit), die einen
Server mit Spielern simulieren. Sie decken u. a. die Chat-Pipeline, Chat-Games, Menüs, das sichere
Speichern, das Config-Update, den Chatlog und automatische Nachrichten ab. Nur Tests ausführen:
`mvn test`.

**Projektstruktur**

```
src/main/java/dev/stonechat/plugin/
  StoneChat.java      Hauptklasse: Start, Reload, Herunterfahren
  manager/            Die Logik, ein Manager pro Feature (inkl. Editor und Menüs)
  command/            Befehle mit Tab-Vervollständigung
  listener/           Event-Listener (Chat, Befehle, Join/Quit, Menüklicks)
  chatgame/           Chat-Games: ChatGameManager, GameType, GameRound, RewardConfig
  chatgame/games/     Die Spieltypen: Math, Unscramble, FastTyping, Trivia, FillBlanks, Custom
  config/             ConfigUpdater: ergänzt fehlende Optionen, sichert kaputte Dateien
  model/              Datenklassen (MessageDisplayType, Menü-Holder)
  util/               ColorUtil, PlaceholderUtil, LuckPermsUtil, VaultEconomyUtil,
                      TextNormalizer (Umgehungserkennung), DataFiles (absturzsicheres Speichern),
                      SmallCaps (Menü-Titel)
src/main/resources/   plugin.yml, config.yml, chatformat.yml, chatgames.yml, gui/, languages/
src/test/java/        MockBukkit-Tests
```

**Erweitern**

- **Neues Feature:** Manager in `manager/`, Einstellungen in `config.yml`, Texte in **beiden**
  `languages/*/messages.yml`, bei Bedarf Befehl bzw. Listener. Neue Schlüssel landen bei bestehenden
  Installationen automatisch in den Dateien.
- **Neuer Hinweis mit wählbarem Kanal:** in `messages.yml` einen Block mit `chat`, `actionbar`, `title`,
  `subtitle` und `bossbar` anlegen, in `config.yml` ein `notification-type` ergänzen und
  `NotificationManager#dispatch` aufrufen.
- **Neuer Chat-Game-Typ:** Wert in `chatgame/GameType.java` ergänzen, Klasse in `chatgame/games/`
  (erbt von `ChatGame`, implementiert `generateRound(Random)`) und einen `case` in
  `ChatGameManager#parseGame`.
- **Chat-Nachrichten** laufen asynchron. Alles, was die Welt, Inventare oder Economy anfasst, muss per
  Scheduler auf den Main-Thread (siehe `ChatGameManager` und `NotificationManager`).
- **Dateien schreiben** immer über `DataFiles` (`writeLater` im Hintergrund, `writeAtomically` direkt),
  damit ein Absturz keine halb geschriebenen Dateien hinterlässt.

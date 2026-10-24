# REGELN FÜR DIE VERSIONIERUNG & BUILD-ERSTELLUNG

Für dieses Minecraft-Plugin trennen wir strikt zwischen Entwicklungs-Builds (Dev-Builds) und Release-Builds für Modrinth. Halte dich bei jeder Änderung an der Codebase zwingend an folgenden Workflow:

1. **Stabile Hauptversion:** Die aktuelle Hauptversionsnummer (z. B. `1.1`) darf von dir NIEMALS selbstständig geändert oder erhöht werden.
2. **Das Dev-Suffix:** Für alle laufenden Änderungen, Fehlerbehebungen und Code-Tests nutzt du ein Entwickler-Suffix. Das Format ist `[Hauptversion]-dev[Nummer]` (Beispiel: `1.1-dev1`, `1.1-dev2`).
3. **Automatisches Hochzählen:** Bei jedem neuen Build, den du generierst, zählst du *ausschließlich* die Nummer des Dev-Suffixes um +1 hoch. Aktualisiere diese Version in allen relevanten Dateien (z.B. `plugin.yml` und `build.gradle` bzw. `pom.xml`).
4. **Der Release-Befehl:** Erst wenn ich ausdrücklich den Befehl **"Baue das Release"** gebe, entfernst du das `-dev`-Suffix vollständig. Du trägst dann die reine Hauptversion (z.B. `1.1`) in die Dateien ein und erstellst die finale, saubere `.jar`-Datei.
5. **Nächster Zyklus:** Nachdem das Release gebaut wurde und neue Änderungen anstehen, fragst du mich, wie die nächste Hauptversion lauten soll, und wir starten wieder bei `-dev1`.

## So gilt das in diesem Projekt

- **Wo die Version steht:** ausschließlich in `pom.xml` unter `<version>`. `plugin.yml` übernimmt sie beim
  Bauen automatisch (`version: '${project.version}'`, Maven-Resource-Filtering), dort also nichts von Hand
  eintragen. Ein `build.gradle` gibt es nicht. Die jar heißt automatisch `target/StoneChat-<version>.jar`.
- **Aktuellen Stand ermitteln:** immer aus `pom.xml` lesen.
  - Endet die Version auf `-devN`, wird der nächste Build `-dev(N+1)`.
  - Hat sie kein Suffix, ist sie ein Release. Vor neuen Änderungen nach der nächsten Hauptversion fragen
    (Regel 5).
- **Was als Build zählt:** ein Stand, der dem Nutzer übergeben wird (jar geschickt oder Änderungen committet
  und gepusht). Reine Zwischenläufe von `mvn package` oder `mvn test` während der Arbeit zählen die Nummer
  nicht hoch.
- **Bauen:** JDK 25, `mvn -B clean package`. Das führt alle Tests aus; ein Build mit roten Tests wird nicht
  ausgeliefert.
- **Release ("Baue das Release"):** `-devN` aus `pom.xml` entfernen, `mvn -B clean package`, prüfen, dass
  `plugin.yml` in der jar die reine Hauptversion trägt, die jar an den Nutzer schicken, committen und pushen.

# Open Pixel Dungeon

Fork von [Shattered Pixel Dungeon](https://github.com/00-Evan/shattered-pixel-dungeon)
(00-Evan), das wiederum auf Watabous Pixel Dungeon basiert. Java + libGDX, GPLv3.
Ziel des Forks: eine saubere, erweiterbare Basis für eigene Inhalte.

## Bauen, Starten, Testen

Gradle-Wrapper benutzen (`gradlew.bat` unter Windows, `./gradlew` sonst).

    gradlew desktop:debug        # Spiel starten (Entwicklungslauf)
    gradlew desktop:release      # ausführbares Jar bauen
    gradlew android:assembleDebug
    gradlew build                # alles kompilieren

Automatisierte Tests laufen headless (kein Android/Desktop-Client nötig), siehe
`docs/testing.md`:

    gradlew :core:test

Java 11 Quellkompatibilität (`appJavaCompatibility` in `build.gradle`). Der Desktop-
Release lädt sich ein JDK 17 herunter (siehe `desktop/build.gradle`).

## Modulübersicht

| Modul | Zweck |
|---|---|
| `core/` | Das gesamte Spiel. 1187 Dateien. Hier passiert praktisch alles. |
| `SPD-classes/` | Watabous Engine-Schicht: Rendering (`noosa`, `glwrap`), Input, Utils (`Bundle`, `Random`, `PathFinder`). Möglichst nicht anfassen. |
| `android/`, `desktop/`, `ios/` | Nur Launcher + Plattform-Support. |
| `services/` | Update-Check und News-Feed. Für einen Fork meist auf die `debug`-Varianten umstellen. |
| `core/src/main/assets/` | Grafiken, Sounds, und `messages/` = alle Texte. |

Wichtige Pakete in `core/.../shatteredpixeldungeon/`:

- `Dungeon.java` – globaler Spielzustand (statisch): Held, aktuelles Level, Tiefe,
  Seed, Speichern/Laden. Wird von ~1800 Stellen referenziert.
- `actors/` – `Char` → `Hero` / `Mob`; `buffs/` (Statuseffekte), `hero/Talent.java`.
- `items/` – alle Gegenstände. `Generator.java` ist die zentrale Loot-Tabelle.
- `levels/` – Levelklassen pro Region, `rooms/` (Raumtypen), `builders/`,
  `painters/`, `traps/`, `Terrain.java` (Kachel-IDs). `RegionDefinition.java`
  ist die zentrale Regionen-Tabelle (Etage 1–26, s. Fallstrick #7);
  `Region0Level.java` ist der eigenständige, raumlose Platzhalter für Etage 0
  ("Oberfläche", noch ohne echten Inhalt – siehe `docs/depth0-implementation.md`).
- `scenes/` – Bildschirme. `GameScene.java` ist der Spielbildschirm und ebenfalls
  über statische Methoden global erreichbar.
- `journal/` – `Bestiary`, `Catalog`, `Document`: Registrierungslisten für Inhalte.
- `sprites/` – `ItemSpriteSheet.java` bindet Items an Pixelpositionen im Sprite-Atlas.
- `ui/changelist/` – 576 KB Shattered-Changelogs. Reiner Ballast für diesen Fork.

## Bekannte Fallstricke

**1. Klassennamen stehen im Savegame.** `Bundle` speichert den vollqualifizierten
Klassennamen (`Bundle.java:369`). Eine Klasse umzubenennen oder in ein anderes Package
zu verschieben macht sie beim Laden unauffindbar: das Objekt verschwindet still aus
dem Spielstand und der Spieler bekommt einen Absturzbericht-Dialog.
Gegenmaßnahme: `Bundle.addAlias(Class, String)`, alte Klassennamen auf neue Klassen
ummappen. Alle Aufrufe gehören nach `ShatteredPixelDungeon.registerBundleAliases()`
(aus dem Konstruktor extrahiert, damit die Registrierung ohne volle `Game`-Instanz
testbar ist – siehe `BundleAliasTest`). Aktuell dort ein einziger Eintrag
(WornKey/pre-v3.3.0 SkeletonKey); bei jeder künftigen Umbenennung einer `Bundlable`-
Klasse dort einen weiteren `addAlias`-Aufruf ergänzen, nicht danach vergessen.

**2. Enum-Konstantennamen stehen ebenfalls im Savegame.** Betrifft u. a.
`Dungeon.LimitedDrops`, `Talent`, `Badges.Badge`, `Generator.Category`, `Level.Feeling`.
Umbenennen setzt den Wert beim Laden still zurück (oder auf die erste Konstante).
`Talent` hat mit `renamedTalents`/`removedTalents` bereits ein Migrationsmuster —
das ist die Vorlage für alles Weitere.

**3. Übersetzungs-Keys sind Klassennamen.** `Messages.get(this, "name")` baut den Key
aus `getClass().getName()` minus Package-Präfix, kleingeschrieben
(`Messages.java:130`). Eine Umbenennung erfordert dieselbe Umbenennung in den
`.properties`-Dateien. Gepflegt werden nur `*.properties` (Englisch) und
`*_de.properties`. Andere Sprachen fallen bei fehlendem Key auf Englisch zurück.

**4. Terrain-IDs sind rohe ints im Savegame.** `Terrain.java`, `Level.map` ist `int[]`.
IDs wurden historisch vergeben (nicht lückenlos: 31, 37, 38 wurden nachträglich
angehängt). Obergrenze ist 256 (`Terrain.flags = new int[256]`). Bestehende Zahlen
niemals ändern, neue nur an freien Stellen ergänzen.

**5. Parallele Arrays ohne Namensbindung (für `Generator` behoben, `StandardRoom` noch offen).**
`Generator.Category` deklarierte Item-Klasse und Gewicht(e) früher in zwei separaten,
von Hand parallel gepflegten Literalen (`classes[]` neben `defaultProbs[]`) — genau das
führte zum bekannten WEP_T3-Bug (T3 verwendet versehentlich `WEP_T1.defaultProbs`, siehe
Kommentar bei `WEP_T3.probs` in `Generator.java`; bewusst unverändert gelassen, da eine
Verhaltensänderung eine eigene Entscheidung braucht). Seit der Umstellung auf
`Generator.Category.ItemEntry` (`entry(Klasse, Gewicht[, Gewicht2])`, kombiniert über
`setEntries()`/`setEntriesWithSecondDeck()`/`setEntriesNoDeck()`) steht Klasse und Gewicht
in einer Zeile; `classes`/`probs`/`defaultProbs`/`defaultProbs2`/`defaultProbsTotal` bleiben
als daraus abgeleitete Felder bestehen, nur noch für lesende Aufrufer außerhalb von
`Generator.java` (`Catalog`, `ItemStatusHandler`-Konstruktion in `Potion`/`Scroll`/`Ring`,
`Succubus`/`Scorpio`-Loot, `QuickRecipe`, `CustomNoteButton`). Wo Aufrufer Klasse und Gewicht
gemeinsam auswerten mussten (`UnstableSpellbook`, `CrystalPathRoom`), lesen sie jetzt
`Category.<NAME>.entries` (`ItemEntry[]`) statt zwei Arrays über einen Index zu koppeln.
Das Speicherformat (`Generator.storeInBundle`/`restoreFromBundle`) ist entsprechend
namensbasiert (Klassenname als Bundle-Key je Item, Enum-Name als Key je Kategorie) statt
positionsbasiert; es gibt keinen Migrationscode für alte Spielstände, da es noch keine gibt.
`StandardRoom` hält weiterhin eine Raumliste und 21 `chances`-Arrays mit je 35 Slots
positionsgebunden — dieselbe Fehlerklasse, noch nicht angegangen. Ein Eintrag in der Mitte
verschiebt dort alles Nachfolgende — der Compiler merkt davon nichts.

**6. Neue Inhalte müssen an vielen Stellen registriert werden.** Ein neues Item braucht
typischerweise: Klasse, `Generator`, `ItemSpriteSheet`, `journal/Catalog`, ggf.
`items/Recipe`, plus Texte in zwei `.properties`-Dateien. Nichts davon erzwingt der
Compiler.

**7. Etagen 1–26 sind die Haupt-Kapitel, über `RegionDefinition[]` verwaltet.**
`levels/RegionDefinition.java` bündelt pro Region (Sewers/Prison/Caves/City/Halls,
je `firstDepth`+`floorCount=5`) die reguläre und die Boss-Levelklasse, das
Regions-Lore-Dokument und das Ladebildschirm-Splash-Bild; `regionOf(depth)` ist die
zentrale Lookup-Funktion. `Dungeon.newLevel()`, `Dungeon.bossLevel()` und
`RegularLevel`s Regions-Dokument-Auswahl lesen alle aus dieser Tabelle statt aus
eigenen `switch(depth)`-Blöcken – Änderungen an der Etagen-/Regionsstruktur gehören
dort hin. Etage 26 (`LastLevel`) ist bewusst KEIN Tabelleneintrag (kein 5-Etagen-Block,
kein Boss, kein Lore-Dokument) und bleibt ein expliziter Sonderfall in `newLevel()`.
`ShopRoom.generateItems()` wurde bewusst NICHT auf die Tabelle umgestellt – der
Imp-Shop auf der City-Boss-Etage nutzt einen abweichenden (höheren) Tier als der
reguläre City-Regionsshop, lässt sich nicht verlustfrei auf "ein Wert pro Region"
abbilden (Details: `docs/depth0-implementation.md`, Segment 3).
Eigenständig und UNVERÄNDERT davon: `StandardRoom.chances[27]` (7 gröbere Brackets),
`MobRegistry.BRACKET_DEPTHS` (20 feinere Brackets), `Generator.floorSetTierProbs` –
bewusst eigene, feinere Tabellen, nicht durch `RegionDefinition` ersetzt. Dazu weiterhin
~290 Stellen mit rohem `Dungeon.depth` und verstreute `depth/5`-Kapitelrechnung
außerhalb der migrierten Stellen.

Seit `docs/depth0-implementation.md` existiert zusätzlich **Etage 0** als eigene,
eigenständige Region ("Oberfläche", `Region0Level.java`, `RegionDefinition.REGIONS[0]`)
– kein `RegularLevel`/`StandardRoom`-Aufbau, kein Boss, kein Lore-Dokument, noch ohne
echten Inhalt (Häuser/NPCs/Handel = Fork-2-Territorium). Über den normalen Spielfluss
aktuell nicht erreichbar (Etage 1s Aufstiegsfeld führt weiterhin zum Amulett-Sieg-
Bildschirm, nicht zu Etage 0) – nur direkt ansteuerbar (`Dungeon.depth=0`).

**8. -1 ist die durchgängige Sentinel-Konvention für "keine Etage gesetzt"**
(`LloydsBeacon.returnDepth`, `BeaconOfReturning.returnDepth`, `Bones.depth`,
`Dungeon.depth` in `loadGame()`, `Statistics.deepestFloor`/`highestAscent`). Seit
Etage 0 eine echte, ladbare Etage ist, steht `0` dafür NICHT mehr zur Verfügung –
neue "kein Wert"-Zustände immer auf `-1` legen, nie auf `0`. `Bones.java` trennt
zusätzlich "noch nicht von der Platte gelesen" (`loaded`, `boolean`) und "an diesem
Ort bereits abgeholt" (`depleted`, `boolean`) in eigene Felder statt sie im
`depth`-Wert zu kodieren – Vorlage, falls an anderer Stelle ein ähnlicher
Drei-Zustände-in-einem-Feld-Fall auftaucht.

**9. `Generator.random()` ist nur reproduzierbar innerhalb eines geseedeten RNG-Blocks.**
`Dungeon.init()` wirft den geseedeten Zufallsgenerator direkt nach `Generator.fullReset()`
wieder weg (`Random.resetGenerators()`) und arbeitet danach mit echtem Zufall weiter —
Absicht, damit z. B. Start-Flüche nicht aus dem Seed ablesbar sind. Reproduzierbar wird
Item-Loot erst wieder, wenn man wie `Level.create()` selbst einen Seed pusht
(`Random.pushGenerator(Dungeon.seedForDepth(...))`). Details: `docs/testing.md`.

## Arbeitsregeln

- **Umbenennung und Logikänderung nie im selben Commit.** Erst reine Umbenennung
  (inkl. `.properties`-Keys und ggf. Bundle-Alias), separat committen, dann Logik.
  Sonst ist im Diff nicht mehr zu sehen, was sich am Verhalten geändert hat.
- **Spielbalance nur auf ausdrückliche Ansage ändern.** Wahrscheinlichkeiten,
  Schadenswerte, Drop-Raten, Tier-Zuordnungen und Spawn-Tabellen bleiben beim
  Refactoring bitweise identisch, solange nichts anderes vereinbart ist. Im Zweifel
  nachfragen statt „nebenbei glattziehen".
- **Vor jeder Umbenennung prüfen:** Ist die Klasse `Bundlable`? Ist es eine
  Enum-Konstante, die gespeichert wird? Hat sie Einträge in `messages/`?
- Beim Ändern von `Generator`, `StandardRoom.chances` oder ähnlichen parallelen
  Arrays: Länge und Reihenfolge beider Seiten gegenprüfen.
- Details und größere Umbauvorhaben gehören in eigene Dokumente unter `docs/`,
  nicht in diese Datei.
- **Codeblöcke in `docs/*.md` nur mit Sprach-Tag (` ```java `), wenn der Inhalt
  eigenständig kompilierbar wäre.** Nicht-kompilierbare Fragmente, Pseudocode
  oder Diff-artige Schnipsel (Variablen ohne Deklaration, abgeschnittene
  Methodenrümpfe) bekommen einen Codeblock ohne Sprach-Tag (` ``` ` statt
  ` ```java `). Grund: Manche IDEs syntax-/semantik-prüfen sprach-getaggte
  Codeblöcke in Markdown-Dateien und markieren dann in reiner Doku-Prosa
  Fehler, die dort nicht hingehören.

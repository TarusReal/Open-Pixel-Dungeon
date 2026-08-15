# depth=0 Implementation Log

Umsetzung der in `docs/depth0-findings.md` getroffenen Entscheidungen. Fork-1-Ziel:
depth=0 kann existieren, laden und verlassen werden, ohne zu crashen — kein
Fork-2-Inhalt (keine Häuser, NPCs, Handel, Item-Platzierung). Segmente werden
nacheinander umgesetzt und hier angehängt.

---

## Segment 1 — Sentinel-Vereinheitlichung (0 → -1)

### Geänderte Dateien

1. **`levels/features/LevelTransition.java:82`** — `case SURFACE: destDepth = 0;`
   → `destDepth = -1;`. Verifiziert vor der Änderung, dass `destDepth` für
   `Type.SURFACE`-Übergänge **nie gelesen wird**: `SewerLevel.activateTransition()`
   behandelt `Type.SURFACE` als eigenen Zweig, der direkt `Game.switchScene(
   SurfaceScene.class, ...)` aufruft (Amulett-Sieg-Bildschirm), nicht
   `InterlevelScene`. Alle Leser von `destDepth` (`InterlevelScene.java:142,158,
   659,703`) laufen ausschließlich über `InterlevelScene`. Änderung ist damit
   reine Konsistenzkorrektur an totem Datenfeld, kein Verhaltensrisiko.

   **Wichtige Klarstellung, die die Analyse in `depth0-findings.md` nicht explizit
   gemacht hat:** `LevelTransition.Type.SURFACE` (das mit "Oberfläche
   verlassen mit Amulett" assoziierte Sieg-Ende) ist ein **anderes Konzept**
   als die neue Region 0 ("Oberfläche" als begehbarer Hub, Segment 3). Beide
   heißen zufällig "Surface", referenzieren aber unterschiedliche Spielmechaniken
   (Sieg-Bildschirm vs. begehbare Etage). Keine Code-Kollision zwischen beiden,
   aber Namensverwechslung ist naheliegend — bei der Benennung der Region-0-
   Levelklasse in Segment 3 auf Abgrenzung achten.

2. **`Statistics.java:81-82`** (`reset()`) — `deepestFloor = 0;` /
   `highestAscent = 0;` → `= -1;` für beide.

3. **`Rankings.java:105`** — `if (Statistics.highestAscent == 0)` →
   `== -1`.

4. **`windows/WndRanking.java:235`** — dieselbe Änderung, `== 0` → `== -1`
   (zeigt Live-`Statistics`-Felder des gerade beendeten Laufs an, exakt
   dieselbe Semantik wie `Rankings.java:105`, daher identischer Fix).

### NICHT geändert — `Bones.java` (gemeldet statt repariert)

`Bones.java:167`s `if (depth > 0)`-Guard wurde in `depth0-findings.md` als
einer der zu vereinheitlichenden 0-Sentinel-Fundorte gelistet. Bei der
Umsetzung zeigte sich: **die Konvertierung ist hier nicht mechanisch möglich**,
weil `Bones.java`s statisches `depth`-Feld intern **zwei verschiedene
Sonderwerte** trägt, die `depth0-findings.md` nicht als getrennte Zustände
unterschieden hatte:

- **Zustand A** (`Bones.java:50`, bereits `-1`): "Bones-Datei wurde in dieser
  Prozess-Laufzeit noch nicht von der Festplatte gelesen." Geprüft in
  `get()` (Zeile 160) als Bedingung dafür, `bones.dat` überhaupt zu laden.
- **Zustand B** (`Bones.java:190,196`, aktuell `0`): "Bones-Datei wurde
  gelesen und **an diesem Ort bereits abgeholt**" — geschrieben, nachdem
  `lootAtCurLevel()` (Zeile 187) das Loot ausgezahlt hat, um erneutes
  Abholen zu verhindern.

Beide Zustände sind heute unterscheidbar, weil `depth==0` als reale Etage nie
vorkommt (Etagen starten bei 1) — Zustand B "funktioniert", indem er absichtlich
einen für echte Spielzüge unerreichbaren Wert besetzt.

**Zwei unabhängige Probleme, die eine einzelne 0→-1-Umbenennung nicht löst:**

1. Würde man **nur** Zustand B auf `-1` ummünzen (Zeilen 190/196:
   `depth = -1` statt `0`), kollidiert das mit Zustand A: `get()`s äußere
   Prüfung `if (depth == -1)` (Zeile 160) würde nach dem Schreiben von
   Zustand B beim nächsten Aufruf erneut "noch nicht gelesen" annehmen und
   `bones.dat` **erneut von der Platte laden**, dabei wieder `depth = -1`
   aus der soeben selbst geschriebenen leeren Datei lesen (Zeile 165-167),
   und über den rekursiven Aufruf `return get();` (Zeile 180) **in eine
   Endlosrekursion laufen** (StackOverflowError) — ein neuer, durch die
   Umbenennung selbst eingeführter Fehler, kein bestehender.
2. Selbst wenn Zustand B einen eigenen, von `-1` verschiedenen dritten
   Sentinel bekäme (z. B. `-2`) statt der beiden zu verschmelzen, bleibt ein
   **zweites, in `depth0-findings.md` nicht dokumentiertes Problem**: sobald
   Region 0 real begehbar ist (Segment 3) und ein Held dort sterben *kann*,
   vergleicht `lootAtCurLevel()` (Zeile 261-268) für den Hauptpfad
   (`branch==0`) exakt `depth == Dungeon.depth`. Ein Zustand-B-Marker mit
   Wert `0` (heutiger Code, unverändert) sieht für einen späteren Besuch
   von *echter* Etage 0 identisch aus wie "hier liegen Bones" — der Marker
   selbst würde als (leerer, aber `heroClass`/`item` aus einem alten,
   inzwischen längst überholten In-Memory-Zustand tragender) Fund
   interpretiert. Das ist strukturell derselbe Kollisionstyp wie die bereits
   bekannten Funde, aber an einer Stelle, die eine reine Sentinel-Umbenennung
   nicht heilt — es braucht eine dritte, von echten Etagen *und* von "noch
   nicht gelesen" unterscheidbare Markierung, oder einen separaten
   boolean-Flag statt der Wiederverwendung von `depth` für drei Zustände.

**Konsequenz:** `Bones.java` bleibt in diesem Segment unverändert. Der
Guard `depth > 0` bleibt bewusst so bestehen (funktioniert für Etagen 1-26
unverändert korrekt) und Zustand B bleibt `0`. Das bereits bekannte
Symptom aus `depth0-findings.md` Kategorie E Punkt 1 (echte Bones auf
Etage 0 laden `item`/`heroClass` nicht) **besteht also weiterhin fort** —
zusammen mit dem hier neu gefundenen Wiederholungsabhol-Risiko. Beides
braucht eine eigene Entscheidung (dritter Sentinel-Wert vs. separates
Boolean-Feld vs. bewusstes Akzeptieren, dass Bones auf Etage 0 nicht
unterstützt werden) — nicht in diesem Segment vorweggenommen.

### Verifikation

Neuer Test: `core/src/test/java/.../test/Depth0SentinelTest.java`, 3 Fälle:

1. `freshStatisticsDefaultToSentinelNotZero` — `Statistics.reset()` liefert
   jetzt `-1`, nicht `0`, für beide Felder.
2. `reachingDepthZeroNowRegistersAsDeepestFloor` — reproduziert exakt die
   Bedingung aus `Dungeon.java:385` (`depth > Statistics.deepestFloor &&
   branch == 0`) und zeigt: mit dem neuen `-1`-Default wird `depth=0` korrekt
   als "neu erreicht" erkannt (`0 > -1` wahr), während dieselbe Bedingung
   unter dem alten `0`-Default (`0 > 0`) falsch gewesen wäre.
3. `ascendingToDepthZeroIsDistinguishableFromNeverAscended` — reproduziert
   den (jetzt gefixten) Vergleich aus `Rankings.java:105`/`WndRanking.java:235`
   und zeigt denselben Vorher/Nachher-Kontrast für `highestAscent`.

**Wichtige Einschränkung, ausdrücklich wie in der Sitzungsvorgabe gefordert
gemeldet statt verschwiegen:** Test 2 ruft **nicht** die echte
`Dungeon.newLevel()` auf, sondern reproduziert deren Bedingung isoliert.
Grund: `newLevel()` (Dungeon.java:297-403) überspringt den
`deepestFloor`-Update-Block komplett für `DeadEndLevel`/`VaultLevel`-Instanzen
(Zeile 378), und `depth=0` fällt heute (vor Segment 3) im `switch(depth)`
über den `default`-Zweig auf `DeadEndLevel` (Zeile 353-354). Die
Sentinel-Arithmetik selbst ist damit nachweislich korrekt, **aber erst ab
Segment 3** (sobald Region 0 eine echte, nicht-`DeadEndLevel`-Levelklasse
bekommt) wird dieser Codepfad in der echten `newLevel()`-Methode tatsächlich
durchlaufen. Kein separater Fix nötig — Segment 3 aktiviert den bereits
korrekten Vergleich automatisch. Der Segment-4-Smoke-Test (depth=0
laden/betreten/verlassen) deckt den End-to-End-Pfad dann zusätzlich ab.

### Testergebnis

```
gradlew :core:test
```

Alle 6 Testklassen grün, 0 Failures/Errors: `BundleAliasTest` (5),
`Depth0SentinelTest` (3, neu), `GameTestBaseSmokeTest` (4),
`GeneratorGoldenMasterTest` (1), `MobSpawnerGoldenMasterTest` (1),
`StandardRoomGoldenMasterTest` (1). Die drei Golden-Master-Tests sind
unverändert grün — Etagen 1-26 unangetastet.

---

## Segment 1 — Nachtrag: Bones.java, getrennte Felder statt drittem Sentinel

Entscheidung des Nutzers: kein dritter Sentinel-Wert. Stattdessen die drei
bisher im einzigen Feld `depth` verschmolzenen Bedeutungen in eigene Felder
auftrennen.

### Neues Feldlayout (`Bones.java`)

| Feld | Bedeutung | Vorher |
|---|---|---|
| `depth`/`branch` | echte Tiefe/Branch des aktuellen Bones-Fundorts, oder `-1` für "keiner" (Daily-Run, noch nie geschrieben) | trug zusätzlich zwei weitere, inkompatible Bedeutungen (s. u.) |
| `loaded` (neu, `boolean`) | wurde `bones.dat` in dieser Prozess-Laufzeit schon von der Platte gelesen? | implizit über `depth == -1` codiert |
| `depleted` (neu, `boolean`) | wurden die Bones am aktuellen Fundort bereits abgeholt? | implizit über `depth == 0` codiert (kollidierte mit einer echten Etage 0) |

`get()` prüft jetzt drei unabhängige Zustände nacheinander: `!loaded` → von
Platte lesen; `depleted` → `null` zurückgeben, ohne `lootAtCurLevel()`
überhaupt aufzurufen; sonst normale Abhol-Logik. Der alte Guard
`if (depth > 0)` beim Laden ist `if (!depleted)` gewichen — lädt `item`/
`heroClass` jetzt unabhängig vom `depth`-Wert, behebt damit direkt den
bereits bekannten Fund (Kategorie E, Punkt 1: echte Bones auf Etage 0 luden
`item`/`heroClass` nicht).

### Persistenz: neuer Bundle-Key `depleted`

Der "abgeholt"-Marker wird weiterhin nach `bones.dat` geschrieben (muss
Prozessneustarts überleben), aber jetzt mit dem echten `depth`/`branch` plus
explizitem `DEPLETED=true`-Key, statt `LEVEL` mit `0` zu überschreiben:

```
emptyBones.put(LEVEL, depth);      // vorher: fest 0
emptyBones.put(BRANCH, branch);    // vorher: gar nicht geschrieben
emptyBones.put(DEPLETED, true);    // neu
```

Damit ist der frühere Wiederholungsabhol-Fund (Segment-1-Bericht oben,
Punkt 2) strukturell behoben: `depleted` wird unabhängig vom tatsächlichen
`depth`-Wert geprüft, eine echte Etage 0 kann nicht mehr mit dem
"abgeholt"-Marker verwechselt werden.

**Rückwärtskompatibilität mit bestehenden `bones.dat`-Dateien** (vom alten
Code geschrieben, `LEVEL=0`, kein `DEPLETED`-Key, kein `BRANCH`-Key): wird
korrekt, aber suboptimal gelesen — `depleted` liest als `false` (Key fehlt,
`Bundle.getBoolean` defaultet zu `false`), `branch` liest als `0`
(`Bundle.getInt`-Default). Erst wenn der Held zufällig exakt auf der neuen
Etage 0 steht, würde `lootAtCurLevel()` fälschlich zuschlagen wollen — findet
aber `item`/`heroClass` beide `null` (die alte Marker-Datei enthielt sie nie),
liefert also `null` zurück (kein Crash, keine Phantom-Beute) und schreibt dabei
automatisch die neue, korrekte Marker-Form zurück. Selbstheilend nach dem
ersten `get()`-Aufruf, keine explizite Migration nötig — bewusst so gewählt,
da es sich um `bones.dat` handelt (Zusatzdatei, kein Spielstand) und der
Fehlerfall harmlos ausläuft.

### Verifikation: `BonesStateTest.java` (neu, 3 Fälle)

1. `neverReadFromDiskReturnsNullWithoutCrashing` — `loaded=false`, keine
   Datei vorhanden → `null`, zweimal hintereinander aufgerufen (kein Absturz,
   kein Hängenbleiben in einem inkonsistenten Zustand).
2. `depletedMarkerAtDepthZeroYieldsNothingEvenThoughDepthZeroIsReal` — Bundle
   mit `LEVEL=0, BRANCH=0, DEPLETED=true` direkt auf die Platte geschrieben,
   `Dungeon.depth=Dungeon.branch=0` (exakt derselbe Ort) → `get()` liefert
   `null`, keine Phantom-Beute. Das ist genau die Kollision, die die alte
   drei-Bedeutungen-in-einem-Feld-Kodierung für Etage 0 riskiert hätte.
3. `realBonesAtDepthZeroAreLoadedOnceThenStayDepletedAcrossSessions` —
   `Bones.leave()` auf Etage 0, erzwungener Neu-Read von Platte (simuliert
   Prozessneustart), erster `get()`-Aufruf liefert echte Beute; zweiter
   Aufruf (gleiche Sitzung) `null` (kein doppeltes Abholen); dritter Aufruf
   nach erneut erzwungenem Neu-Read `null` (Marker übersteht auch einen
   simulierten Neustart, nicht nur den In-Memory-Zustand).

`Bones`s private-static Felder werden zwischen Tests dieser Klasse per
Reflection zurückgesetzt (kein produktionsseitiger Reset-Hook — bewusst so,
weil `Bones`s Zustand *im echten Spiel* absichtlich prozessweit persistiert,
s. Feld-Kommentare in `Bones.java`) und `bones.dat` wird vor jedem Test aus
dem geteilten Test-Save-Verzeichnis gelöscht (`GdxTestRuntime` legt dieses
Verzeichnis nur einmal pro JVM-Testlauf an, s. `docs/testing.md`).

### Testergebnis

`gradlew :core:test`: 7 Testklassen grün, 0 Failures/Errors — die drei
bestehenden Golden-Master-Tests weiterhin unverändert, `BonesStateTest` (3,
neu) zusätzlich zu den bisherigen 4 Klassen aus dem Hauptsegment.

---

**Segment 1 (inkl. Nachtrag): abgeschlossen. Weiter mit Segment 2.**

---

## Segment 2 — Bekannte Crashes + rundungsanfällige Arithmetik

### a) Vier Crash-Stellen abgesichert

`StandardRoom.java:228`, `EntranceRoom.java:185`, `ExitRoom.java:122`,
`ConnectionRoom.java:82` indizierten alle direkt ein `float[27][] chances`
mit `Dungeon.depth`. `chances[0]` ist in allen vier Fällen `null` (das Array
wird nur für 1-26 befüllt) → `Random.chances(null)` → NPE. Gefixt nach
demselben Muster wie `Generator.randomArmor/randomWeapon/randomMissile`
(`GameMath.gate(...)`), jeweils direkt vor der Array-Indizierung:

```
int depth = (int)GameMath.gate(1, Dungeon.depth, chances.length-1);
```

Bewusst unabhängig von der Region-0-Levelklasse aus Segment 3 gehalten (wie
in der Aufgabenstellung gefordert) — falls je ein anderer Codepfad
(Debug-Tools, künftige Wiederverwendung) diese Fabriken mit `depth==0`
aufruft, liefern sie jetzt den Wert für Etage 1 statt abzustürzen, statt gar
nicht erst zu funktionieren.

### b) Rundungsanfällige `(depth-1)/5`-Stellen

- **`RegularLevel.java:591`** (`region`-Variable für `Document`-Auswahl,
  z. B. `Document.SEWERS_GUARD`) — `region = Dungeon.depth <= 0 ? 0 :
  1+(Dungeon.depth-1)/5;`. `region=0` trifft keinen der `case 1..5`-Zweige,
  fällt auf `default: regionDoc = null;` — Etage 0 bekommt jetzt korrekt
  kein Regions-Lore-Dokument statt fälschlich `SEWERS_GUARD`.
- **`DriedRose.java:784` (`sayAppeared`) und `:814` (`sayBoss`)** — dieselbe
  Guard-Form, `depth = Dungeon.depth <= 0 ? -1 : (Dungeon.depth-1)/5;`.
  `-1` trifft keinen der `case 0..3`-Zweige, fällt auf `case 4: default:`
  (Halls-Dialogzeilen) statt fälschlich die Sewers-Zeilen zu sprechen.
  `sayBoss()` wird nach heutigem `Dungeon.bossLevel()`-Gate ohnehin nie mit
  depth=0 erreicht: Boss-Etagen sind weiterhin fest 5/10/15/20/25 (Design-
  Entscheidung 3), Fix trotzdem ergänzt für Konsistenz mit `sayAppeared()`
  und falls sich das Gate künftig ändert.
- **`TerrainFeaturesTilemap.java:69` — bewusst NICHT geändert.** Diese
  Stelle unterscheidet sich strukturell von den drei obigen: kein
  `switch`/`case` mit einem sauberen "nicht zugeordnet"-Zweig, sondern ein
  reiner Multiplikator (`16*stage`) für die Gras-/Embers-Kachelvariante.
  `stage=(0-1)/5=0` für Etage 0 ist **kein Crash-Risiko** (bereits in
  `depth0-findings.md` Kategorie A so eingestuft: reine Arithmetik, keine
  Array-Indizierung) — der einzige Effekt ist, dass Etage-0-Gras (falls es
  je welches gäbe) optisch wie Etage-1-5-Gras eingefärbt würde. Eine
  "korrekte" Ersatzstufe für Etage 0 zu wählen wäre eine Deko-/
  Inhaltsentscheidung (wie soll Oberflächen-Vegetation aussehen?) — exakt
  das, was der Prompt ausdrücklich ausschließt ("keine Dekoration", "keine
  spezifischen Räume"). Der Platzhalter-Raum aus Segment 3 ist zudem
  unverziert und dürfte `HIGH_GRASS`/`FURROWED_GRASS`/`GRASS`/`EMBERS`
  ohnehin gar nicht verwenden, wodurch dieser Codepfad für Etage 0 aktuell
  praktisch unerreicht bleibt. Gemeldet statt geraten.

### c) `Dungeon.java` — Limited-Drop-Helfer gegen `depth<=0` abgesichert

`posNeeded()`, `souNeeded()`, `asNeeded()`, `enchStoneNeeded()`,
`labRoomNeeded()` nutzen `depth/5` bzw. `depth%5` für "Ende eines
Etagen-Sets"-Logik. `depth=0` teilt sich `depth/5==0` und `depth%5==0` mit
Etage 5 (Ende von Floor-Set 1) — dieselbe Aliasing-Klasse wie bei a)/b),
hier aber ohne Crash-Risiko (reine Zähler-Arithmetik). Jede der fünf
Methoden bekommt jetzt einen frühen `if (depth <= 0) return false;`-Guard,
analog zur Aufgabenstellung. Für `enchStoneNeeded()` war das Verhalten
bereits zufällig korrekt (der bestehende `region > 1`-Check filtert Etage 0
schon heraus, `region` wird dort `1`) — Guard trotzdem ergänzt, wie
ausdrücklich verlangt, zur Konsistenz und als Absicherung falls sich der
Schwellenwert künftig ändert.

**Nachtrag (auf Ansage):** `Dungeon.intStoneNeeded()` (Zeile 579-582) und
`trinketCataNeeded()` (Zeile 584-587) zeigen dieselbe Aliasing-Klasse
(`depth < 5 && Random.Int(4-depth) == 0` — für Etage 0 identisch zu Etage 1,
1/4-Chance). Ursprünglich nicht Teil der explizit benannten Liste, auf
Ansage aber mit demselben `depth<=0`-Guard versehen — folgt laut Nutzer
direkt aus Design-Entscheidung 2 (Etage 0 gehört zu keinem Etagen-Set),
keine separate Entscheidung mehr nötig.

### Verifikation

Neuer Test: `Depth0RoomFactoryGuardTest.java`, 2 Fälle:

1. `roomFactoriesDoNotCrashAtDepthZero` — ruft alle vier reparierten
   Fabriken (`StandardRoom.createRoom()`, `EntranceRoom.createEntrance()`,
   `ExitRoom.createExit()`, `ConnectionRoom.createRoom()`) mit
   `Dungeon.depth=0` auf, unter einem gepushten Test-Seed (dieselbe
   Notwendigkeit wie in `StandardRoomGoldenMasterTest`, s.
   `docs/testing.md`s Hinweis zu ambient generators). Erwartet: alle vier
   liefern eine Instanz, kein NPE.
2. `limitedDropHelpersReturnFalseAtDepthZeroInsteadOfAliasingFloorSetEnd` —
   alle sieben gegateten `Dungeon`-Methoden (die fünf ursprünglich benannten
   plus `intStoneNeeded()`/`trinketCataNeeded()` aus dem Nachtrag) liefern
   bei `depth=0` `false`.

### Testergebnis

`gradlew :core:test`: 8 Testklassen grün, 0 Failures/Errors (erneut geprüft
nach dem `intStoneNeeded`/`trinketCataNeeded`-Nachtrag) — die drei
bestehenden Golden-Master-Tests weiterhin unverändert (die
`GameMath.gate(...)`-Guards ändern für Eingaben, die bereits im Bereich
1-26 liegen, nichts an Index oder Reihenfolge).

---

**Segment 2: abgeschlossen. Wartet auf Bestätigung vor Segment 3.**

---

## Segment 3 — RegionDefinition[] + Platzhalter-Region 0

### a) `RegionDefinition[]`

Neue Klasse `levels/RegionDefinition.java`: `index`, `firstDepth`, `floorCount`,
`levelClass`, `bossLevelClass` (nullable), `loreDocument` (nullable),
`splashAsset`, `displayName` (nullable, optionaler Anzeigename). 6 Einträge:
Region 0 (Oberfläche, `firstDepth=0, floorCount=1`) plus die 5 bestehenden
Regionen (Sewers/Prison/Caves/City/Halls, je `floorCount=5`, unverändert
`firstDepth=1/6/11/16/21`). `regionOf(depth)` ist eine Bereichssuche
(`firstDepth <= depth <= lastDepth()`), keine indexbasierte Lookup wie bei
`StandardRoom`/`MobRegistry` — Regionen haben unterschiedliche `floorCount`
(Region 0 hat 1, die anderen 5), ein flaches `depth`-indiziertes Array wäre
hier keine natürliche Passform gewesen.

**Migrierte Konsumenten:**

- `Dungeon.newLevel()` (branch==0-Zweig) — `RegionDefinition.regionOf(depth)`
  liefert `levelClass` oder (an der letzten Etage der Region, falls
  `bossLevelClass!=null`) `bossLevelClass`, instanziert per
  `Reflection.newInstance(...)`. Etage 26 (`LastLevel`) und alles außerhalb
  0-25 bleiben als explizite Sonderfälle NACH der Tabellen-Lookup bestehen
  (siehe unten, "Was die Tabelle bewusst nicht abdeckt").
- `Dungeon.bossLevel(int depth)` — `region!=null && region.bossLevelClass!=null
  && depth==region.lastDepth()`. Reproduziert exakt die alte Literal-Liste
  `5/10/15/20/25` (verifiziert, s. Testabschnitt unten).
- `RegularLevel.java`s Regions-Dokument-Switch (Zeile ~589-604, in Segment 2
  nur notdürftig mit einem `depth<=0`-Ternary abgesichert) — jetzt
  vollständig durch `RegionDefinition.regionOf(Dungeon.depth)?.loreDocument`
  ersetzt. Ersetzt außerdem die abgeleitete `5*(region-1)+1`-Rechnung fürs
  "erste Etage der Region"-Ziel weiter unten in derselben Methode durch das
  benannte Feld `region.firstDepth` (identisch, aber jetzt ohne erneute
  Arithmetik).
- `InterlevelScene.java`s Splash-Switch — jeder `case`-Zweig liest
  `RegionDefinition.REGIONS[i].splashAsset` statt eines fest verdrahteten
  `Assets.Splashes.X`-Literals. Neuer `case 0:`-Zweig für Region 0 (bisher
  fiel `region=0` unbemerkt auf `default:` = Halls-Splash, s. Segment 4c
  unten). Die Fokuspunkt-Untervarianten (`loadingCenter`-Werte je Bild)
  bleiben unverändert als Switch bestehen — das sind pro-Bild-Bildausschnitte,
  keine Regionszuordnung, gehören konzeptionell nicht in die Tabelle.
  Region 0 nutzt (mangels eigener Grafik, s. u.) dieselben Fokuspunkte wie
  Sewers, da es exakt dasselbe Bild ist.

**`lastRegion`/`region` (int) bewusst NICHT durch `RegionDefinition`-Objekte
ersetzt:** `InterlevelScene.lastRegion` wird zusätzlich für Textur-Cache-
Leerung, RNG-Seed-Offset (`seed+lastRegion`), einen Spezialeffekt
(`lastRegion==6` → Alpha-Übergang bei der allerletzten Etage) und das
Story-Popup-Gate (`lastRegion<=5`) verwendet — alles fein auf die
bestehende numerische 1-6-Zählung kalibriert. `Math.ceil(loadingDepth/5f)`
ergibt für Etage 0 bereits heute zufällig genau `0` (kollidiert mit keinem
bestehenden Wert 1-6) und für Etage 26 weiterhin `6` (unverändert, erhält
den bestehenden Spezialeffekt) — die Formel selbst musste NICHT geändert
werden, nur der Switch bekam einen echten `case 0:`-Zweig statt in
`default:` zu fallen.

### Was die Tabelle bewusst nicht abdeckt

**Etage 26 (`LastLevel`) ist keine eigene Region.** Kein 5-Etagen-Block,
kein Boss, kein Lore-Dokument — sowohl der alte Dokument-Switch als auch der
alte Splash-Switch fielen für Etage 26 bereits auf ihren `default`-Zweig
(identisch zu "keine Region gefunden"). `RegionDefinition.regionOf(26)`
liefert daher bewusst `null`; `Dungeon.newLevel()` behandelt `depth==26` als
expliziten Sonderfall NACH der Tabellen-Lookup (`else if (depth==26) level =
new LastLevel();`), nicht als Tabelleneintrag.

**`ShopRoom.generateItems()` — bewusst NICHT migriert, gemeldet statt
riskiert.** Der ursprüngliche Auftrag nannte diese Methode explizit als zu
ersetzenden Switch. Bei der Prüfung zeigte sich: dieser Switch vermischt
zwei unabhängige, nicht auf ein "ein Konfigurationswert pro Region"-Modell
abbildbare Mechaniken:

1. Den echten Regions-Shop auf der ersten Etage von Prison/Caves/City
   (`Dungeon.shopOnLevel()` = Etage 6/11/16, via `RegularLevel.initRooms()`
   `new ShopRoom()` — gated durch `shopOnLevel()`).
2. Den Imp-Shop (`ImpShopRoom extends ShopRoom`, erbt `generateItems()`) auf
   der City-Boss-Etage (`CityBossLevel`, Etage 20) — dessen `case 20: case
   21:`-Zweig nutzt Waffen-/Rüstungs-Tier 4 (Platten-Rüstung), **eine Stufe
   höher** als Citys eigener Regions-Shop auf Etage 16 (Tier 3,
   Schuppen-Rüstung). Verifiziert per Grep: `new ShopRoom()` wird
   AUSSCHLIESSLICH in `RegularLevel.java:144` konstruiert (gated durch
   `shopOnLevel()`), `ImpShopRoom` ausschließlich in `CityBossLevel.java:190`
   und `LastShopLevel.java:85`. `LastShopLevel` selbst wird nirgends im
   aktuellen Code instanziert (`new LastShopLevel` liefert keinen Treffer) —
   der `case 21`-Teil dieses kombinierten Switch-Zweigs scheint bereits vor
   dieser Sitzung totes Gepäck zu sein, vermutlich ein Überbleibsel aus dem
   Shattered-Pixel-Dungeon-Upstream.

   Eine Migration auf `RegionDefinition` hätte entweder (a) den Tier-Sprung
   an der Boss-Etage stillschweigend verloren (falls naiv "eine Zeile pro
   Region" angenommen wird) oder (b) eine zusätzliche, in der Tabelle nicht
   vorgesehene "Boss-Etage hat einen eigenen, höheren Tier"-Sonderregel
   gebraucht — beides eine Verhaltensentscheidung, keine reine
   Struktur-Extraktion. `ShopRoom.generateItems()` bleibt daher **komplett
   unverändert**. `RegionDefinition` hat trotzdem kein `hasShop`-Feld
   bekommen, da es für nichts gebraucht worden wäre (kein Konsument hätte es
   gelesen) — bei Bedarf für Fork 2 leicht nachrüstbar.

### b) Region-0-Platzhalterklasse: `Region0Level`

Neue Klasse `levels/Region0Level.java`, `extends Level` direkt (nicht
`RegularLevel`/`StandardRoom`) — strukturell an `DeadEndLevel` angelehnt
(die einzige bereits bestehende, handgebaute `Level`-Unterklasse ohne
Raumsystem). `build()` schnitzt einen 7×7-Raum (5×5 begehbar) von Hand,
platziert einen `Terrain.EXIT` mit `LevelTransition.Type.REGULAR_EXIT`
(Ziel: Etage 1, `destDepth=Dungeon.depth+1`). Keine eigene Eingangs-Treppe
nötig — `Level.getTransition(null)`s bestehender Fallback (letzte
Transition der Liste, falls kein "Eingangs-artiger" Typ gefunden wird)
greift bereits korrekt für den Ein-Transition-Fall.

**Kämpfe technisch möglich, aber nichts konfiguriert:** `createMob()` bleibt
UNVERÄNDERT auf der `Level`-Basisimplementierung (nutzt
`MobSpawner.getMobRotation(Dungeon.depth)`, exakt derselbe Mechanismus wie
jede reguläre Etage — funktioniert bereits heute für Etage 0 dank
`MobRegistry.bracketOfDepth`s dokumentiertem depth-0-Fallback auf Bracket 0,
s. `depth0-findings.md`). `addRespawner()` liefert dagegen `null` (wie
`DeadEndLevel`) — unterbindet die automatische Ambient-Respawn-Uhr, sodass
in der Praxis KEIN Mob von selbst erscheint, obwohl der Mechanismus
technisch verdrahtet ist. `createMobs()` (die initiale Einmal-Besetzung)
ist leer — kein erzwungener Auftakt-Kampf. `createItems()` übernimmt
1:1 `DeadEndLevel`s Bones-Anbindung (`Bones.get()`, an der Exit-Zelle
abgelegt) — das macht den in Segment 1 gebauten `BonesStateTest`
(Bones an einer echten Etage 0) auch über den vollständigen Level-Zyklus
hinweg wirksam, nicht nur isoliert.

**Bewusst NICHT gebaut (Fork-2-Scope):** Deko, NPCs, Häuser, Handel,
prozedurale Item-Platzierung. `tilesTex()`/`waterTex()` liefern
Platzhalter-Assets (`TILES_SEWERS`/`WATER_SEWERS`, dieselbe Wahl wie beim
Splash-Bild, s. u.) statt neuer Grafik.

**Namenskollisions-Warnung bestätigt (bereits in Segment 1 vorab notiert):**
`Region0Level` ist bewusst NICHT "SurfaceLevel" genannt — `SurfaceScene`
existiert bereits als der Amulett-Sieg-Bildschirm (`LevelTransition.Type.
SURFACE`, ausgelöst von Etage 1s Eingangsfeld). Beide Konzepte heißen
zufällig "Oberfläche", sind aber vollständig unabhängig: `EntranceRoom.java`s
`Dungeon.depth==1`-Sonderfall (Zeile 93-97, nutzt weiterhin `Type.SURFACE`)
wurde NICHT angefasst. Das bedeutet: **Etage 0 ist über den normalen
Spielfluss aktuell nicht erreichbar** — Etage 1s "nach oben"-Feld führt
weiterhin zum Sieg-Bildschirm, nicht zu `Region0Level`. Etage 0 existiert
nur für direkten Aufruf (`Dungeon.depth=0; Dungeon.newLevel()`), genau wie
im Auftrag umrissen ("Fähigkeit dass Region 0 als Level existieren, geladen
und verlassen werden kann" — nicht "ins Standard-Spiel verdrahtet"). Eine
echte Verbindung (Etage 1 ↔ Etage 0 im normalen Spielfluss, plus eine
Entscheidung, was dann mit dem Amulett-Sieg-Pfad passiert) ist explizit
Fork-2-Territorium.

### c) Weitere Fundstellen aus der Statistics-/Splash-Prüfung

**Splash-Switch (bereits oben, Teil a):** Region 0 bekommt jetzt einen
echten Splash statt unbemerkt auf Halls zu fallen — der ursprünglich in
Segment 4c der Analyse befürchtete Fund.

**Story-Popup-Gate (`InterlevelScene.java`, `mode==DESCEND && lastRegion<=5`)
— geprüft, kein Fix nötig.** Für `lastRegion==0` (Etage 0) wäre
`Document.INTROS.pageBody(0)` erreichbar — Index 0 in `Document.INTROS`s
`pagesStates`-Map ist der Eintrag `"Dungeon"` (Property-Key
`journal.document.intros.dungeon.*`). Das Popup feuert für Etage 0 trotzdem
**nicht**: Die zweite Bedingung (`loadingDepth > Statistics.deepestFloor &&
loadingDepth % 5 == 1`) prüft `0 % 5 == 1`, was strukturell nie wahr ist —
dieselbe Absicherung, die auch "erste Etage einer Region" für 1/6/11/16/21
korrekt erkennt, schließt 0 bereits sauber aus. Über `InterlevelScene`
erreicht also tatsächlich nie jemand `pageBody(0)`; keine Änderung nötig.

**Korrektur (auf Nachfrage des Nutzers):** Der erste Entwurf dieses
Abschnitts behauptete, der "Dungeon"-Text sei "nirgends im Code als gelesen
markiert" und damit unbenutzter, für Fork 2 vorgemerkter Text. Das war
**falsch** — nur zu eng geprüft (nur nach `readPage("Dungeon")`-Aufrufen
gesucht, nicht nach direkten `pageBody(...)`-Lesezugriffen). Tatsächlich
ist dieser Text **aktiv in Benutzung**: `WelcomeScene.java:200-201` zeigt
`Document.INTROS.pageBody(0)` jedem neuen Spieler auf dem allerersten
Willkommens-Bildschirm an (`previousVersion==0` = frische Installation,
oder `SPDSettings.intro()==true` = Einführungsmodus noch aktiv, bevor der
erste Lauf gestartet wird) — unabhängig vom `readPage`-basierten
Fundstatus-Mechanismus, den die anderen fünf `INTROS`-Seiten
(Sewers/Prison/Caves/City/Halls) über `InterlevelScene` nutzen. Deshalb ist
`"Dungeon"` in `Document.java:290` auch unbedingt `READ` vorbelegt (nicht
`debug ? READ : NOT_FOUND` wie die anderen fünf) — als bereits bekanntes
Lore gedacht, einmalig auf dem Willkommens-Bildschirm gezeigt, nicht
etagenweise freigeschaltet. Laut Git-Historie (Upstream Shattered Pixel
Dungeon, v1.4.0) ist das seit 2022 bewusst so gebaut, keine Altlast dieses
Forks.

**Konsequenz für den ursprünglichen "Fork 2"-Hinweis: hinfällig.** Der Text
ist kein brachliegender Kandidat für einen Oberflächen-Einstiegstext — er
wird bereits aktiv für etwas anderes verwendet (die allgemeine
Willkommensbotschaft vor der Heldenerstellung). Ihn zusätzlich für Etage 0
zu zeigen würde denselben Text an zwei völlig verschiedenen Stellen
duplizieren, was eher verwirrend als hilfreich wäre. Kein Änderungsbedarf
an `InterlevelScene`/`WelcomeScene` in dieser Sitzung — nur diese
Doku-Korrektur.

**Statistics-Schleifen (`storeInBundle`/`restoreFromBundle`,
`for(i=1;i<26;i++)` für `floorsExplored`) — geprüft, kein Fix nötig.** Diese
Schleifen decken bewusst nur 1-25 ab (nicht einmal 26). Der einzige Schreiber
von `floorsExplored` (`Dungeon.java`, `if (branch==0 && level instanceof
RegularLevel && !Dungeon.bossLevel())`) ist durch `instanceof RegularLevel`
gated — `Region0Level extends Level` direkt, fällt strukturell NIE in diesen
Zweig, unabhängig von den Schleifengrenzen. Eine Erweiterung auf `i=0` wäre
totes Gepäck (nie befüllter Eintrag). Keine Änderung vorgenommen.

`WndJournal.java`/`CustomNoteButton.java`s `for(i=deepestFloor;i>0;i--)`
(bereits in Segment 1 dokumentiert, dort unverändert gelassen) bleiben aus
demselben Grund unangetastet — beide listen Notizen ausschließlich für
raumbasierte Etagen auf, für die Etage 0 strukturell nie in Frage kommt.

### Verifikation

Neuer Test: `RegionDefinitionTest.java`, 5 Fälle:

1. `newLevelMatchesOriginalSwitchForEveryMainPathDepth` — ruft
   `Dungeon.newLevel()` für JEDE Etage 1-26 auf und vergleicht die
   zurückgegebene Klasse gegen eine im Test unabhängig (per Hand, nicht aus
   dem neuen Code abgeschrieben) nachgebaute Kopie des alten Switches. Das
   ist die stärkste Absicherung gegen einen Tippfehler in der Tabelle —
   keiner der drei bestehenden Golden-Master-Tests hätte einen falschen
   Tabelleneintrag bemerkt, da keiner `newLevel()`/`bossLevel()` direkt prüft.
2. `bossLevelMatchesOriginalLiteralListForEveryDepth` — `Dungeon.bossLevel(depth)`
   für -1 bis 27 gegen die alte Literal-Liste `5/10/15/20/25`.
3. `depthZeroProducesRegion0LevelWithAWorkingExit` — Etage 0 liefert
   `Region0Level`, mit einer funktionierenden `REGULAR_EXIT`-Transition
   Richtung Etage 1.
4. `depth26AndOutOfRangeDepthsAreNotPartOfAnyRegion` — `regionOf(26)`,
   `regionOf(-1)`, `regionOf(27)` liefern `null`.
5. `region0HasNoBossAndNoLoreDocument` — `RegionDefinition.regionOf(0)`s
   Felder plus `Dungeon.bossLevel(0)==false`.

### Testergebnis

`gradlew :core:test`: 9 Testklassen grün, 0 Failures/Errors — die drei
bestehenden Golden-Master-Tests weiterhin unverändert; `RegionDefinitionTest`
(5, neu) bestätigt zusätzlich explizit, dass Etage 1-26 durch die
`RegionDefinition[]`-Umstellung byte-identisch bleiben.

---

**Segment 3: abgeschlossen. Wartet auf Bestätigung vor Segment 4.**

---

## Segment 4 — Verifikation

### 1. Bestehende Golden-Master-Tests

`GeneratorGoldenMasterTest`, `StandardRoomGoldenMasterTest`,
`MobSpawnerGoldenMasterTest` — alle drei über jeden Segment-Durchlauf hinweg
kontinuierlich mitlaufen lassen, nie angefasst, durchgängig grün. Keiner
der drei testet `Dungeon.newLevel()`/`bossLevel()` direkt (die decken
`Generator`/`StandardRoom.createRoom()`/`MobSpawner` isoliert ab) — deshalb
zusätzlich `RegionDefinitionTest` (Segment 3) gebaut, der genau diese Lücke
mit einem unabhängig nachgebauten Vergleich gegen den alten Switch schließt.

### 2. Neuer Smoke-Test: Etage 0 laden/betreten/verlassen

`Depth0SmokeTest.depthZeroLoadsIsEnteredAndIsLeftRepeatedlyWithoutCrashing`:
fünf Durchläufe in einer Testmethode, jeder davon vollständig
Laden→Betreten→Verlassen:

1. **Laden:** `Dungeon.depth=0; Dungeon.newLevel()` — liefert `Region0Level`.
2. **Betreten:** `Dungeon.level` gesetzt, Held auf `entrance()` platziert
   (begehbare Zelle bestätigt), `Actor.init()` — Held als aktiver Actor
   registriert.
3. **Verlassen:** `REGULAR_EXIT`-Transition gefunden (Ziel: Etage 1),
   `Dungeon.depth`/`branch` entsprechend gesetzt, erneut `Dungeon.newLevel()`
   — liefert korrekt `SewerLevel` (Etage 1), nicht `DeadEndLevel` o. Ä.

Zusätzlich 3× per Hand mit `--rerun` einzeln erneut ausgeführt (nicht nur
einmalig grün) — stabil, keine Flakiness, keine über Durchläufe hinweg
leckende `Random`-Generator-Stack- oder `Bones`/`LimitedDrops`-Zustände.

Bewusst NICHT über `Dungeon.switchLevel()` (schreibt auf Platte, spricht
`GameScene`-UI an) — mirror von `GameTestBase`s eigener, dokumentierter
Grenze (s. `docs/testing.md`, "Known boundary").

### 3. CLAUDE.md und docs/testing.md aktualisiert

- **CLAUDE.md Fallstrick #7** überarbeitet: beschreibt jetzt
  `RegionDefinition[]` als zentrale Tabelle für `newLevel()`/`bossLevel()`/
  Regions-Dokument, nennt explizit, was NICHT migriert wurde
  (`ShopRoom.generateItems()`, `StandardRoom.chances`, `MobRegistry`,
  `Generator.floorSetTierProbs`), und dokumentiert Etage 0/`Region0Level`
  als neue, eigenständige Region — inkl. der Einschränkung, dass sie über
  den normalen Spielfluss noch nicht erreichbar ist.
- **Neues Fallstrick #8** (vorheriges #8 zu #9 verschoben): hält die
  `-1`-Sentinel-Konvention und `Bones.java`s `loaded`/`depleted`-Feldtrennung
  als durables Architekturwissen fest (nicht nur Sitzungsprotokoll).
- **`levels/`-Zeile** in "Wichtige Pakete" ergänzt um `RegionDefinition.java`/
  `Region0Level.java`.
- **`docs/testing.md`** um einen Absatz zu den fünf neuen Testklassen
  ergänzt (`Depth0SentinelTest`, `BonesStateTest`,
  `Depth0RoomFactoryGuardTest`, `RegionDefinitionTest`, `Depth0SmokeTest`).

---

# Gesamtbericht

## Was in jedem Segment geändert wurde

**Segment 1 (+ Nachtrag):** `0` als "keine Etage"-Sentinel auf `-1`
vereinheitlicht: `LevelTransition`s `SURFACE`-destDepth (totes Datenfeld),
`Statistics.deepestFloor`/`highestAscent` (Default + Vergleichsstellen in
`Rankings.java`/`WndRanking.java`). `Bones.java` intern neu strukturiert:
`depth`/`branch` (echte Tiefe oder -1), `loaded` (von Platte gelesen?) und
`depleted` (an diesem Ort bereits abgeholt?) als drei getrennte Felder statt
einem überladenen `depth`-Int — behebt sowohl das bekannte "Bones auf Etage
0 laden kein Item" als auch einen dabei neu gefundenen
Wiederholungsabhol-Fehler.

**Segment 2:** Die vier bekannten `chances[27]`-Crashes
(`StandardRoom`/`EntranceRoom`/`ExitRoom`/`ConnectionRoom`) mit
`GameMath.gate(1, depth, chances.length-1)` abgesichert. Rundungsfehler bei
`(depth-1)/5` in `RegularLevel.java` und `DriedRose.java` (beide Stellen)
gefixt, sodass Etage 0 nicht mehr als Region 1 erkannt wird.
`Dungeon.java`s sieben Limited-Drop-Helfer (`posNeeded`/`souNeeded`/
`asNeeded`/`enchStoneNeeded`/`labRoomNeeded`, dazu auf Nachfrage
`intStoneNeeded`/`trinketCataNeeded`) mit `depth<=0`-Guards versehen.

**Segment 3:** Neue `RegionDefinition[]`-Tabelle (`levels/RegionDefinition.java`)
ersetzt die Switches in `Dungeon.newLevel()`, `Dungeon.bossLevel()` und
`RegularLevel`s Regions-Dokument-Auswahl; `InterlevelScene`s Splash-Switch
liest jetzt aus derselben Tabelle plus einem neuen `case 0:`-Zweig für
Region 0. Neue Platzhalter-Levelklasse `levels/Region0Level.java` — ein
handgebauter 7×7-Raum nach dem Vorbild von `DeadEndLevel`, mit
funktionierender Exit-Transition nach Etage 1, MobRegistry-Anbindung aktiv
aber nichts spawnt automatisch.

**Segment 4:** Verifikation (dieser Abschnitt) plus Doku-Updates.

## Bestätigung: bestehende Tests grün

`gradlew :core:test` — **10 Testklassen, 0 Failures, 0 Errors:**
`BonesStateTest` (3), `BundleAliasTest` (5), `Depth0RoomFactoryGuardTest` (2),
`Depth0SentinelTest` (3), `Depth0SmokeTest` (1), `GameTestBaseSmokeTest` (4),
`GeneratorGoldenMasterTest` (1), `MobSpawnerGoldenMasterTest` (1),
`RegionDefinitionTest` (5), `StandardRoomGoldenMasterTest` (1). Die drei
Golden-Master-Tests sind über alle vier Segmente hinweg kontinuierlich grün
geblieben — Etage 1-26 wurde an keiner Stelle in ihrem Verhalten verändert.

## Bestätigung: Etage 0 lädt/verlässt sich sauber, ohne Inhalt

`Depth0SmokeTest` beweist den vollen Zyklus (laden → betreten → verlassen
→ Etage 1 erreichen) über 5 Durchläufe, zusätzlich mehrfach manuell erneut
ausgeführt ohne Flakiness. `Region0Level` enthält bewusst keine Dekoration,
keine NPCs, keine Items außer der bestehenden Bones-Mechanik — ein einzelner
begehbarer Raum, der nur beweist, dass die Mechanik trägt.

**Einschränkung, die im Bericht nicht verschwiegen werden soll:** Etage 0
ist über den NORMALEN Spielfluss (neues Spiel, Etage 1s Aufstiegsfeld)
weiterhin nicht erreichbar — dieses Feld führt unverändert zum
Amulett-Sieg-Bildschirm (`SurfaceScene`, `LevelTransition.Type.SURFACE`).
Das war laut Auftrag auch nicht verlangt ("Fähigkeit dass Region 0 als
Level existieren, geladen und verlassen werden kann" — nicht "ins
Standard-Spiel verdrahtet"); die tatsächliche Verknüpfung beider Konzepte
(und was dann mit dem Sieg-Pfad passiert) ist eine eigene, größere
Entscheidung.

## Explizite Liste: was für den Content-Fork (Fork 2) offen ist

- **Anzeigename für Etage 0.** `RegionDefinition.REGIONS[0].displayName`
  trägt aktuell den Platzhalter `"???"`. Mechanismus vorhanden, Text nicht
  final.
- **Splash-Bild für Etage 0.** Nutzt aktuell `Assets.Splashes.SEWERS`
  (dieselbe Datei wie Region 1), mangels eigener Grafik. Auch
  `Region0Level.tilesTex()`/`waterTex()` sind Platzhalter (`TILES_SEWERS`/
  `WATER_SEWERS`).
- **Echte Verknüpfung in den Spielfluss.** Etage 1 ↔ Etage 0 im normalen
  Spielfluss verbinden; Entscheidung, was mit dem heutigen
  Amulett-Sieg-Pfad über Etage 1s Aufstiegsfeld passiert (koexistieren?
  ersetzt werden? verschoben?).
- **Häuser, NPCs, Handel.** Explizit ausgeschlossen laut Auftrag, komplett
  offen.
- **Prozedurale Item-/Gebäude-Platzierung.** Ebenfalls explizit
  ausgeschlossen, komplett offen.
- **Mobs für Etage 0 konfigurieren.** `createMob()` ist technisch
  angebunden (nutzt `MobRegistry`/`MobSpawner` wie jede andere Etage,
  fällt via `MobRegistry.bracketOfDepth(0)` auf Bracket 0 = Kanalisations-
  Ratten zurück), aber `addRespawner()` liefert bewusst `null` — keine
  automatische Ambient-Spawns. Für Fork 2: entscheiden, ob Bracket-0-Fallback
  passend ist oder Etage 0 eine eigene Mob-Rotation braucht.
- **`Bones.java` auf Etage 0 — Grundmechanik funktioniert, aber
  ungetestet im echten Mehrfach-Session-Zusammenspiel mit echtem
  Content.** Die drei neuen Felder (`loaded`/`depleted`/`depth`) sind
  isoliert und im Smoke-Test verifiziert; ein echter Todesfall auf einer
  inhaltlich ausgebauten Etage 0 (mit echten Items/Held) ist noch nicht
  durchgespielt worden.
- **`ShopRoom.generateItems()`** bleibt ein eigener, nicht auf
  `RegionDefinition` migrierter Switch (Segment 3). Falls Fork 2 einen Shop
  für Etage 0 will, braucht das eine eigene Entscheidung, kein einfaches
  Tabellen-Update.
- **Der "Dungeon"-Intro-Text** (`journal.document.intros.dungeon.*`) ist
  BEREITS aktiv in Verwendung (Willkommens-Bildschirm vor Heldenerstellung,
  `WelcomeScene.java`) — NICHT als Kandidat für einen Etage-0-Text verfügbar,
  ohne Duplikation/Verwirrung zu riskieren (Korrektur eines Fehlers aus der
  ersten Fassung dieses Berichts, s. Segment 3c).

## Keine Commits

Alle Änderungen stehen im Arbeitsverzeichnis, nicht committet — wie
durchgehend vereinbart ("keine Commits ohne separate Ansage").

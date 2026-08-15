# depth=0 Machbarkeitsanalyse — Fundstellen

Reine Analyse, keine Code-Änderungen. Fortsetzung von "Etagen entkoppeln,
depth=0 ermöglichen" (Punkt 3). Segmente werden nacheinander abgearbeitet
und hier angehängt.

## Bereits bekannt (aus vorheriger, abgestürzter Sitzung — verifiziert, nicht erneut geprüft)

1. `Level1Transition.SURFACE = 0` nutzt die Zahl 0 heute als Sonderwert
   (Oberfläche/Ausgangspunkt, keine echte Etage).
2. `Bones.LEVEL = 0` nutzt die Zahl 0 heute als Sonderwert ("keine
   Überreste vorhanden").
3. `StandardRoom.java:228` crasht sofort bei depth=0.
4. Derselbe `chances[27]`/NPE-bei-depth-0-Fehler betrifft zusätzlich
   `EntranceRoom`, `ExitRoom`, `ConnectionRoom`.
5. `Statistics.deepestFloor` startet bei 0 und aktualisiert nur bei
   strikt `>` — Etage 0 könnte dadurch nie als "erreicht" gelten.

---

## Segment 1 — Kapitel-/Regions-Arithmetik & feste Switches über Etagen

### Kategorie A: Kapitel-/Regions-Arithmetik (`depth/5`, `(depth-1)/5`, `depth%5`)

Ca. 15+ Fundstellen über `Dungeon.java`, `Generator.java`, `RegularLevel.java`,
`DriedRose.java` und mehrere `rooms/special/*Room.java` / `rooms/secret/*Room.java`.

Beispiele:

- `Dungeon.java:531,548,558,569,591` (`posNeeded`, `souNeeded`, `asNeeded`,
  `enchStoneNeeded`, `labRoomNeeded`) — alle rechnen mit `depth/5` und
  `depth%5` für limitierte Drops (Stärke-Tränke, Aufwertungsschriftrollen,
  Zauberstäbe, Verzauberungssteine, Labor-Räume).
- `Generator.java:419,440,448,473,481` (`randomArmor`, `randomWeapon`,
  `randomMissile`) — nutzen `Dungeon.depth/5` als `floorSet`-Index, aber
  clampen ihn danach explizit mit `GameMath.gate(0, floorSet, ...)`.
- `RegularLevel.java:591` — `int region = 1+(Dungeon.depth-1)/5;` zur
  Bestimmung von Regions-Dokumenten (`Document.SEWERS_GUARD` etc.).
- `items/artifacts/DriedRose.java:784,814` — lokale Variable
  `int depth = (Dungeon.depth - 1) / 5;` für Ghost-Dialogzeilen pro Region.

**Einschätzung:** Funktioniert bei depth=0 überwiegend automatisch, aber
**mit einer stillen Kollision**: Java rundet Integer-Division Richtung
Null, also ist `(0-1)/5 == 0` (nicht `-1`). Dadurch fällt depth=0 in
`RegularLevel.java:591` und `DriedRose.java` genau in "Region 1" (Sewers)
— exakt wie Etage 1-5. Das ist kein Crash, aber eine unbeabsichtigte
Gleichsetzung von Etage 0 mit "erste Etage der Kanalisation", die bei
jeder Verwendung von `(depth-1)/5` in dieser Codebase vermutlich identisch
auftritt. `Generator.java`s Varianten sind unkritisch, weil dort explizit
gegated wird. `Dungeon.java`s Drop-Funktionen aliasen auf dieselbe Weise
depth=0 mit depth 5/10/15/20/25 (da `depth%5==0` für beide), was z. B.
`enchStoneNeeded()` (region>1 nötig) faktisch für depth=0 deaktiviert,
aber `posNeeded`/`souNeeded`/`asNeeded` durchlaufen ungehindert dieselbe
Rechnung wie auf jeder 5./10./15. Etage — kleine Anpassung nötig, falls
depth=0 fachlich NICHT wie "Ende eines Etagen-Sets" behandelt werden soll.

### Kategorie B: Feste Switches über einzelne Etagen (`newLevel`, `standardMobRotation`, u. Ä.)

Ca. 7 Fundstellen mit `switch(depth)`/`switch(Dungeon.depth)`, plus die
tabellenbasierten Nachfolger (`MobRegistry`, `StandardRoom.chances`, s.
bereits bekannter Punkt 3/4).

Beispiele:

- `Dungeon.java:297` `newLevel()` — `switch(depth)` über `branch==0`
  Sewers/Prison/Caves/City/Halls/LastLevel, `default: new DeadEndLevel()`.
- `actors/mobs/MobRegistry.java:70-84` — `bracketOfDepth(depth)` hat
  bereits einen expliziten depth=0-Kommentar/Fallback: "depth 0 (and
  anything else outside 1-26) falls back to bracket 0, reproducing
  standardMobRotation's 'case 1: default:'".
- `levels/rooms/special/ShopRoom.java:228` `generateItems()` —
  `switch(Dungeon.depth) { case 6: default: ... }` — depth=0 fällt auf
  den `case 6`-Zweig (Leder-Rüstung/Tier-1-Waffen) statt zu crashen.
- `Badges.java:840,912` — `switch(Dungeon.depth)` für Boss-Slain/Boss-
  Challenge-Badges, nur `case 5/10/15/20/25`, kein `default` mit
  Nebenwirkung (Badge bleibt einfach unvergeben bei depth=0 — unkritisch).

**Einschätzung:** Gemischt, aber überwiegend "funktioniert automatisch,
ohne zu crashen" — mit zwei bemerkenswerten Ausnahmen:
- `Dungeon.newLevel()` liefert für depth=0 aktuell `DeadEndLevel` (über
  den `default`-Zweig). Das ist *kein* Crash, aber auch keine "echte"
  Etage 0 — wer depth=0 als spielbare Oberflächen-Etage will, muss hier
  einen eigenen `case 0:`-Zweig ergänzen.
- `ShopRoom.generateItems()` hat ein `case 6: default:`-Muster, das
  depth=0 (falls dort je ein Shop-Room gebaut würde) unbemerkt mit
  Etage-6-Shop-Inhalten befüllen würde. In der Praxis vermutlich
  ungefährlich, weil `Dungeon.shopOnLevel()` (`depth==6||11||16`) für
  depth=0 `false` liefert und Shop-Rooms dadurch dort gar nicht gebaut
  werden — aber es ist ein weiteres Beispiel für "0 fällt in einen
  `default`-Zweig, der eigentlich für eine andere, konkrete Etage gedacht
  war", strukturell verwandt mit dem bereits bekannten `StandardRoom`-
  Problem.
- `MobRegistry.bracketOfDepth` ist der einzige Fundort in Segment 1, der
  depth=0 bereits *bewusst* und *dokumentiert* behandelt (Kommentar
  Zeile 77-78) — als Vorlage dafür, wie ein sauberer depth=0-Fallback
  aussehen könnte.

### Nachtrag: Musterzuordnung der restlichen 13 Dateien aus dem Kategorie-A-Grep

Die ursprüngliche grobe Schätzung ("~15+ Fundstellen") deckte 18 vom Grep
getroffene Dateien ab, von denen in der ersten Runde nur `Dungeon.java`,
`Generator.java`, `RegularLevel.java` und `DriedRose.java` einzeln
klassifiziert wurden. Die restlichen 13 wurden jetzt nachklassifiziert
(nur Musterzuordnung, keine Tiefenanalyse):

**`(depth-1)/5`-Muster:** 1 Datei
- `tiles/TerrainFeaturesTilemap.java:69` — `int stage = (Dungeon.depth-1)/5;`
  (Tile-Variantenstufe für Gras/Bewuchs). Für depth=0 identisches
  Rundungsverhalten wie in Segment 1 beschrieben: `(0-1)/5 == 0`, also
  `stage=0` — aliast mit Etage 1-5, kein Crash (nur Multiplikator in
  einer Formel, keine Array-Indizierung).

**`depth/5` bzw. `depth%5`-Muster:** 12 Dateien
- `rooms/special/TrapsRoom.java` (Trap-Auswahl `levelTraps[depth/5]`,
  plus zwei `randomWeapon/randomArmor((depth/5)+1)`-Aufrufe)
- `rooms/special/WeakFloorRoom.java` (Tilemap-Variante)
- `rooms/special/SacrificeRoom.java`
- `rooms/special/SentryRoom.java` (3 Aufrufe, `(depth/5)+1`)
- `rooms/special/PoolRoom.java` (3 Aufrufe, `(depth/5)+1`)
- `rooms/special/LaboratoryRoom.java` (`chapter = 1+depth/5`, nur für
  Vergleiche/Arithmetik, keine Indizierung)
- `rooms/special/CryptRoom.java`
- `rooms/secret/SecretMazeRoom.java` (2 Aufrufe, `(depth/5)+1`)
- `rooms/secret/SecretRoom.java` (`region=depth/5`, `floor=depth%5` in
  `secretsForFloor(depth)`)
- `rooms/secret/SecretLarderRoom.java`
- `effects/particles/SpectralWallParticle.java` (2 Stellen, `1+depth/5`,
  oben bei 5 gekappt)
- `actors/mobs/npcs/Shopkeeper.java:202` (`sellPrice`, `depth/5+1`)

Insgesamt also 1:12 zugunsten von `depth/5`/`depth%5` unter diesen 13
Dateien — das `(depth-1)/5`-Muster (rundungsanfällig) bleibt auf die in
Segment 1 bereits gefundenen Stellen (`RegularLevel.java`,
`DriedRose.java`, jetzt zusätzlich `TerrainFeaturesTilemap.java`)
konzentriert.

### Nachtrag: weitere `chances[27]`-artige Crashes bei depth=0?

Keine gefunden. Alle direkt indexierenden Verwendungen in diesen 13
Dateien landen bei depth=0 auf einem gültigen Index 0 (kein
Out-of-Bounds):

- `TrapsRoom.levelTraps[Dungeon.depth/5]` — Array hat 5 Einträge
  (Sewers/Prison/Caves/City/Halls, Index 0-4); `depth/5` für depth=0
  ergibt Index 0 (Sewers-Fallen). Kein Crash, aber wieder dieselbe
  Aliasing-Art wie beim `MobRegistry`-Bracket: depth=0 verhält sich wie
  Etage 1-5.
- `SecretRoom.regionSecretsThisRun[region]` — Array hat ebenfalls 5
  Einträge; `region=depth/5` für depth=0 ergibt Index 0. Kein Crash,
  gleiche Aliasing-Art.
- Alle `Generator.randomWeapon/randomArmor/randomMissile((depth/5)+1)`-
  Aufrufe (TrapsRoom, SacrificeRoom, SentryRoom, PoolRoom, CryptRoom,
  SecretMazeRoom) laufen durch das in Segment 1 bereits dokumentierte
  `GameMath.gate(0, floorSet, floorSetTierProbs.length-1)` — für depth=0
  ergibt `(0/5)+1=1`, ohnehin im gültigen Bereich.
- `TerrainFeaturesTilemap.java`s `stage` und `LaboratoryRoom.java`s
  `chapter` sind reine Arithmetik-/Vergleichswerte, keine Array-Indizes.

Fazit: Der `chances[27]`/NPE-Fehler aus den bereits bekannten Funden
(`StandardRoom`, `EntranceRoom`, `ExitRoom`, `ConnectionRoom`) scheint
ein spezifisches Muster dieser vier Raumklassen zu sein (fest 27-lange
Wahrscheinlichkeits-Arrays ohne Gate/Clamp), nicht ein generelles Muster
der `depth/5`-Verwendung in der Codebase. Die `depth/5`-Stellen sind
durchgängig entweder gegated (`Generator`) oder zeigen auf Arrays, die
für Index 0 ohnehin belegt sind.

---

**Segment 1: abgeschlossen.**

---

## Segment 2 — Speicher/Serialisierung, UI-Anzeige, weitere 0-als-Sonderwert-Kollisionen

### Kategorie C: Speicher-/Serialisierungscode, der depth als rohen int behandelt

Sehr viele Fundstellen (>15 `bundle.put(DEPTH, depth)`/`bundle.getInt(DEPTH)`-
Paare in Buffs, Items, Artefakten). Durchgehendes Muster: `depth` wird 1:1
als Bundle-Int gespeichert/gelesen, ohne eigene depth=0-Behandlung — der
Bundle-Layer selbst crasht nirgends bei 0.

Beispiele:
- `Dungeon.java:611,638,815` — Haupt-Spielstand, `depth`/`branch` als
  simple Ints.
- `actors/buffs/SuperNovaTracker.java`, `RevealedArea.java`,
  `actors/hero/abilities/mage/WarpBeacon.java` (`WarpBeaconTracker`) —
  Buffs, die eine Zielposition inkl. `depth` speichern. Hier ist der
  Buff selbst (vorhanden/nicht vorhanden) das "ist gesetzt"-Flag, nicht
  der `depth`-Wert — depth=0 wäre also unproblematisch speicherbar.
- `items/keys/Key.java` — Schlüssel merken sich `depth`, roh gespeichert,
  keine Sonderbehandlung.

**Einschätzung:** Funktioniert bei depth=0 durchweg automatisch — mit
einer wichtigen Ausnahme, die in Kategorie E unten näher ausgeführt wird
(`Bones.java`).

**Bemerkenswert – bereits etablierte Konvention für "kein Wert":** Mehrere
Speicherstellen verwenden für "Depth nicht gesetzt" bereits **-1**, nicht
0:
- `items/artifacts/LloydsBeacon.java:61` `returnDepth = -1`, geprüft via
  `returnDepth != -1` (Zeilen 83, 100, 160, 303, 313).
- `items/spells/BeaconOfReturning.java` — dieselbe `returnDepth = -1`
  /`!= -1`-Konvention, zweimal (Haupt- und innere Klasse).
- `Bones.java:50` `private static int depth = -1;`, geprüft via
  `if (depth == -1)` (Zeile 160).
- `Dungeon.java:746` `Dungeon.depth = -1;` in `loadGame()`, ausgewertet in
  `InterlevelScene.java:740` `if (Dungeon.depth == -1) { Dungeon.depth =
  Statistics.deepestFloor; ... }` — ein "kein Wert, nimm den tiefsten
  erreichten Floor"-Fallback für den Wiedereinstieg nach Spielstart.

Das ist für Segment 4 relevant: **-1 als Sentinel für "keine Etage" ist
kein neuer Vorschlag, sondern bereits durchgängige Praxis** in der
Codebase — nur `Level1Transition.SURFACE` und der Lese-Pfad von
`Bones.java` nutzen stattdessen 0.

### Kategorie D: UI-Anzeigen, die depth in Text umwandeln

Durchgehend **roh, ohne Offset** — depth=0 würde überall wörtlich "0"
bzw. "floor 0" anzeigen, kein Crash:

- `ui/MenuPane.java:91` — HUD-Etagenanzeige:
  `Integer.toString( Dungeon.depth )`, keine Umrechnung.
- `journal/Notes.java:461` (`CustomRecord`) —
  `Integer.toString(depth())` im Journal.
- `messages/scenes/scenes.properties:31,35,36` (`gamescene.descend`,
  `.return`, `.resurrect`) — "You descend to floor %d of the dungeon."
  mit rohem `Dungeon.depth` aus `GameScene.java:599,628,630`. Für
  depth=0 ergäbe das wörtlich "You descend to floor 0 of the dungeon."
  — kein Crash, aber ungeprüft, ob das als Text gewünscht ist (z. B.
  könnte eine Oberflächen-Etage stattdessen einen eigenen String statt
  "floor 0" verdienen).

**Einschätzung:** Funktioniert technisch automatisch (keine Arrays, keine
Indizierung), aber alle Anzeigen sind unvorbereitet auf einen sprechenden
Namen für Etage 0 ("Oberfläche" o. Ä.) — reine Zahlenanzeige übersetzt
nicht automatisch zu einer sinnvollen Beschriftung.

### Kategorie E: Weitere 0-als-Sonderwert-Kollisionen (gezielte Suche, wie in Segment 1 der Vorsitzung gefordert)

1. **`Bones.java:167` `if (depth > 0) { ... item/heroClass laden ... }`**
   — präzisiert den bereits bekannten Fund #2. Der In-Memory-Sentinel für
   "keine Überreste geladen" ist korrekt `-1` (Zeile 50, geprüft in
   Zeile 160), aber der anschließende Lese-Pfad hat eine *zweite*,
   unabhängige 0-Kollision: Selbst wenn `bundle.getInt(LEVEL)`
   erfolgreich einen gespeicherten `depth=0` zurückliefert, verhindert
   die Bedingung `depth > 0` in Zeile 167 das Laden von `item`/
   `heroClass` für genau diesen Fall. Überreste auf einer echten Etage 0
   würden also stumm ohne Gegenstand/Heldenklasse geladen.

2. **`scenes/InterlevelScene.java:167-178`** — `int region =
   (int)Math.ceil(loadingDepth / 5f);` ergibt für `loadingDepth=0`
   `region=0`. Der nachfolgende `switch(lastRegion)` hat aber nur
   `case 1` bis `case 5`, mit **`case 5: default:`** (Zeile 207) für
   Halls. Ladebildschirm-Hintergrund für depth=0 würde also unbemerkt
   das Halls-Splash-Bild zeigen — strukturell identisch zum bereits in
   Segment 1 gefundenen `ShopRoom.java:229` `case 6: default:`-Muster:
   ein für eine bestimmte, hohe Etage gedachter `default`-Zweig fängt
   depth=0 mit auf.

3. **`SpecialRoom.resetPitRoom(Dungeon.depth+1)`**
   (`InterlevelScene.java:809`) — nutzt `depth+1`, kein Kollisionsrisiko
   für depth=0 selbst (ergibt 1), aber zeigt, dass an dieser Stelle
   bereits mit einer Verschiebung gearbeitet wird; erwähnt hier nur als
   Beleg, dass `depth+1`-Muster im Loading-Code neben den ungeschützten
   `depth/5`-Stellen koexistieren.

4. Kein weiterer Fund einer dritten unabhängigen 0-Sentinel-Stelle
   außerhalb der bereits bekannten (`Level1Transition.SURFACE`,
   `Bones`) und der beiden oben neu gefundenen (`Bones.java:167`,
   `InterlevelScene`-Regions-Switch) im Rahmen dieser Suche — die
   Kollisionen scheinen sich auf "Region/Etagen-Klassifizierung via
   `default`-Zweig" und "Bundle-Lesepfade mit `> 0`-Guard" zu
   konzentrieren, nicht auf breit gestreute Einzelfälle.

---

**Segment 2: abgeschlossen.**

---

## Segment 3 — items/actors/buffs & weitere 0-als-Sonderwert-Kollisionen

Dritter, in der Vorsitzung nie gestarteter Themenbereich. ~33 Dateien
referenzieren `Dungeon.depth` in `items/`+`actors/` (17+16, grober
Grep-Treffer, Überschneidung mit bereits in Segment 1/2 behandelten
Dateien wie `LloydsBeacon`/`DriedRose`/`SuperNovaTracker` etc. ausgenommen).

### Kategorie F: depth-Referenzen in items/actors/buffs

Fast durchgehend **reine Skalierungs-Arithmetik** (HP/Schaden/Rüstung
nach Tiefe), kein Indexing, daher kein Crash-Risiko bei depth=0:

- `actors/mobs/Statue.java:58-59,98,113`, `ArmoredStatue.java:46`,
  `actors/mobs/Piranha.java:65-95` — `HP = 15+depth*5`,
  `defenseSkill = 4+depth`, `Random.NormalIntRange(0, depth+...)` usw.
  Für depth=0 ergeben sich durchweg die schwächstmöglichen, aber
  gültigen Werte (z. B. `NormalIntRange(0,0)` liefert deterministisch 0,
  kein Crash).
- `items/wands/CursedWand.java:755-761` — einzige Stelle mit einer
  potenziell gefährlichen Konstruktion, `float[] depths = new
  float[Dungeon.depth-1]`, die bei depth=0 ein `NegativeArraySizeException`
  auslösen würde — ist aber durch die vorausgehende Bedingung
  `Dungeon.depth > 1` bereits abgesichert und für depth=0 unerreichbar.
  Kein Crash, aber ein Beispiel dafür, dass depth-Arithmetik in
  Waffen-/Zauber-Code stellenweise schon bewusst gegen kleine
  Tiefenwerte abgesichert ist (Vorlage-würdig).
- `items/scrolls/exotic/ScrollOfPassage.java:55` —
  `Math.max(1, (Dungeon.depth-1-(Dungeon.depth-2)%5))` als "Rücksprung
  zum Regionsanfang". Ergibt für depth=0 rechnerisch `1` (Java-Vorzeichen-
  Modulo `(0-2)%5=-2`), damit bereits selbst nach unten auf Etage 1
  geklemmt — kein Crash, konsistent mit der "Region-1"-Aliasing-Art aus
  Segment 1.
- `items/rings/RingOfWealth.java:273` `floorset=(depth+level)/5` — läuft
  in `Generator.randomWeapon/randomArmor(floorset)`, dort bereits
  gegated (s. Segment 1).
- `items/artifacts/SkeletonKey.java:568-573` `ironKeysNeeded = new
  int[26]` (ebenso golden/crystal) — mit `Arrays.fill(..., -1)`
  vorinitialisiert, `-1` geprüft als "für diese Etage noch nicht
  aufgesetzt" (Zeilen 606,614,622). Index 0 liegt innerhalb des
  Arrays (0-25) — kein Crash, aber ein weiterer Beleg für die in
  Segment 2 dokumentierte **-1-als-„kein Wert"-Konvention**.
- `actors/hero/Hero.java:2523-2529` — bemerkenswert: die Kommentare dort
  lauten wörtlich *"unintentional trap detection scales from **40% at
  floor 0** to 30% at floor 25"* und *"door detection scales from **20%
  at floor 0** to 0% at floor 20"*. Der Code selbst
  (`chance = 0.4f - depth/250f` bzw. `0.2f - depth/100f`) ist bereits so
  geschrieben, dass depth=0 der rechnerische Ausgangspunkt ist — ein
  seltener Fall, in dem „Etage 0" im Code schon *implizit als gültiger
  Basiswert* mitgedacht wurde, obwohl `Dungeon.depth` heute nie 0 wird.

### Kategorie G: weitere 0-als-Sonderwert-Kollisionen (gezielte Suche)

**Neuer, eigenständiger Fund — `Statistics.highestAscent`:**

- `Statistics.java:82` `highestAscent = 0;` (Default/Reset).
- `levels/HallsBossLevel.java:352` `Statistics.highestAscent = 25;` —
  Startwert beim Beginn der Aufstiegs-Herausforderung (Ascension) nach
  Sieg über den letzten Boss.
- `actors/buffs/AscensionChallenge.java:252-253` `if (Dungeon.depth <
  Statistics.highestAscent) { Statistics.highestAscent = Dungeon.depth;
  ... }` — trackt beim Aufstieg die *kleinste* erreichte Tiefe. Diese
  Logik selbst verträgt depth=0 sauber (0 < 25 z. B. ist wahr, würde
  korrekt aktualisieren).
- **Aber:** `Rankings.java:105` und `windows/WndRanking.java:235` nutzen
  `Statistics.highestAscent == 0` als Flag für **"Spieler hat nie
  aufgestiegen"** (Zeile 105-111: ist es 0, wird stattdessen
  `Statistics.deepestFloor`/`ascending=false` verwendet). Würde ein
  Spieler die Aufstiegs-Herausforderung tatsächlich bis Etage 0
  abschließen, würde dieser Fund fälschlich als "nie aufgestiegen"
  gewertet und der Rankings-Eintrag zeigt `deepestFloor` statt der
  echten Aufstiegstiefe 0 an. Strukturell identisch zum bereits
  bekannten `Statistics.deepestFloor`-Problem (#5), aber eine
  **eigenständige zweite Fundstelle in derselben Statistik-Klasse**,
  bisher nicht in den bekannten Funden erwähnt.

Kein weiterer, von den bereits gefundenen Mustern unabhängiger
0-Sonderwert in `items/`/`actors/`/`buffs/` gefunden.

---

**Segment 3: abgeschlossen.**

---

## Zwischen-Nachtrag: Statistics.java komplett durchgesehen

Auf Wunsch vollständig gelesen (nicht erneut über die 33 Verbrauchsdateien
gesucht, nur `Statistics.java` selbst plus gezielter Grep nach
`Statistics.<Feld> ==/>/< 0` in der übrigen Codebase). Die Klasse hat 18
`int`/`float`-Felder, alle in `reset()` explizit auf `0` gesetzt. Fundstellen,
an denen ein Feld andernorts als "==0/>0 bedeutet nie erreicht/nie gesetzt"
geprüft wird:

1. **`deepestFloor`** — bereits bekannter Fund #5 (nur `>`, kein `==0`-Check
   gefunden, aber das strikte `>` hat denselben Effekt: 0 kann nie "erreicht"
   registriert werden).
2. **`highestAscent`** — neu in Segment 3 gefunden: `Rankings.java:105` und
   `WndRanking.java:235` nutzen `== 0` als "Spieler ist nie aufgestiegen".
3. **`heldItemValue`** — **neuer Fund, aber kein Depth-Bezug:**
   `Rankings.java:179` `if (Statistics.heldItemValue == 0) { ... berechne
   neu ... }` ist ein Memoization-Muster ("noch nicht berechnet" = 0). Eine
   Kollision entstünde nur, wenn ein Held tatsächlich Gegenstände im
   Gesamtwert von genau 0 mit sich trägt (z. B. leeres Inventar) — dann
   würde `calculateScore()` bei jedem Aufruf harmlos neu (mit demselben
   Ergebnis 0) rechnen. Kein Crash, keine Fehlanzeige, nur unnötige
   Neuberechnung.
4. **`spawnersAlive`** — `actors/mobs/YogDzewa.java:577`
   `if (Statistics.spawnersAlive > 0)`. Kein Sonderwert-Muster, sondern ein
   normaler "ist die Zahl größer null"-Zählercheck (aktive Spawner
   vorhanden ja/nein) — hier zum Vergleich aufgeführt, weil es strukturell
   dem Grep-Muster entspricht, aber es handelt sich nicht um dieselbe
   Fehlerklasse (kein "0 heißt eigentlich unbekannt/nicht gesetzt").

Alle übrigen Statistics-Felder (`goldCollected`, `enemiesSlain`,
`foodEaten`, `itemsCrafted`, `piranhasKilled`, `hazardAssistedKills`,
`ankhsUsed`, `progressScore`, `treasureScore`, `exploreScore`,
`totalBossScore`, `totalQuestScore`, `upgradesUsed`, `sneakAttacks`,
`thrownAttacks`, `duration`) haben keine `==0`/`>0`/`<0`-Vergleichsstelle
außerhalb von `Statistics.java` selbst gefunden — entweder ungenutzt für
solche Schwellenwert-Prüfungen oder nur additiv/statistisch verwendet.

**Fazit:** Von den 18 Feldern folgen zwei (`deepestFloor`, `highestAscent`)
dem depth-relevanten "0 heißt nie erreicht"-Muster — beide bereits
dokumentiert (Fund #5 bzw. Segment 3). Kein drittes depth-bezogenes Feld
in `Statistics.java` gefunden.

---

## Segment 4 — Konsolidierter Design-Vorschlag

### a) Design-Vorschlag: `RegionDefinition[]`

Die in `CLAUDE.md` (Fallstrick #7) benannte harte Kodierung von Etage 1-26
verteilt sich über mindestens acht strukturelle Stellen plus die in
Segment 1-3 gefundenen `default`-Zweig-Kollisionen (`ShopRoom`,
`InterlevelScene`-Splash-Switch) und die verstreute `depth/5`-Arithmetik.
Vorschlag: eine einzige Tabelle als Ersatz für die meisten dieser Stellen:

```
// Pseudocode - kein kompilierbarer Vorschlag, nur zur Veranschaulichung
public class RegionDefinition {
    public final int index;                              // 0-basierte Regionsnummer
    public final int firstDepth;                          // erste Etage dieser Region
    public final int floorCount;                          // normal 5
    public final Class<? extends RegularLevel> levelClass;
    public final Class<? extends RegularLevel> bossLevelClass; // null = keine Boss-Etage
    public final boolean hasShop;
    public final Document loreDocument;                    // ersetzt RegularLevel.java:594-Switch
    public final String splashAsset;                       // ersetzt InterlevelScene-Switch
}

public static final RegionDefinition[] REGIONS = {
    // Sewers, Prison, Caves, City, Halls, ...
};

public static RegionDefinition regionOf(int depth) {
    // lookup by firstDepth
}
```

Migrierte Konsumenten (Auswahl, nicht vollständig):

- `Dungeon.newLevel()` (Zeile 297) → `REGIONS[regionOf(depth).index].levelClass`/
  `.bossLevelClass` statt `switch(depth){...default: new DeadEndLevel()}` —
  macht dabei den bisher stillen `default`-Fallback für depth=0 explizit.
- `Dungeon.bossLevel(depth)` (Zeile 441) → Vergleich gegen
  `regionOf(depth).firstDepth + floorCount - 1` statt Literal-Liste
  `5/10/15/20/25`.
- `ShopRoom.generateItems()` (Zeile 228) → Auswahl über `regionOf(depth).index`
  statt `switch(Dungeon.depth){case 6: default: ...}` — entfernt die in
  Segment 2 gefundene Kollision strukturell, statt sie nur zu patchen.
- `InterlevelScene`s Splash-Switch (Zeile 167-207) → `regionOf(loadingDepth).splashAsset`
  statt `Math.ceil(loadingDepth/5f)` + `case 5: default:`.
- `RegularLevel.java:594` (Regions-Dokument-Switch), `DriedRose.java`s
  Dialog-Switches → `regionOf(depth).loreDocument`/`.index` statt
  `(depth-1)/5`.
- `MobRegistry.BRACKET_DEPTHS` bleibt bewusst **unverändert** — 20
  Brackets statt 5 Regionen ist laut eigenem Klassenkommentar Absicht
  (feinere Granularität als `StandardRoom`); `bracketOfDepth`s depth<0-
  Fallback (Zeile 82) ist bereits das Vorbild für `regionOf()`s eigene
  Randbehandlung.

**Nicht** durch `REGIONS[]` ersetzt: die ~290 einzelnen
`Dungeon.depth`-Referenzen (Fallstrick #7) und die in Segment 1/3
gefundene `depth/5`-Skalierungsarithmetik in `items/`/`actors/` (HP-,
Schadens-, Dropchancen-Formeln) — die bleiben bewusst als Arithmetik über
den flachen `depth`-Wert bestehen, s. offene Frage 2 unten.

### b) Die drei ursprünglichen offenen Fragen

**1. Einsortierung von Ebene 0.** Zwei Optionen: (i) Etage 0 als
zusätzliche "Prolog/Oberflächen"-Etage *unterhalb* der heutigen Etage 1
einfügen, Etagen 1-26 bleiben unverändert; oder (ii) alles um 1
verschieben (heutige Etage 1 → 2, neue Etage 1 = heutige Oberfläche).
**Empfehlung: (i).** Alle in Segment 1-3 gefundenen harten Literale
(Boss-Etagen 5/10/15/20/25, Shop-Etagen 6/11/16, `SkeletonKey`s
`int[26]`-Arrays, `Statistics.floorsExplored`-Schleifen `for(i=1;i<26)`,
Badge-Switches) sind an die *heutigen* Zahlen gebunden. Bei (i) bleiben
sie unangetastet; bei (ii) müsste jede einzelne dieser Stellen mitgezogen
werden — ein erheblich größerer, fehleranfälligerer Eingriff für einen
rein kosmetischen Nummerierungswunsch. Passt außerdem zur heutigen
Bedeutung von `Level1Transition.SURFACE = 0` als Einstiegspunkt/Hub, nicht
als erste Dungeon-Etage.

**2. depth als fortlaufende Zahl vs. Region+Offset.** Empfehlung: `depth`
bleibt der flache, fortlaufende, gespeicherte `int` (wie heute) — `REGIONS[]`
ist eine reine Ableitungs-/Lookup-Schicht, keine Änderung des
Speicherformats. Begründung: `Dungeon.depth` wird laut Fallstrick #7 an
~290 Stellen direkt gelesen; ein Wechsel auf ein `(region, offset)`-Paar
als Kernrepräsentation würde diese 290 Stellen potenziell berühren statt
nur die ~8 strukturellen Hotspots plus die in Segment 1-3 katalogisierten
`default`-Kollisionen. Auch das gespeicherte Bundle-Format
(`Dungeon.java:611`, `DEPTH="depth"`) bliebe unverändert — relevant, weil
Umbenennungen/Formatwechsel laut `CLAUDE.md` ohnehin mit Bundle-Alias-
Vorsicht zu behandeln sind.

**3. Relative vs. feste Boss-Meilensteine.** Direkte Folge aus Frage 1:
bei Einsortierung von Ebene 0 *unterhalb* der bestehenden Struktur (Option
i) bleiben die festen Meilensteine 5/10/15/20/25 unverändert gültig —
`REGIONS[]` bildet sie nur nach, ändert sie nicht. Eine Umstellung auf
*relative* Meilensteine (z. B. "Ende jeder Region", unabhängig von
absoluten Zahlen) wäre nur bei Option (ii) zwingend nötig. Empfehlung:
feste Meilensteine beibehalten, `REGIONS[].firstDepth`/`.floorCount`
lediglich als Ableitung davon exponieren.

### c) Neue vierte offene Frage: Sentinel-Wert für "keine Etage"

Aus Segment 2/3: **-1 ist bereits die dominante, etablierte Konvention**
für "Etage nicht gesetzt" in dieser Codebase (`LloydsBeacon.returnDepth`,
`BeaconOfReturning.returnDepth` ×2, `Bones.depth`,
`Dungeon.depth=-1` in `loadGame()`/ausgewertet in
`InterlevelScene.java:740`, `SkeletonKey`s `ironKeysNeeded[]`-Arrays).
Nur eine kleine, jetzt vollständig katalogisierte Gruppe nutzt stattdessen
0: `Level1Transition.SURFACE`, `Bones.java:167`s `depth>0`-Lese-Guard,
`Statistics.deepestFloor` (striktes `>`), `Statistics.highestAscent`
(`==0`-Check in `Rankings.java`/`WndRanking.java`).

Optionen:

- **Option A — auf -1 vereinheitlichen (Empfehlung).** Die ~5 bekannten
  0-Sentinel-Stellen auf `-1` umstellen, passend zur bereits
  überwiegenden Konvention. Kleinster Änderungsradius: nur diese
  konkret gefundenen Stellen, keine der ~290 `Dungeon.depth`-Lesestellen
  betroffen, da die meisten davon depth als Wert nutzen, nicht als
  Sentinel prüfen. Macht `Bones.java` intern konsistent (Zeile 50 nutzt
  bereits -1, Zeile 167 noch 0/`>`).
- **Option B — 0 und -1 beide als "kein Wert" reservieren, echte Etagen
  bei 1 belassen (Ebene 0 wird intern z. B. als "Etage -1" oder
  gesonderter Marker geführt).** Vermeidet jede Sentinel-Migration,
  verschiebt das Problem aber auf Frage 1 (Einsortierung) und bricht mit
  der in `CLAUDE.md`/Fallstrick #7 dokumentierten Zahlenlogik
  (`depth/5`-Kapitelrechnung ginge nicht mehr für die neue unterste
  Etage auf).
- **Option C — nichts vereinheitlichen, nur die ~5-6 konkret gefundenen
  Kollisionsstellen einzeln patchen.** Geringster Aufwand kurzfristig,
  lässt aber die bestehende Inkonsistenz (0 an manchen, -1 an anderen
  Stellen) bestehen und erhöht das Risiko, dass ein künftiger Fund #7
  auftaucht, der hier nicht mit erfasst wurde.

Keine Vorfestlegung von meiner Seite über die Empfehlung hinaus — das ist
eine Entscheidung, die ausdrückliche Ansage braucht (vgl.
`CLAUDE.md`-Arbeitsregel zu Verhaltensänderungen).

### d) Risikoeinschätzung (Golden-Master-Tests)

Drei Golden-Master-Tests existieren: `GeneratorGoldenMasterTest`,
`StandardRoomGoldenMasterTest` (`FIXED_DEPTHS = {1,3,5,8,13,18,24}`),
`MobSpawnerGoldenMasterTest` (`FIXED_DEPTHS` deckt 1-24 in
Chapter-Schritten ab). **Keiner der drei testet aktuell depth=0** —
alle `FIXED_DEPTHS`-Listen beginnen bei 1. Das bedeutet zweierlei:

- **Geringes Regressionsrisiko für Bestandsverhalten:** Solange die
  `REGIONS[]`-Umstellung für Etagen 1-26 exakt dieselbe Klassenwahl /
  RNG-Aufrufreihenfolge reproduziert wie die heutigen Switches (Option i
  aus Frage 1 vorausgesetzt), bleiben alle drei Golden-Master-Tests
  unverändert grün — sie decken den depth=0-Pfad schlicht nicht ab.
- **Kein automatischer Schutz für den neuen depth=0-Pfad:** Sobald
  depth=0 implementiert wird, deckt keiner der bestehenden Tests dessen
  Verhalten ab (weder Raumwahl noch Mob-Rotation). `FIXED_DEPTHS` müsste
  um `0` erweitert werden (mit frisch generiertem Erwartungswert, laut
  `docs/testing.md`-Anleitung), sonst bleibt der komplette neue Pfad
  ungetestet.
- `GeneratorGoldenMasterTest` ist von depth=0 nur indirekt betroffen
  (testet `Generator`, nicht direkt `Dungeon.depth`-Switches) — dort
  bereits bekanntes Gotcha aus `docs/testing.md`: `Generator.random()`
  braucht einen explizit geseedeten Generator für Reproduzierbarkeit,
  unabhängig vom depth=0-Thema.

### e) Aktualisierte Aufwandseinschätzung

Ursprüngliche Einschätzung "groß" bleibt **im Kern bestätigt, aber mit
klarerer, kleinerer Kernmenge als befürchtet:**

- Die vier in der Vorsitzung gefundenen Crash-Stellen (`StandardRoom`,
  `EntranceRoom`, `ExitRoom`, `ConnectionRoom`) plus die zwei hier neu
  gefundenen `default`-Kollisionen (`ShopRoom`, `InterlevelScene`-Splash)
  plus die fünf Sentinel-Stellen (`Level1Transition.SURFACE`,
  `Bones.java:167`, `Statistics.deepestFloor`, `Statistics.highestAscent`,
  s. Option A) sind **jede für sich klein und lokal** — keine davon
  erfordert einen breiten Umbau.
- Der größte Umfang liegt nicht in Einzel-Bugs, sondern in der schieren
  **Menge** an Stellen, die zwar automatisch funktionieren, aber bei
  Nicht-Beachtung eine der in Segment 1-3 dokumentierten
  Aliasing-Kollisionen (depth=0 verhält sich wie Etage 1-5 oder wie ein
  Vielfaches von 5) unbemerkt einführen — hier hilft die
  `REGIONS[]`-Konsolidierung, weil sie diese Stellen bündelt statt sie
  einzeln zu patchen.
- **Größer als ursprünglich angenommen:** die Sentinel-Frage (offene
  Frage 4) — bei Entscheidung für Option A kommt eine kleine, aber
  codebase-weite Umstellung von fünf Stellen hinzu, die in der
  ursprünglichen "groß"-Schätzung (vor den Segmenten) nicht eingepreist
  war.
- **Kleiner als ursprünglich befürchtet:** das Ausmaß der
  `depth/5`-Arithmetik — von den in Segment 1 gefundenen ~15+ Stellen
  ist keine einzige akut gefährlich (keine weiteren `chances[27]`-artigen
  Crashes gefunden); die meisten brauchen bei Wahl von Option (i) in
  Frage 1 gar keine Änderung, weil depth=0 dort ohnehin sauber auf
  "Region 1"/"vor Etage 1" abbildet.
- Golden-Master-Tests bremsen die Umsetzung nicht (kein Bestandsrisiko),
  erzeugen aber zusätzlichen Aufwand, weil für den neuen depth=0-Pfad
  selbst noch keine Testabdeckung existiert und neu aufgebaut werden
  müsste.

---

## Entscheidungen (nach Segment 4)

### Frage 1 (Einsortierung von Ebene 0): entschieden — eigene Region 0

Klärung vom Nutzer: Ebene 0 ist ein **eigener Hub mit eigenem Aufbau
("Oberfläche")** — kein raumbasiertes Layout wie die übrigen Etagen
(keine geschlossenen Räume/anderer Aufbau), aber durchaus mit Kämpfen.

Damit ist die in Segment 4a offengelassene Frage entschieden: **Ebene 0
gehört zu einer eigenen Region 0, nicht zu Region 1 (Sewers).** Ein Hub
mit anderem Layout kann nicht einfach als "eine Sewers-Etage mehr" über
`RegularLevel`/`StandardRoom`-Raumkomposition laufen — strukturell ein
eigener Level-Typ, ähnlich wie Boss-Level bereits vom
Standard-Raumsystem abweichen.

**Konsequenzen für den Design-Vorschlag (Segment 4a):**

- `REGIONS[0]` braucht eine eigene `levelClass`, die vermutlich nicht
  über die `StandardRoom`-Raumkomposition läuft, sondern einen eigenen
  Builder nutzt. `hasShop`, `bossLevelClass`, `loreDocument` vermutlich
  `false`/`null` für Region 0 — aber offen, da "durchaus auch Kämpfe"
  auf eine eigene, noch zu definierende Mob-Rotation hindeutet statt
  automatisch `MobRegistry`-Bracket 0 (Sewers-Ratten) zu übernehmen.
- **Die vier bekannten Crashes bleiben unabhängig davon fixpflichtig.**
  `StandardRoom.java:228`, `EntranceRoom`, `ExitRoom`, `ConnectionRoom`
  hängen an statischen, nach `Dungeon.depth` indizierten Arrays, nicht
  an der aktiven `Level`-Subklasse. Selbst wenn Ebene 0 über eine
  eigene, raumlose Klasse läuft und `StandardRoom.createRoom()` im
  regulären Spielfluss nie für depth=0 aufgerufen wird, bleibt das
  Risiko bestehen, sobald irgendein Code-Pfad (Editor-Tools, Vorschau,
  künftige Wiederverwendung von Raumtypen) diese Klassen bei
  `Dungeon.depth==0` aufruft — das Array kennt die aktive Level-Klasse
  nicht.
- Die in Segment 1 gefundene `(depth-1)/5`-Aliasing-"Kollision" (Ebene 0
  rutscht durch Rundung automatisch in Region 1) ist damit **eindeutig
  ein zu behebender Fehler**, keine zufällig passende Vorlage mehr:
  `RegularLevel.java:591`, `DriedRose.java:784,814`,
  `TerrainFeaturesTilemap.java:69` brauchen einen expliziten
  depth=0-Sonderfall. Für die übrigen `depth/5`-Stellen aus Segment 1/3
  (`TrapsRoom.levelTraps`, `SecretRoom.regionSecretsThisRun`,
  `RingOfWealth` u. a.) ist die praktische Auswirkung vermutlich gering,
  weil Ebene 0 laut Aufbau kaum durch die raumbasierten
  `Special-`/`SecretRoom`-Pipelines läuft, die diese Werte lesen — zu
  bestätigen, sobald der Hub-Builder feststeht.

---

**Segment 4: abgeschlossen. Analyse komplett — bereit für Entscheidung.**

# Testing

Open Pixel Dungeon can be tested headlessly - without starting the Android or Desktop
client - using JUnit 5 plus libGDX's headless backend. This lets you exercise real game logic
(item generation, level generation, hero setup, ...) directly against the actual game classes.
Turn-based combat between `Actor`s hasn't been exercised through this harness yet - it should
work the same way, but treat that as unverified until a test actually covers it.

Run all tests:

    gradlew :core:test

Run one test class:

    gradlew :core:test --tests "com.shatteredpixel.shatteredpixeldungeon.test.GeneratorGoldenMasterTest"

Test source lives in `core/src/test/java`. Test-only Gradle config is in `core/build.gradle`
(search for `test {`, `testImplementation`, and the `sourceSets` block that puts
`core/src/main/assets` on the test classpath).

## Writing a new test

Extend `GameTestBase` (`core/src/test/java/.../test/GameTestBase.java`). Its `@BeforeEach`
mirrors what `Dungeon.init()` + `Dungeon.newLevel()` do at the start of a real run: after it
runs, `Dungeon.hero` is alive and initialized, `Dungeon.level` is a freshly generated depth-1
level, and `Dungeon.seed` is fixed (`42L`) so results are reproducible. `@AfterEach` resets the
same global state again, so the next test (in this class or any other) starts clean.

```java
package com.shatteredpixel.shatteredpixeldungeon.test;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class MyFeatureTest extends GameTestBase {

    @Test
    void heroStartsWithClothArmor() {
        assertNotNull(Dungeon.hero.belongings.armor);
    }
}
```

Override these hooks when the defaults don't fit:

- `heroClass()` - which `HeroClass` to start as (default `WARRIOR`).
- `testSeed()` - the fixed `Dungeon.seed` (default `42L`).
- `generateLevelInSetup()` - return `false` to skip level generation in `@BeforeEach` when a
  test doesn't need one (faster, and doesn't spend RNG draws before your test body runs). See
  `GeneratorGoldenMasterTest` for an example.

Golden-master tests (locking down a large, observable result - like a loot distribution - so a
refactor can't silently change it without a test catching it) are a good fit for classes like
`Generator` or `StandardRoom`, where item/room classes and their selection weights are stored as
two parallel arrays held together only by their shared index - a refactor of that storage can
shift which weight belongs to which class without the compiler noticing. See
`GeneratorGoldenMasterTest` for the pattern: draw a few thousand times with
a fixed seed, tally outcomes by class name, assert the tally against a checked-in expected value.
When you need to regenerate that expected value after a deliberate change, the test class doc
comment explains the exact steps (temporarily empty the expected array, run the test, paste the
actual value from the assertion failure back in).

`BundleAliasTest` covers the savegame-rename countermeasure (audit finding #4): it simulates an
"old" save by constructing a `Bundle` with a class name that no longer exists, and checks that
`Bundle.addAlias(...)` correctly redirects it. It also runs
`ShatteredPixelDungeon.registerBundleAliases()` directly and confirms the one alias that ships
today (`WornKey` / pre-v3.3.0 `SkeletonKey`) actually resolves - so deleting that line, or
renaming `WornKey` again without updating it, fails a test instead of failing silently for
players with old saves. If you add a new `Bundle.addAlias(...)` call there, add a matching case
to this test the same way.

## Tests must not run in parallel

Game state (`Dungeon`, `Actor`, `Random`, `GamesInProgress`, ...) lives in static fields shared
by the entire JVM - there is exactly one "current" hero, one "current" level, one RNG generator
stack. Two tests running at the same time would stomp on each other's state.

`GameTestBase`'s `@BeforeEach`/`@AfterEach` reset that state between tests, which is enough
*because* JUnit runs tests one at a time by default. `core/src/test/resources/junit-platform.properties`
pins `junit.jupiter.execution.parallel.enabled=false` explicitly, so a future global JUnit
config change can't silently turn concurrency on for this project. `core/build.gradle` also sets
`maxParallelForks = 1` for the same reason, at the Gradle level (multiple JVM workers, not
JUnit's own in-process parallelism).

Do not remove either of these to "speed up" the suite - it will produce flaky, order-dependent
failures instead.

## Known boundary: `GameTestBase` stops short of `Dungeon.switchLevel()`

`GameTestBase` calls `Dungeon.init()` and `Dungeon.newLevel()`, then `Actor.init()` to register
the hero/mobs/blobs as processable actors. It deliberately does **not** call
`Dungeon.switchLevel()`, which a real run also does at this point. `switchLevel()` additionally:

- writes the save file to disk (`Dungeon.saveAll()`),
- calls into `GameScene` (fog-of-war, hero placement UI logic) that assumes a running client.

Neither is needed for testing game logic, and both add real dependencies (disk I/O, a graphical
scene) that a headless test shouldn't need. If a test genuinely needs disk-backed saves, note
that `GdxTestRuntime` already points `FileUtils` at a throwaway temp directory (not the real save
folder) - see its class comment.

## Known gotcha: `Generator.random()` needs an explicitly seeded generator to be reproducible

This was found empirically while building `GeneratorGoldenMasterTest`, not by reading the code
first - worth knowing before you write a test around anything that calls into `Generator`.

`Dungeon.init()` calls `Random.pushGenerator(seed+1)`, does a handful of seed-dependent setup
(`Scroll.initLabels()`, `Potion.initColors()`, `Ring.initGems()`, `Generator.fullReset()`), and
then calls `Random.resetGenerators()` - which throws away that seeded generator and replaces the
"current" one with a freshly constructed, **unseeded** `java.util.Random`. Everything after that
point (hero setup, and anything else that draws from `Random` without explicitly pushing its own
generator first) is genuine, non-reproducible randomness. This is deliberate upstream design -
things like your starting curses shouldn't be predictable from a level seed - not a bug.

`Generator.random()` itself only becomes reproducible again once something pushes a seeded
generator back onto the stack. Real level generation does exactly that: `Level.create()` wraps
its entire body (including the `createItems()` call that invokes `Generator.random()`) in
`Random.pushGenerator(Dungeon.seedCurDepth())` / `popGenerator()`. A test that calls
`Generator.random()` directly (outside of actual level generation, as a loot-table test typically
would) must reproduce that same bracket itself, e.g.:

```java
class MyGeneratorTest extends GameTestBase {

    @Test
    void generatorDrawIsReproducible() {
        int targetDepth = 15; // whatever depth this draw should be reproducible for
        Random.pushGenerator(Dungeon.seedForDepth(targetDepth, 0));
        // ... draw items ...
        Random.popGenerator();
    }
}
```

Skip this and you'll see something confusing: results that look random-ish but structurally
plausible, and differ on every JVM run. *Which category* wins each draw (`Generator.random()`'s
top-level `Random.chances(categoryProbs)`) always reads the shared/ambient generator, seeded or
not - so without the fix, exactly how many times each category gets drawn over your run is itself
non-reproducible. Categories with a private per-category seed (potions, scrolls, rings, wands,
stones - handled by `random(Category)`'s default branch, which pushes `cat.seed` before drawing;
artifacts go through their own `randomArtifact()`, which pushes the same way) still resolve
*which item within that category* deterministically for
a given draw count, so their sub-distributions often look identical across runs by coincidence
over a large sample - but they aren't protected from the total-draw-count drift itself, and can
still visibly differ (a wand's count moving by one is the same root cause as a weapon's). The
effect is just far more visible for weapons/armor/missiles, which are split into many small
sub-buckets where a one-count shift changes proportions noticeably. This is a hint to look for the
mechanism, not a reliable per-category test.

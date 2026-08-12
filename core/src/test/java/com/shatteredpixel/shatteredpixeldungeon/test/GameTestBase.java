package com.shatteredpixel.shatteredpixeldungeon.test;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.watabou.utils.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for tests that need a working game state without starting the Android or
 * Desktop client. Subclass it, and every {@code @Test} method starts from a freshly
 * initialized hero (see {@link #heroClass()}) on a freshly generated depth-1 level (see
 * {@link #generateLevelInSetup()}), with a fixed RNG seed (see {@link #testSeed()}).
 *
 * This mirrors what {@code Dungeon.init()} plus {@code Dungeon.newLevel()} do at the start of
 * a real run (see {@code InterlevelScene.descend()}). It deliberately stops short of
 * {@code Dungeon.switchLevel()}, which additionally saves to disk and pokes at
 * {@code GameScene}/hero-placement UI logic that assumes a running client - see
 * docs/testing.md for the exact boundary and why it's there.
 *
 * Game state (Dungeon, Actor, Random, GamesInProgress, ...) lives in static fields shared by
 * the whole JVM. Tests using this base class MUST NOT run concurrently - see
 * src/test/resources/junit-platform.properties, which disables JUnit's parallel execution for
 * this exact reason.
 */
public abstract class GameTestBase {

	private static final long DEFAULT_TEST_SEED = 42L;

	@BeforeEach
	final void baseSetUp() {
		GdxTestRuntime.ensureStarted();
		resetGlobalState();

		GamesInProgress.selectedClass = heroClass();

		//bypasses Dungeon.initSeed(), which would pull in SPDSettings' custom-seed/daily logic -
		//tests want one fixed, explicit seed rather than whatever the last run left behind
		Dungeon.seed = testSeed();
		Dungeon.customSeedText = "";
		Dungeon.daily = false;
		Dungeon.dailyReplay = false;

		Dungeon.init();

		if (generateLevelInSetup()) {
			Dungeon.level = Dungeon.newLevel();
			Actor.init();
		}
	}

	@AfterEach
	final void baseTearDown() {
		resetGlobalState();
	}

	private void resetGlobalState() {
		//pops/re-seeds every pushed RNG generator in one step, so a test that throws mid-way
		//through a push/pop pair (e.g. inside level generation) can't leak RNG state forward
		Random.resetGenerators();

		Actor.clear();
		Actor.resetNextID();

		Dungeon.hero = null;
		Dungeon.level = null;
		Dungeon.depth = 0;
		Dungeon.branch = 0;
		Dungeon.generatedLevels.clear();

		GamesInProgress.selectedClass = null;
	}

	/** Hero class used to initialize {@link Dungeon#hero} in {@code @BeforeEach}. */
	protected HeroClass heroClass() {
		return HeroClass.WARRIOR;
	}

	/** Fixed RNG seed used for every test in this class, for reproducible results. */
	protected long testSeed() {
		return DEFAULT_TEST_SEED;
	}

	/**
	 * Whether {@code @BeforeEach} should generate a depth-1 {@link Level} into
	 * {@link Dungeon#level}. Override to return {@code false} in test classes that don't need
	 * a level (e.g. pure loot-table tests) to keep setup fast and avoid spending RNG draws on
	 * level generation before the test body runs.
	 */
	protected boolean generateLevelInSetup() {
		return true;
	}
}

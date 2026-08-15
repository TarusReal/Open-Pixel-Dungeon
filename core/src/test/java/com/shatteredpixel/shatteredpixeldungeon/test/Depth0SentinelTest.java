package com.shatteredpixel.shatteredpixeldungeon.test;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the depth=0 "no floor" sentinel unification (docs/depth0-findings.md Segment 1 /
 * docs/depth0-implementation.md): {@link Statistics#deepestFloor} and
 * {@link Statistics#highestAscent} now default to {@code -1} instead of {@code 0}, so a real
 * depth 0 can be distinguished from "never reached".
 *
 * <p>{@link Statistics#reset()} (called by {@code Dungeon.init()}) is the "fresh state" this
 * class exercises; {@code generateLevelInSetup()} is disabled so {@code GameTestBase} doesn't
 * immediately advance {@code deepestFloor} to 1 by generating a depth-1 level before the test
 * body runs.
 *
 * <p><b>What this class does NOT cover yet:</b> {@code Dungeon.newLevel()} (Dungeon.java:385)
 * still only runs its {@code deepestFloor} update for levels that aren't {@code DeadEndLevel}/
 * {@code VaultLevel} - and depth 0 currently falls through {@code newLevel()}'s {@code default}
 * case to {@code DeadEndLevel} (no Region 0 level class exists yet, that's Segment 3). So the
 * first test below reproduces the exact comparison at Dungeon.java:385 directly on
 * {@code Statistics}/{@code Dungeon} fields rather than calling {@code newLevel()}, to isolate
 * "does the sentinel arithmetic work" from "is depth 0 routed to a real level class yet". Once
 * Segment 3 gives Region 0 a real (non-DeadEndLevel) level class, this comparison starts firing
 * for real inside {@code newLevel()} automatically - no further change needed here - and the
 * Segment 4 smoke test exercises that end-to-end.
 */
class Depth0SentinelTest extends GameTestBase {

	@Override
	protected boolean generateLevelInSetup() {
		return false;
	}

	@Test
	void freshStatisticsDefaultToSentinelNotZero() {
		// Dungeon.init() (called by GameTestBase's @BeforeEach) calls Statistics.reset()
		assertTrue(Statistics.deepestFloor == -1, "deepestFloor should default to -1, not 0");
		assertTrue(Statistics.highestAscent == -1, "highestAscent should default to -1, not 0");
	}

	@Test
	void reachingDepthZeroNowRegistersAsDeepestFloor() {
		// mirrors the exact condition at Dungeon.java:385 (Dungeon.newLevel())
		Dungeon.depth = 0;
		Dungeon.branch = 0;
		assertTrue(Dungeon.depth > Statistics.deepestFloor && Dungeon.branch == 0,
				"with deepestFloor's new -1 default, reaching depth 0 must register as a new deepest floor");

		// simulate what Dungeon.newLevel() would now do with that result
		Statistics.deepestFloor = Dungeon.depth;
		assertTrue(Statistics.deepestFloor == 0);

		// contrast: under the old 0-default this same depth-0 arrival would have been silently
		// ignored, because strict '>' can never be satisfied by two equal zeros
		int oldDefault = 0;
		assertFalse(Dungeon.depth > oldDefault,
				"documents why the old 0 default made depth 0 unable to ever register as 'reached'");
	}

	@Test
	void ascendingToDepthZeroIsDistinguishableFromNeverAscended() {
		// AscensionChallenge.onLevelSwitch() sets Statistics.highestAscent = Dungeon.depth as the
		// hero ascends past each floor; simulate it having counted all the way down to floor 0
		Statistics.highestAscent = 0;

		// mirrors the fixed check in Rankings.java:105 / WndRanking.java:235
		boolean neverAscended = Statistics.highestAscent == -1;
		assertFalse(neverAscended, "reaching floor 0 during an ascent must not be reported as 'never ascended'");

		// contrast: under the old 0-default, highestAscent == 0 was indistinguishable from
		// "the hero never started an ascent at all"
		boolean oldNeverAscendedCheck = Statistics.highestAscent == 0;
		assertTrue(oldNeverAscendedCheck,
				"documents the old collision: reaching floor 0 looked identical to never ascending");
	}
}

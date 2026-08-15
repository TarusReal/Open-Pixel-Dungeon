package com.shatteredpixel.shatteredpixeldungeon.test;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.levels.CavesBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.CavesLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.CityBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.CityLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.DeadEndLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.HallsBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.HallsLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.LastLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.PrisonBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.PrisonLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Region0Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegionDefinition;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers docs/depth0-implementation.md's Segment 3: {@code RegionDefinition[]} replaced the
 * hand-synced switches in {@code Dungeon.newLevel()}, {@code Dungeon.bossLevel(int)}, and
 * {@code RegularLevel}'s region-document lookup. The stated requirement (CLAUDE.md
 * Arbeitsregeln, and this session's explicit instructions) is that depths 1-26 behave
 * byte-identically to before - this class independently reproduces the OLD switch's depth-to-
 * class and depth-to-boss mapping by hand (not by reading the new code) and checks
 * {@code Dungeon.newLevel()}/{@code bossLevel()} against it for every depth 1-26, so a
 * transcription slip in the table would fail here even though it wouldn't be caught by the
 * existing golden-master tests (none of which exercise {@code newLevel()} or {@code bossLevel()}
 * directly).
 */
class RegionDefinitionTest extends GameTestBase {

	@Override
	protected boolean generateLevelInSetup() {
		return false;
	}

	//independent restatement of the pre-refactor switch in Dungeon.newLevel(), branch==0
	private static Class<? extends Level> expectedClass(int depth) {
		switch (depth) {
			case 1: case 2: case 3: case 4: return SewerLevel.class;
			case 5: return SewerBossLevel.class;
			case 6: case 7: case 8: case 9: return PrisonLevel.class;
			case 10: return PrisonBossLevel.class;
			case 11: case 12: case 13: case 14: return CavesLevel.class;
			case 15: return CavesBossLevel.class;
			case 16: case 17: case 18: case 19: return CityLevel.class;
			case 20: return CityBossLevel.class;
			case 21: case 22: case 23: case 24: return HallsLevel.class;
			case 25: return HallsBossLevel.class;
			case 26: return LastLevel.class;
			default: return DeadEndLevel.class;
		}
	}

	@Test
	void newLevelMatchesOriginalSwitchForEveryMainPathDepth() {
		Dungeon.branch = 0;
		for (int depth = 1; depth <= 26; depth++) {
			Dungeon.depth = depth;
			Level level = Dungeon.newLevel();
			assertEquals(expectedClass(depth), level.getClass(),
					"depth " + depth + " must produce the same level class as before the RegionDefinition[] refactor");
			Actor.clear();
		}
	}

	@Test
	void bossLevelMatchesOriginalLiteralListForEveryDepth() {
		for (int depth = -1; depth <= 27; depth++) {
			boolean expected = depth == 5 || depth == 10 || depth == 15 || depth == 20 || depth == 25;
			assertEquals(expected, Dungeon.bossLevel(depth), "depth " + depth);
		}
	}

	@Test
	void depthZeroProducesRegion0LevelWithAWorkingExit() {
		Dungeon.branch = 0;
		Dungeon.depth = 0;
		Level level = Dungeon.newLevel();
		assertTrue(level instanceof Region0Level);

		LevelTransition exit = level.getTransition(LevelTransition.Type.REGULAR_EXIT);
		assertNotNull(exit, "Region 0 must have a way down to depth 1");
		assertEquals(1, exit.destDepth);
		Actor.clear();
	}

	@Test
	void depth26AndOutOfRangeDepthsAreNotPartOfAnyRegion() {
		assertNull(RegionDefinition.regionOf(26), "LastLevel isn't a region - see RegionDefinition's class comment");
		assertNull(RegionDefinition.regionOf(-1));
		assertNull(RegionDefinition.regionOf(27));
	}

	@Test
	void region0HasNoBossAndNoLoreDocument() {
		RegionDefinition region0 = RegionDefinition.regionOf(0);
		assertNotNull(region0);
		assertEquals(Region0Level.class, region0.levelClass);
		assertNull(region0.bossLevelClass);
		assertNull(region0.loreDocument);
		assertFalse(Dungeon.bossLevel(0));
	}
}

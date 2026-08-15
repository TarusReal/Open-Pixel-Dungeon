package com.shatteredpixel.shatteredpixeldungeon.test;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Region0Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Segment 4 smoke test (docs/depth0-implementation.md): depth 0 generated, entered, and left
 * repeatedly, end to end, with no crash - the concrete proof-of-mechanism the whole depth0-
 * findings.md effort was building towards. Deliberately drives the same primitives
 * {@code InterlevelScene}/{@code Dungeon.switchLevel()} use (newLevel(), a level's transitions,
 * Actor registration, hero placement) rather than calling switchLevel() itself, for the same
 * reason GameTestBase stops short of it - see GameTestBase's class comment.
 *
 * <p>Looped several times in one test (not just left to Gradle's own re-run) to catch state that
 * leaks or accumulates across repeated depth 0 visits within a single session - e.g. an
 * unbalanced {@code Random} generator push/pop, or {@code Bones}/{@code LimitedDrops} state that
 * only misbehaves on a second pass.
 */
class Depth0SmokeTest extends GameTestBase {

	@Override
	protected boolean generateLevelInSetup() {
		return false;
	}

	@Test
	void depthZeroLoadsIsEnteredAndIsLeftRepeatedlyWithoutCrashing() {
		for (int run = 0; run < 5; run++) {

			//load: generate depth 0 like InterlevelScene.descend()/ascend() would
			Dungeon.branch = 0;
			Dungeon.depth = 0;
			Level surface = Dungeon.newLevel();
			assertInstanceOf(Region0Level.class, surface, "run " + run);

			//enter: register it as the active level, place the hero, register Actors -
			//mirrors what Dungeon.switchLevel() does minus the disk save + GameScene UI
			Dungeon.level = surface;
			Dungeon.hero.pos = surface.entrance();
			assertTrue(surface.passable[Dungeon.hero.pos], "hero must land on a walkable cell, run " + run);
			Actor.init();
			assertTrue(Actor.chars().contains(Dungeon.hero));

			//leave: walk the level's exit down to depth 1, exactly like a real descent
			LevelTransition exit = surface.getTransition(LevelTransition.Type.REGULAR_EXIT);
			assertNotNull(exit, "run " + run);
			assertEquals(1, exit.destDepth);

			Dungeon.depth = exit.destDepth;
			Dungeon.branch = exit.destBranch;
			Actor.clear();
			Level next = Dungeon.newLevel();
			assertInstanceOf(SewerLevel.class, next, "descending from Region 0 must reach the normal depth-1 Sewers, run " + run);

			Dungeon.level = next;
			Dungeon.hero.pos = next.entrance();
			Actor.init();

			Actor.clear();
		}
	}
}

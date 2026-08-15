package com.shatteredpixel.shatteredpixeldungeon.test;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.connection.ConnectionRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.StandardRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.entrance.EntranceRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.exit.ExitRoom;
import com.watabou.utils.Random;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Covers docs/depth0-findings.md's four known depth=0 crash sites (Segment 2a): each of these
 * room factories indexed a {@code chances[27][]} array directly by {@code Dungeon.depth}, and
 * that array only has entries for depths 1-26 - {@code chances[0]} was {@code null}, so
 * {@code Random.chances(null)} threw an NPE. Each now gates depth into [1, chances.length-1]
 * before indexing (see StandardRoom.createRoom() for the pattern all four share).
 *
 * <p>These factories are exercised directly at depth 0 regardless of whether Region 0's own
 * (non-StandardRoom) level class ever calls them in the regular game flow - CLAUDE.md's stance
 * on this class of bug (Fallstrick #5) is that any code path reaching them with depth 0 must not
 * crash, not just the ones currently wired up.
 */
class Depth0RoomFactoryGuardTest extends GameTestBase {

	@Override
	protected boolean generateLevelInSetup() {
		return false;
	}

	@Test
	void roomFactoriesDoNotCrashAtDepthZero() {
		Dungeon.depth = 0;
		Random.pushGenerator(1);
		try {
			assertNotNull(StandardRoom.createRoom());
			assertNotNull(EntranceRoom.createEntrance());
			assertNotNull(ExitRoom.createExit());
			assertNotNull(ConnectionRoom.createRoom());
		} finally {
			Random.popGenerator();
		}
	}

	@Test
	void limitedDropHelpersReturnFalseAtDepthZeroInsteadOfAliasingFloorSetEnd() {
		//depth 0 shares depth%5==0/depth/5==0 with depth 5 (end of floor set 1) under plain
		//integer arithmetic; Region 0 isn't part of any floor set, so these must not fire there
		Dungeon.depth = 0;
		assertFalse(Dungeon.posNeeded());
		assertFalse(Dungeon.souNeeded());
		assertFalse(Dungeon.asNeeded());
		assertFalse(Dungeon.enchStoneNeeded());
		assertFalse(Dungeon.labRoomNeeded());
		//depth 0 shares depth<5 && Random.Int(4-depth) with floor 1 - same aliasing class
		assertFalse(Dungeon.intStoneNeeded());
		assertFalse(Dungeon.trinketCataNeeded());
	}
}

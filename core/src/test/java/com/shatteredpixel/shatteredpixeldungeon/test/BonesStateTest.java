package com.shatteredpixel.shatteredpixeldungeon.test;

import com.shatteredpixel.shatteredpixeldungeon.Bones;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Covers the {@link Bones} rewrite from docs/depth0-implementation.md's Segment 1 addendum:
 * "already read from disk this session", "already collected at this location", and "real depth
 * (possibly 0)" used to be conflated into a single {@code depth} field, which meant a real depth
 * 0 bones location could be silently mistaken for the in-memory "collected" marker (which used to
 * be the literal value 0) once Region 0 becomes reachable. They are now three independent pieces
 * of state ({@code depth}/{@code branch}, {@code loaded}, {@code depleted}) - this class exercises
 * each of the three states {@code get()} can be in, individually, at depth 0 specifically (the
 * depth value that used to be ambiguous).
 *
 * <p>{@code Bones}'s fields are private static and (by production design - see the field
 * comments in Bones.java) deliberately NOT reset between runs within one process lifetime, so
 * each test resets them via reflection instead of a production-code hook, and clears
 * {@code bones.dat} from the shared per-JVM test save directory (see {@code GdxTestRuntime}) so
 * tests don't leak state into each other through the filesystem either.
 */
class BonesStateTest extends GameTestBase {

	private static final String BONES_FILE = "bones.dat";
	private static final String LEVEL = "level";
	private static final String BRANCH = "branch";
	private static final String DEPLETED = "depleted";

	@Override
	protected boolean generateLevelInSetup() {
		return false;
	}

	@BeforeEach
	void resetBonesStaticState() throws Exception {
		FileUtils.deleteFile(BONES_FILE);
		setBonesField("depth", -1);
		setBonesField("branch", -1);
		setBonesField("loaded", false);
		setBonesField("depleted", false);
		setBonesField("item", null);
		setBonesField("heroClass", null);
	}

	private static void setBonesField(String name, Object value) throws Exception {
		Field f = Bones.class.getDeclaredField(name);
		f.setAccessible(true);
		f.set(null, value);
	}

	@Test
	void neverReadFromDiskReturnsNullWithoutCrashing() {
		//no bones.dat on disk, static state freshly reset -> the "loaded == false, nothing to
		// read" path. Called twice to confirm it also doesn't wedge into a bad state on retry.
		assertNull(Bones.get());
		assertNull(Bones.get());
	}

	@Test
	void depletedMarkerAtDepthZeroYieldsNothingEvenThoughDepthZeroIsReal() {
		//simulates a bones.dat already marked depleted at depth 0/branch 0 - the exact collision
		//that using the literal value 0 as both "collected" and "real depth" used to risk
		Bundle depletedMarker = new Bundle();
		depletedMarker.put(LEVEL, 0);
		depletedMarker.put(BRANCH, 0);
		depletedMarker.put(DEPLETED, true);
		try {
			FileUtils.bundleToFile(BONES_FILE, depletedMarker);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		Dungeon.depth = 0;
		Dungeon.branch = 0;

		assertNull(Bones.get(), "a depleted marker at depth 0 must not be handed out as if it were unclaimed remains");
	}

	@Test
	void realBonesAtDepthZeroAreLoadedOnceThenStayDepletedAcrossSessions() throws Exception {
		Dungeon.depth = 0;
		Dungeon.branch = 0;

		//hero death on the surface leaves real remains at depth 0
		Bones.leave();

		//simulate a fresh process picking the game back up: force get() to actually re-read
		//bones.dat from disk rather than reusing leave()'s in-memory state
		setBonesField("loaded", false);

		ArrayList<?> firstPickup = Bones.get();
		assertNotNull(firstPickup, "real depth-0 remains must be loaded and handed out - this is the Kategorie E #1 fix");
		assertFalse(firstPickup.isEmpty());

		//same session, same location: must not hand out the same remains twice
		assertNull(Bones.get());

		//simulate another fresh process reading bones.dat again: the depleted marker written
		//after the first pickup must persist to disk, not just in memory
		setBonesField("loaded", false);
		assertNull(Bones.get(), "the depleted marker must survive a fresh disk read, not just the in-memory session");
	}
}

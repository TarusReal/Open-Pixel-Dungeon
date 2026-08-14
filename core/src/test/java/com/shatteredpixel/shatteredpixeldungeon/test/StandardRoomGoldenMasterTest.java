package com.shatteredpixel.shatteredpixeldungeon.test;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.StandardRoom;
import com.watabou.utils.Random;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Golden-master test for {@link StandardRoom}'s room-selection distribution (audit finding #1,
 * the StandardRoom half of the {@code ItemRegistry} refactor - see docs/testing.md).
 *
 * {@link StandardRoom} stores room classes and per-depth drop weights as two parallel structures
 * held together only by shared array index ({@code rooms} next to {@code chances[depth]}, see
 * {@code StandardRoom.java}) - the same class of bug that {@code Generator.Category.ItemEntry}
 * closed for items (see {@code GeneratorGoldenMasterTest}), not yet applied here.
 *
 * This test locks down today's *observable* behavior instead: drawing a large, fixed number of
 * rooms from {@link StandardRoom#createRoom()} at a fixed seed, for one representative depth per
 * distinct weight "bracket" (depths 1, 2-4, 5, 6-10, 11-15, 16-20, 21-26 all have their own -
 * possibly array-aliased - weight set, see {@code StandardRoom.chances}), always produces the
 * same distribution of room classes. If a refactor changes that distribution, this test fails and
 * its diff shows exactly which room's frequency moved at which depth.
 *
 * <p>Like {@code Generator.random()} (see docs/testing.md), {@code StandardRoom.createRoom()}
 * reads the ambient RNG generator without pushing its own seed, so reproducibility requires this
 * test to explicitly bracket its draws in {@code Random.pushGenerator(...)}/{@code popGenerator()}
 * itself.
 *
 * If you intentionally change room weights: that's a deliberate design change, not something to
 * wave through here. Regenerate EXPECTED_DISTRIBUTION deliberately (see its comment) as its own
 * reviewable step.
 */
class StandardRoomGoldenMasterTest extends GameTestBase {

	private static final int DRAWS = 2000;
	//one representative depth per distinct weight bracket in StandardRoom.chances
	private static final int[] FIXED_DEPTHS = {1, 3, 5, 8, 13, 18, 24};

	@Override
	protected boolean generateLevelInSetup() {
		//this test only needs StandardRoom's statics and a fixed Dungeon.depth, not an actual
		//generated Level - skipping level generation keeps the test fast and avoids spending RNG
		//draws before the room draws below
		return false;
	}

	@Test
	void roomClassDistributionMatchesGoldenMaster() {
		Map<String, Integer> counts = new TreeMap<>();

		Random.pushGenerator(testSeed());
		for (int depth : FIXED_DEPTHS) {
			Dungeon.depth = depth;
			for (int i = 0; i < DRAWS; i++) {
				StandardRoom room = StandardRoom.createRoom();
				counts.merge("depth" + depth + ":" + room.getClass().getSimpleName(), 1, Integer::sum);
			}
		}
		Random.popGenerator();

		assertEquals(String.join("\n", EXPECTED_DISTRIBUTION), render(counts),
				"StandardRoom's room-selection distribution changed for a fixed seed. If this is "
				+ "an intentional design change, regenerate EXPECTED_DISTRIBUTION - see this "
				+ "test's class comment. If not, a refactor shifted class<->weight pairing.");
	}

	private static String render(Map<String, Integer> counts) {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, Integer> e : counts.entrySet()) {
			if (sb.length() > 0) sb.append('\n');
			sb.append(e.getKey()).append('=').append(e.getValue());
		}
		return sb.toString();
	}

	//Golden master, captured from a real run of this test at DRAWS=2000, FIXED_DEPTHS above, and
	//GameTestBase's fixed seed (42L) and default hero class (WARRIOR). To regenerate after a
	//deliberate design change: temporarily set this to {}, run the test, and paste the actual
	//value from the assertion failure message back in here as one "depthN:Class=count" entry per
	//line.
	private static final String[] EXPECTED_DISTRIBUTION = {
			"depth13:AquariumRoom=48",
			"depth13:BurnedRoom=32",
			"depth13:CaveRoom=666",
			"depth13:CavesFissureRoom=313",
			"depth13:CirclePitRoom=154",
			"depth13:CircleWallRoom=150",
			"depth13:FissureRoom=51",
			"depth13:GrassyGraveRoom=27",
			"depth13:MinefieldRoom=38",
			"depth13:PlantsRoom=38",
			"depth13:PlatformRoom=36",
			"depth13:RegionDecoBridgeRoom=322",
			"depth13:StripedRoom=48",
			"depth13:StudyRoom=42",
			"depth13:SuspiciousChestRoom=35",
			"depth18:AquariumRoom=38",
			"depth18:BurnedRoom=48",
			"depth18:FissureRoom=37",
			"depth18:GrassyGraveRoom=44",
			"depth18:HallwayRoom=373",
			"depth18:LibraryHallRoom=426",
			"depth18:LibraryRingRoom=420",
			"depth18:MinefieldRoom=49",
			"depth18:PlantsRoom=38",
			"depth18:PlatformRoom=39",
			"depth18:SegmentedLibraryRoom=172",
			"depth18:StatuesRoom=191",
			"depth18:StripedRoom=36",
			"depth18:StudyRoom=45",
			"depth18:SuspiciousChestRoom=44",
			"depth1:CircleBasinRoom=178",
			"depth1:FissureRoom=37",
			"depth1:PlantsRoom=48",
			"depth1:PlatformRoom=32",
			"depth1:RegionDecoPatchRoom=202",
			"depth1:RingRoom=366",
			"depth1:SewerPipeRoom=674",
			"depth1:StripedRoom=46",
			"depth1:StudyRoom=43",
			"depth1:WaterBridgeRoom=374",
			"depth24:AquariumRoom=32",
			"depth24:BurnedRoom=42",
			"depth24:ChasmRoom=379",
			"depth24:FissureRoom=41",
			"depth24:GrassyGraveRoom=37",
			"depth24:MinefieldRoom=43",
			"depth24:PlantsRoom=35",
			"depth24:PlatformRoom=40",
			"depth24:RegionDecoPatchRoom=416",
			"depth24:RitualRoom=212",
			"depth24:RuinsRoom=389",
			"depth24:SkullsRoom=210",
			"depth24:StripedRoom=32",
			"depth24:StudyRoom=46",
			"depth24:SuspiciousChestRoom=46",
			"depth3:AquariumRoom=44",
			"depth3:BurnedRoom=40",
			"depth3:CircleBasinRoom=178",
			"depth3:FissureRoom=44",
			"depth3:GrassyGraveRoom=29",
			"depth3:MinefieldRoom=33",
			"depth3:PlantsRoom=33",
			"depth3:PlatformRoom=37",
			"depth3:RegionDecoPatchRoom=166",
			"depth3:RingRoom=322",
			"depth3:SewerPipeRoom=613",
			"depth3:StripedRoom=52",
			"depth3:StudyRoom=37",
			"depth3:SuspiciousChestRoom=35",
			"depth3:WaterBridgeRoom=337",
			"depth5:RegionDecoPatchRoom=232",
			"depth5:RingRoom=447",
			"depth5:SewerPipeRoom=884",
			"depth5:WaterBridgeRoom=437",
			"depth8:AquariumRoom=40",
			"depth8:BurnedRoom=40",
			"depth8:CellBlockRoom=227",
			"depth8:ChasmBridgeRoom=189",
			"depth8:FissureRoom=40",
			"depth8:GrassyGraveRoom=36",
			"depth8:MinefieldRoom=25",
			"depth8:PillarsRoom=380",
			"depth8:PlantsRoom=34",
			"depth8:PlatformRoom=41",
			"depth8:RegionDecoLineRoom=411",
			"depth8:SegmentedRoom=397",
			"depth8:StripedRoom=44",
			"depth8:StudyRoom=46",
			"depth8:SuspiciousChestRoom=50",
	};
}

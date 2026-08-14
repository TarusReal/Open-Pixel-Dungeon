package com.shatteredpixel.shatteredpixeldungeon.test;

import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.MobSpawner;
import com.watabou.utils.Random;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Golden-master test for {@link MobSpawner}'s regular-mob rotation distribution (audit finding #1,
 * the MobSpawner/Bestiary half of the {@code ItemRegistry}/{@code RoomEntry} refactor - see
 * docs/testing.md).
 *
 * {@code MobSpawner.standardMobRotation} (private, hence {@code @code} not {@code @link} here)
 * stores mob classes and per-depth repeat-counts as a
 * {@code switch(depth)} of hand-written {@code Arrays.asList(...)} calls, and
 * {@link MobSpawner#RARE_ALTS} is a separately hand-maintained class-to-class swap map - the same
 * class of bug ({@code Generator}'s WEP_T3 pairing drift, closed by
 * {@code Generator.Category.ItemEntry}) that this test locks down for mobs before any refactor
 * touches that structure.
 *
 * This test locks down today's *observable* behavior: drawing a large, fixed number of rotations
 * from {@link MobSpawner#getMobRotation(int)} at a fixed seed, for one representative depth per
 * distinct rotation "bracket" (20 brackets: {1}, {2}, {3}, {4,5}, {6}, {7}, {8}, {9,10}, {11},
 * {12}, {13}, {14,15}, {16}, {17}, {18}, {19,20}, {21}, {22}, {23}, {24,25,26} - see the case
 * groupings in {@code MobSpawner.standardMobRotation}), always produces the same distribution of
 * spawned classes. Depths 4, 9, 14, and 19 are each the low end of a bracket AND one of
 * {@link MobSpawner#addRareMobs}'s four special-cased depths, so this test exercises the rotation
 * weights, the rare-bonus-add chance, and the {@code swapMobAlts} rare-alt-swap chance together, in
 * their real composition. If a refactor changes that distribution, this test fails and its diff
 * shows exactly which class's frequency moved at which depth.
 *
 * <p>Like {@code Generator.random()} and {@code StandardRoom.createRoom()} (see docs/testing.md),
 * {@code MobSpawner.getMobRotation(int)} reads the ambient RNG generator without pushing its own
 * seed, so reproducibility requires this test to explicitly bracket its draws in
 * {@code Random.pushGenerator(...)}/{@code popGenerator()} itself.
 *
 * If you intentionally change mob rotation weights: that's a deliberate design change, not
 * something to wave through here. Regenerate EXPECTED_DISTRIBUTION deliberately (see its comment)
 * as its own reviewable step.
 */
class MobSpawnerGoldenMasterTest extends GameTestBase {

	private static final int DRAWS = 3000;
	//one representative depth per distinct rotation bracket - see class comment
	private static final int[] FIXED_DEPTHS = {
			1, 2, 3, 4, 6, 7, 8, 9, 11, 12, 13, 14, 16, 17, 18, 19, 21, 22, 23, 24
	};

	@Override
	protected boolean generateLevelInSetup() {
		//this test only needs MobSpawner's statics and a fixed depth argument, not an actual
		//generated Level - skipping level generation keeps the test fast and avoids spending RNG
		//draws before the rotation draws below
		return false;
	}

	@Test
	void mobRotationDistributionMatchesGoldenMaster() {
		Map<String, Integer> counts = new TreeMap<>();

		Random.pushGenerator(testSeed());
		for (int depth : FIXED_DEPTHS) {
			for (int i = 0; i < DRAWS; i++) {
				for (Class<? extends Mob> cls : MobSpawner.getMobRotation(depth)) {
					counts.merge("depth" + depth + ":" + cls.getSimpleName(), 1, Integer::sum);
				}
			}
		}
		Random.popGenerator();

		assertEquals(String.join("\n", EXPECTED_DISTRIBUTION), render(counts),
				"MobSpawner's mob rotation distribution changed for a fixed seed. If this is an "
				+ "intentional design change, regenerate EXPECTED_DISTRIBUTION - see this test's "
				+ "class comment. If not, a refactor shifted class<->weight pairing.");
	}

	private static String render(Map<String, Integer> counts) {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, Integer> e : counts.entrySet()) {
			if (sb.length() > 0) sb.append('\n');
			sb.append(e.getKey()).append('=').append(e.getValue());
		}
		return sb.toString();
	}

	//Golden master, captured from a real run of this test at DRAWS=3000, FIXED_DEPTHS above, and
	//GameTestBase's fixed seed (42L) and default hero class (WARRIOR). To regenerate after a
	//deliberate design change: temporarily set this to {}, run the test, and paste the actual
	//value from the assertion failure message back in here as one "depthN:Class=count" entry per
	//line.
	private static final String[] EXPECTED_DISTRIBUTION = {
			"depth11:ArmoredBrute=65",
			"depth11:Bat=9000",
			"depth11:BlueShaman=1195",
			"depth11:Brute=2935",
			"depth11:PurpleShaman=576",
			"depth11:RedShaman=1229",
			"depth12:ArmoredBrute=109",
			"depth12:Bat=6000",
			"depth12:BlueShaman=1167",
			"depth12:Brute=5891",
			"depth12:PurpleShaman=618",
			"depth12:RedShaman=1215",
			"depth12:Spinner=3000",
			"depth13:ArmoredBrute=131",
			"depth13:Bat=3000",
			"depth13:BlueShaman=2377",
			"depth13:Brute=5869",
			"depth13:DM200=2939",
			"depth13:DM201=61",
			"depth13:PurpleShaman=1229",
			"depth13:RedShaman=2394",
			"depth13:Spinner=6000",
			"depth14:ArmoredBrute=70",
			"depth14:Bat=3000",
			"depth14:BlueShaman=2404",
			"depth14:Brute=2930",
			"depth14:DM200=5886",
			"depth14:DM201=114",
			"depth14:Ghoul=84",
			"depth14:PurpleShaman=1141",
			"depth14:RedShaman=2455",
			"depth14:Spinner=6000",
			"depth16:ChaosElemental=55",
			"depth16:FireElemental=1152",
			"depth16:FrostElemental=1168",
			"depth16:Ghoul=9000",
			"depth16:ShockElemental=625",
			"depth16:Warlock=3000",
			"depth17:ChaosElemental=112",
			"depth17:FireElemental=2391",
			"depth17:FrostElemental=2305",
			"depth17:Ghoul=3000",
			"depth17:Monk=2943",
			"depth17:Senior=57",
			"depth17:ShockElemental=1192",
			"depth17:Warlock=3000",
			"depth18:ChaosElemental=54",
			"depth18:FireElemental=1132",
			"depth18:FrostElemental=1208",
			"depth18:Ghoul=3000",
			"depth18:Golem=3000",
			"depth18:Monk=5885",
			"depth18:Senior=115",
			"depth18:ShockElemental=606",
			"depth18:Warlock=6000",
			"depth19:ChaosElemental=69",
			"depth19:FireElemental=1191",
			"depth19:FrostElemental=1156",
			"depth19:Golem=9000",
			"depth19:Monk=5870",
			"depth19:Senior=130",
			"depth19:ShockElemental=584",
			"depth19:Succubus=76",
			"depth19:Warlock=6000",
			"depth1:Albino=182",
			"depth1:Rat=8818",
			"depth1:Snake=3000",
			"depth21:Eye=3000",
			"depth21:Succubus=6000",
			"depth22:Eye=3000",
			"depth22:Succubus=3000",
			"depth23:Acidic=64",
			"depth23:Eye=6000",
			"depth23:Scorpio=2936",
			"depth23:Succubus=3000",
			"depth24:Acidic=190",
			"depth24:Eye=6000",
			"depth24:Scorpio=8810",
			"depth24:Succubus=3000",
			"depth2:Albino=132",
			"depth2:Gnoll=5869",
			"depth2:GnollExile=131",
			"depth2:Rat=5868",
			"depth2:Snake=3000",
			"depth3:Albino=54",
			"depth3:Crab=2935",
			"depth3:Gnoll=8816",
			"depth3:GnollExile=184",
			"depth3:HermitCrab=65",
			"depth3:Rat=2946",
			"depth3:Snake=3000",
			"depth3:Swarm=3000",
			"depth4:Bandit=2",
			"depth4:CausticSlime=130",
			"depth4:Crab=5885",
			"depth4:Gnoll=2938",
			"depth4:GnollExile=62",
			"depth4:HermitCrab=115",
			"depth4:Slime=5870",
			"depth4:Swarm=3000",
			"depth4:Thief=88",
			"depth6:Bandit=58",
			"depth6:Skeleton=9000",
			"depth6:Swarm=3000",
			"depth6:Thief=2942",
			"depth7:Bandit=61",
			"depth7:DM100=3000",
			"depth7:Guard=3000",
			"depth7:Skeleton=9000",
			"depth7:Thief=2939",
			"depth8:Bandit=64",
			"depth8:DM100=6000",
			"depth8:Guard=6000",
			"depth8:Necromancer=2948",
			"depth8:Skeleton=6000",
			"depth8:SpectralNecromancer=52",
			"depth8:Thief=2936",
			"depth9:Bandit=75",
			"depth9:Bat=74",
			"depth9:DM100=6000",
			"depth9:Guard=6000",
			"depth9:Necromancer=5874",
			"depth9:Skeleton=3000",
			"depth9:SpectralNecromancer=126",
			"depth9:Thief=2925",
	};
}

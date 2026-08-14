package com.shatteredpixel.shatteredpixeldungeon.test;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.watabou.utils.Random;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Golden-master test for {@link Generator}'s loot distribution (audit findings #2 and #7, and
 * the proof-of-concept requested for #12).
 *
 * {@link Generator.Category} stores item classes and drop weights as two parallel arrays held
 * together only by their shared index (see e.g. {@code Category.POTION.classes} next to
 * {@code Category.POTION.defaultProbs} in Generator.java). A refactor of that storage - or a
 * slip while adding a new item - can silently shift which weight belongs to which item without
 * the compiler ever noticing.
 *
 * This test locks down today's *observable* behavior instead: drawing a large, fixed number of
 * items from {@link Generator#random()} with a fixed seed always produces the same distribution
 * of item classes. If a refactor changes that distribution, this test fails and its diff shows
 * exactly which item's frequency moved - it does not care how Generator is implemented
 * internally, only what it produces.
 *
 * <p><b>Why this test explicitly pushes its own RNG generator:</b> after {@code Dungeon.init()}
 * returns, the "current" RNG (what {@code Random.Int/Float/chances(...)} draw from without an
 * explicit push) is a freshly constructed, unseeded {@code java.util.Random} - see
 * {@code Dungeon.init()}, which calls {@code Random.resetGenerators()} right after
 * {@code Generator.fullReset()}. That's deliberate upstream (hero-setup specifics like starting
 * curses shouldn't be guessable from a seed), but it means {@code Generator.random()} is only
 * reproducible from {@code Dungeon.seed} while running inside a generator that was explicitly
 * (re-)seeded - exactly what real level generation does: {@code Level.create()} wraps its whole
 * body, including the {@code createItems()} call that invokes {@code Generator.random()}, in
 * {@code Random.pushGenerator(Dungeon.seedCurDepth())}. This test reproduces that same bracket
 * via {@code Dungeon.seedForDepth(...)} so it draws loot exactly the way a real level would.
 * Skipping this bracket produces a different, non-reproducible distribution every run - found
 * empirically while building this test, not by reading the code; see docs/testing.md.
 *
 * If you intentionally change loot odds: that's a deliberate balance change, not something to
 * wave through here. Regenerate EXPECTED_DISTRIBUTION deliberately (see its comment) as its own
 * reviewable step.
 *
 * <p><b>Note on the WEP_T3 defaultProbs fix (CLAUDE.md "Balance-Fix" session):</b> ItemRegistry
 * used to clobber {@code WEP_T3.probs} with {@code WEP_T1.defaultProbs} right after setting it
 * correctly via {@code setEntries()}; that stray assignment has been deleted. It did NOT change
 * EXPECTED_DISTRIBUTION below (confirmed by running this test before/after): {@code Dungeon.init()}
 * calls {@code Generator.fullReset()} in {@code GameTestBase}'s {@code @BeforeEach}, which
 * overwrites {@code probs} with {@code defaultProbs.clone()} for every category before this test
 * ever draws an item - and {@code defaultProbs} was never wrong, only the post-setEntries()
 * override of {@code probs} was. The bug was real but had no observable effect once a
 * reset/fullReset had run, which is exactly the sequence real level generation goes through.
 */
class GeneratorGoldenMasterTest extends GameTestBase {

	private static final int DRAWS = 5000;
	private static final int FIXED_DEPTH = 15;

	@Override
	protected boolean generateLevelInSetup() {
		//this test only needs Generator's statics (initialized by Dungeon.init()) and a fixed
		//Dungeon.depth, not an actual generated Level - skipping level generation keeps the
		//test fast and avoids spending RNG draws before the loot draws below
		return false;
	}

	@Test
	void lootClassDistributionMatchesGoldenMaster() {
		Dungeon.depth = FIXED_DEPTH;

		Map<String, Integer> counts = new TreeMap<>();
		//see the class comment: reproducibility requires drawing inside a seeded generator,
		//the same way Level.create() brackets real level generation
		Random.pushGenerator(Dungeon.seedForDepth(FIXED_DEPTH, 0));
		for (int i = 0; i < DRAWS; i++) {
			Item item = Generator.random();
			counts.merge(item.getClass().getSimpleName(), 1, Integer::sum);
		}
		Random.popGenerator();

		assertEquals(String.join("\n", EXPECTED_DISTRIBUTION), render(counts),
				"Generator's loot distribution changed for a fixed seed. If this is an "
				+ "intentional balance change, regenerate EXPECTED_DISTRIBUTION - see this "
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

	//Golden master, captured from a real run of this test at DRAWS=5000, FIXED_DEPTH=15, and
	//GameTestBase's fixed seed (42L) and default hero class (WARRIOR). To regenerate after a
	//deliberate balance change: temporarily set this to {}, run the test, and paste the actual
	//value from the assertion failure message back in here as one "Class=count" entry per line.
	private static final String[] EXPECTED_DISTRIBUTION = {
			"AlchemistsToolkit=1",
			"AssassinsBlade=14",
			"BattleAxe=14",
			"Bolas=15",
			"ChaliceOfBlood=1",
			"Crossbow=15",
			"DriedRose=1",
			"EtherealChains=1",
			"Flail=15",
			"ForceCube=25",
			"Gauntlet=19",
			"Glaive=20",
			"Gold=1430",
			"Greataxe=18",
			"Greatshield=20",
			"Greatsword=18",
			"HeavyBoomerang=31",
			"HornOfPlenty=1",
			"Javelin=31",
			"Katana=15",
			"Kunai=15",
			"Longsword=16",
			"Mace=9",
			"MailArmor=46",
			"MasterThievesArmband=1",
			"PlateArmor=68",
			"PotionOfExperience=38",
			"PotionOfFrost=114",
			"PotionOfHaste=76",
			"PotionOfHealing=230",
			"PotionOfInvisibility=76",
			"PotionOfLevitation=76",
			"PotionOfLiquidFlame=114",
			"PotionOfMindVision=152",
			"PotionOfParalyticGas=76",
			"PotionOfPurity=76",
			"PotionOfToxicGas=114",
			"RingOfAccuracy=11",
			"RingOfArcana=10",
			"RingOfElements=12",
			"RingOfEnergy=12",
			"RingOfEvasion=10",
			"RingOfForce=12",
			"RingOfFuror=10",
			"RingOfHaste=10",
			"RingOfMight=11",
			"RingOfSharpshooting=12",
			"RingOfTenacity=11",
			"RingOfWealth=11",
			"RoundShield=9",
			"RunicBlade=14",
			"Sai=9",
			"SandalsOfNature=1",
			"ScaleArmor=100",
			"Scimitar=8",
			"ScrollOfIdentify=228",
			"ScrollOfLullaby=76",
			"ScrollOfMagicMapping=76",
			"ScrollOfMirrorImage=114",
			"ScrollOfRage=76",
			"ScrollOfRecharging=114",
			"ScrollOfRemoveCurse=153",
			"ScrollOfRetribution=76",
			"ScrollOfTeleportation=114",
			"ScrollOfTerror=77",
			"ScrollOfTransmutation=38",
			"Seed=142",
			"SkeletonKey=1",
			"StoneOfAggression=14",
			"StoneOfBlast=14",
			"StoneOfBlink=15",
			"StoneOfClairvoyance=14",
			"StoneOfDeepSleep=14",
			"StoneOfDetectMagic=15",
			"StoneOfFear=14",
			"StoneOfFlock=14",
			"StoneOfIntuition=14",
			"StoneOfShock=15",
			"Sword=8",
			"TalismanOfForesight=1",
			"ThrowingHammer=27",
			"ThrowingSpear=15",
			"TimekeepersHourglass=1",
			"Tomahawk=30",
			"Trident=26",
			"UnstableSpellbook=1",
			"WandOfBlastWave=9",
			"WandOfCorrosion=12",
			"WandOfCorruption=11",
			"WandOfDisintegration=12",
			"WandOfFireblast=11",
			"WandOfFrost=12",
			"WandOfLightning=11",
			"WandOfLivingEarth=10",
			"WandOfMagicMissile=11",
			"WandOfPrismaticLight=9",
			"WandOfRegrowth=11",
			"WandOfTransfusion=12",
			"WandOfWarding=12",
			"WarHammer=19",
			"WarScythe=18",
			"Whip=8",
	};
}

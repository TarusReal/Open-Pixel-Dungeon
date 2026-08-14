/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Single place a new regular (non-boss) mob is wired up (audit finding #1): its
 * {@link MobSpawner} rotation weight per depth bracket, its optional rare/alt swap target, and its
 * {@link com.shatteredpixel.shatteredpixeldungeon.journal.Bestiary} membership - the mob equivalent
 * of {@link com.shatteredpixel.shatteredpixeldungeon.items.ItemRegistry}/
 * {@code Generator.Category.ItemEntry} for items and {@code StandardRoom.RoomEntry} for rooms
 * ({@code @code}, not {@code @link}, for both - {@code RoomEntry} is {@code private} to
 * {@code StandardRoom}, so a cross-package {@code @link} to it wouldn't resolve).
 *
 * <p>Bosses (DM-300, Tengu, Goo, Dwarf King, Yog-Dzewa) are deliberately not part of this registry
 * - they never go through {@link MobSpawner}'s rotation, each spawning via its own hand-scripted
 * {@code *BossLevel} class instead. {@code Bestiary.BOSSES} stays manually maintained.
 *
 * <p><b>Bracket granularity:</b> unlike {@code StandardRoom}'s 7 depth brackets, the switch this
 * registry replaces ({@code MobSpawner.standardMobRotation}, pre-refactor) actually distinguishes
 * 20 brackets - mostly individual depths, with a handful of adjacent-depth case groups
 * ({@code case 4: case 5:}, {@code case 9: case 10:}, etc, see {@link #BRACKET_DEPTHS}). This
 * class follows that granularity rather than forcing StandardRoom's 7-bracket shape onto data that
 * doesn't have it.
 *
 * <p><b>{@code int[] counts}, not {@code float[] weights}:</b> {@code Generator.Category.ItemEntry}
 * and {@code StandardRoom.RoomEntry} store probability weights fed into {@code Random.chances(...)}.
 * The numbers here are not weights - they're literal repeat-counts in a deterministically built
 * list (no {@code Random.chances} call anywhere in the mob rotation). {@code int} makes that
 * distinction visible in the type and rules out an accidental {@code Random.chances} misuse later.
 */
public class MobRegistry {

	private MobRegistry(){}

	//one bracket per distinct case-grouping in MobSpawner.standardMobRotation's switch(depth),
	//verified against that switch directly - see class comment
	static final int[][] BRACKET_DEPTHS = {
			{1}, {2}, {3}, {4,5},                    //Sewers
			{6}, {7}, {8}, {9,10},                   //Prison
			{11}, {12}, {13}, {14,15},                //Caves
			{16}, {17}, {18}, {19,20},                //City
			{21}, {22}, {23}, {24,25,26},             //Halls
	};

	private static final int[] BRACKET_OF_DEPTH = new int[27];
	static {
		for (int b = 0; b < BRACKET_DEPTHS.length; b++) {
			for (int depth : BRACKET_DEPTHS[b]) {
				BRACKET_OF_DEPTH[depth] = b;
			}
		}
		//depth 0 (and anything else outside 1-26) falls back to bracket 0, reproducing
		//standardMobRotation's "case 1: default:"
	}

	static int bracketOfDepth(int depth) {
		if (depth < 0 || depth >= BRACKET_OF_DEPTH.length) return 0;
		return BRACKET_OF_DEPTH[depth];
	}

	static final class MobEntry {
		final Class<? extends Mob> cls;
		//null for ordinary entries; Shaman::random/Elemental::random for polymorphic ones, which
		//resolve to a concrete subtype live at rotation-build time (matching the original inline
		//Shaman.random()/Elemental.random() calls, including Elemental.random()'s own internal
		//chaos-elemental roll)
		final Supplier<Class<? extends Mob>> resolver;
		//null for ordinary entries; the concrete subtypes a polymorphic entry can resolve to,
		//used only for Bestiary.REGIONAL membership
		final Class<? extends Mob>[] variants;
		//null if this mob has no rare/alt swap target
		final Class<? extends Mob> alt;
		//one repeat-count per bracket, see BRACKET_DEPTHS
		final int[] counts;

		private MobEntry( Class<? extends Mob> cls, Supplier<Class<? extends Mob>> resolver,
						   Class<? extends Mob>[] variants, Class<? extends Mob> alt, int[] counts ) {
			if (counts.length != BRACKET_DEPTHS.length) {
				throw new IllegalArgumentException(cls.getSimpleName() + ": expected "
						+ BRACKET_DEPTHS.length + " bracket counts, got " + counts.length);
			}
			this.cls = cls;
			this.resolver = resolver;
			this.variants = variants;
			this.alt = alt;
			this.counts = counts;
		}
	}

	private static MobEntry entry( Class<? extends Mob> cls, int... counts ) {
		return new MobEntry(cls, null, null, null, counts);
	}

	private static MobEntry entry( Class<? extends Mob> cls, Class<? extends Mob> alt, int... counts ) {
		return new MobEntry(cls, null, null, alt, counts);
	}

	@SafeVarargs
	private static MobEntry entryPolymorphic( Class<? extends Mob> cls, Supplier<Class<? extends Mob>> resolver,
											   Class<? extends Mob>[] variants, int... counts ) {
		return new MobEntry(cls, resolver, variants, null, counts);
	}

	//canonical rotation table - class (+ optional rare/alt target) + one repeat-count per bracket.
	//
	//Declaration order matters and must not be reshuffled: MobSpawner.swapMobAlts rolls one
	//Random.Float() per rotation-list element in list order, and standardMobRotation rebuilds that
	//list by iterating this array in order - this order is what reproduces the original switch's
	//exact RNG draw sequence. Kept as a flat array rather than a Map<Class,MobEntry> for the same
	//reason (a Map's iteration order isn't a safe stand-in for that), mirroring
	//StandardRoom.RoomEntry[] - no mob currently needs two independent entries the way
	//RegionDecoPatchRoom does for rooms, so this is precedent-consistency, not a forced requirement.
	//
	//Note the order below is NOT simply region-by-region: the pre-refactor switch's depth-6 case
	//("case 6: Skeleton, Skeleton, Skeleton, Thief, Swarm") places the Sewers holdover Swarm AFTER
	//Prison's Skeleton/Thief, while the Sewers cases place Swarm BEFORE Crab/Slime - Skeleton/Thief
	//are moved ahead of Swarm here (and Crab/Slime after it) to satisfy both orderings at once with
	//a single flat traversal; verified against every bracket via MobSpawnerGoldenMasterTest.
	//
	//Deliberately no "alt" on the Shaman/Elemental entries: Elemental.random() already rolls its
	//own chaos-elemental chance internally, so a RARE_ALTS-style Elemental.class->ChaosElemental
	//mapping (present in the pre-refactor RARE_ALTS map) can never actually fire - Elemental.class
	//itself never appears in a built rotation list, only its resolved concrete subtypes do. Not
	//reproduced here on purpose; see MobSpawnerGoldenMasterTest and docs/testing.md.
	//
	//                                                     b1 b2 b3 b4 b5 b6 b7 b8 b9 b10 b11 b12 b13 b14 b15 b16 b17 b18 b19 b20
	static final MobEntry[] ROTATION = {

			// Sewers/Prison (interleaved - see comment above)
			entry(Rat.class,         Albino.class,               3, 2, 1, 0, 0, 0, 0, 0, 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0),
			entry(Snake.class,                                   1, 1, 1, 0, 0, 0, 0, 0, 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0),
			entry(Gnoll.class,       GnollExile.class,           0, 2, 3, 1, 0, 0, 0, 0, 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0),
			entry(Skeleton.class,                                0, 0, 0, 0, 3, 3, 2, 1, 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0),
			entry(Thief.class,       Bandit.class,               0, 0, 0, 0, 1, 1, 1, 1, 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0),
			entry(Swarm.class,                                   0, 0, 1, 1, 1, 0, 0, 0, 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0),
			entry(DM100.class,                                   0, 0, 0, 0, 0, 1, 2, 2, 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0),
			entry(Guard.class,                                   0, 0, 0, 0, 0, 1, 2, 2, 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0),
			entry(Necromancer.class, SpectralNecromancer.class,  0, 0, 0, 0, 0, 0, 1, 2, 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0),
			entry(Crab.class,        HermitCrab.class,           0, 0, 1, 2, 0, 0, 0, 0, 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0),
			entry(Slime.class,       CausticSlime.class,         0, 0, 0, 2, 0, 0, 0, 0, 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0),

			// Caves
			entry(Bat.class,                                     0, 0, 0, 0, 0, 0, 0, 0, 3,  2,  1,  1,  0,  0,  0,  0,  0,  0,  0,  0),
			entry(Brute.class,       ArmoredBrute.class,         0, 0, 0, 0, 0, 0, 0, 0, 1,  2,  2,  1,  0,  0,  0,  0,  0,  0,  0,  0),
			entryPolymorphic(Shaman.class, Shaman::random,
					new Class[]{Shaman.RedShaman.class, Shaman.BlueShaman.class, Shaman.PurpleShaman.class},
															      0, 0, 0, 0, 0, 0, 0, 0, 1,  1,  2,  2,  0,  0,  0,  0,  0,  0,  0,  0),
			entry(Spinner.class,                                 0, 0, 0, 0, 0, 0, 0, 0, 0,  1,  2,  2,  0,  0,  0,  0,  0,  0,  0,  0),
			entry(DM200.class,       DM201.class,                0, 0, 0, 0, 0, 0, 0, 0, 0,  0,  1,  2,  0,  0,  0,  0,  0,  0,  0,  0),

			// City
			entry(Ghoul.class,                                   0, 0, 0, 0, 0, 0, 0, 0, 0,  0,  0,  0,  3,  1,  1,  0,  0,  0,  0,  0),
			entryPolymorphic(Elemental.class, Elemental::random,
					new Class[]{Elemental.FireElemental.class, Elemental.FrostElemental.class, Elemental.ShockElemental.class},
															      0, 0, 0, 0, 0, 0, 0, 0, 0,  0,  0,  0,  1,  2,  1,  1,  0,  0,  0,  0),
			entry(Warlock.class,                                 0, 0, 0, 0, 0, 0, 0, 0, 0,  0,  0,  0,  1,  1,  2,  2,  0,  0,  0,  0),
			entry(Monk.class,        Senior.class,               0, 0, 0, 0, 0, 0, 0, 0, 0,  0,  0,  0,  0,  1,  2,  2,  0,  0,  0,  0),
			entry(Golem.class,                                   0, 0, 0, 0, 0, 0, 0, 0, 0,  0,  0,  0,  0,  0,  1,  3,  0,  0,  0,  0),

			// Halls
			entry(Succubus.class,                                0, 0, 0, 0, 0, 0, 0, 0, 0,  0,  0,  0,  0,  0,  0,  0,  2,  1,  1,  1),
			entry(Eye.class,                                     0, 0, 0, 0, 0, 0, 0, 0, 0,  0,  0,  0,  0,  0,  0,  0,  1,  1,  2,  2),
			entry(Scorpio.class,     Acidic.class,                0, 0, 0, 0, 0, 0, 0, 0, 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  1,  3),
	};

	static final class RareBonusEntry {
		final int depth;
		final Class<? extends Mob> cls;
		final float chance;

		private RareBonusEntry( int depth, Class<? extends Mob> cls, float chance ) {
			this.depth = depth;
			this.cls = cls;
			this.chance = chance;
		}
	}

	//MobSpawner.addRareMobs' data: an independent bonus chance to add one extra mob on top of the
	//rotation, not a weighted pick within it - kept as its own small table rather than forced into
	//ROTATION's per-bracket counts, which have different semantics (see class comment)
	static final RareBonusEntry[] RARE_BONUS = {
			new RareBonusEntry(4,  Thief.class,    0.025f),
			new RareBonusEntry(9,  Bat.class,      0.025f),
			new RareBonusEntry(14, Ghoul.class,    0.025f),
			new RareBonusEntry(19, Succubus.class, 0.025f),
	};

	//derived fields, populated below from ROTATION - analogous to how Generator.Category derives
	//classes/defaultProbs from entries

	//canonical rare/alt swap data - MobSpawner.RARE_ALTS is populated from this
	static final Map<Class<? extends Mob>, Class<? extends Mob>> ALT_MAP = new LinkedHashMap<>();
	//non-polymorphic rotation classes, for Bestiary.REGIONAL
	public static final Class<?>[] ROTATION_CLASSES;
	//concrete subtypes of polymorphic rotation entries (Shaman/Elemental variants), for Bestiary.REGIONAL
	public static final Class<?>[] ROTATION_VARIANTS;
	//rare/alt swap targets in ROTATION's declaration order (not ALT_MAP.values(), whose HashMap-derived
	//iteration order isn't guaranteed stable), for Bestiary.RARE
	public static final Class<?>[] RARE_ALT_TARGETS;
	//regional mobs placed by other mechanisms entirely (DemonSpawnerRoom), not MobSpawner - the mob
	//equivalent of ItemRegistry's non-Generator Catalog-only constants, for Bestiary.REGIONAL
	public static final Class<?>[] REGIONAL_EXTRA = { RipperDemon.class, DemonSpawner.class };

	static {
		List<Class<?>> classes = new ArrayList<>();
		List<Class<?>> variants = new ArrayList<>();
		List<Class<?>> altTargets = new ArrayList<>();
		for (MobEntry e : ROTATION) {
			if (e.resolver == null) {
				classes.add(e.cls);
			} else {
				variants.addAll(java.util.Arrays.asList(e.variants));
			}
			if (e.alt != null) {
				ALT_MAP.put(e.cls, e.alt);
				altTargets.add(e.alt);
			}
		}
		ROTATION_CLASSES = classes.toArray(new Class<?>[0]);
		ROTATION_VARIANTS = variants.toArray(new Class<?>[0]);
		RARE_ALT_TARGETS = altTargets.toArray(new Class<?>[0]);
	}
}

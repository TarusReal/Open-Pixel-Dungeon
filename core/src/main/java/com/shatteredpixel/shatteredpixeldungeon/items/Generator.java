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

package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.Brew;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.Elixir;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.ExoticPotion;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ExoticScroll;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.Spell;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.Runestone;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ExoticCrystals;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.Trinket;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.TrinketCatalyst;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
import com.watabou.utils.Bundle;
import com.watabou.utils.GameMath;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class Generator {

	public enum Category {
		TRINKET ( 0, 0, Trinket.class),

		WEAPON	( 2, 2, MeleeWeapon.class),
		WEP_T1	( 0, 0, MeleeWeapon.class),
		WEP_T2	( 0, 0, MeleeWeapon.class),
		WEP_T3	( 0, 0, MeleeWeapon.class),
		WEP_T4	( 0, 0, MeleeWeapon.class),
		WEP_T5	( 0, 0, MeleeWeapon.class),

		ARMOR	( 2, 1, Armor.class ),

		MISSILE ( 1, 2, MissileWeapon.class ),
		MIS_T1  ( 0, 0, MissileWeapon.class ),
		MIS_T2  ( 0, 0, MissileWeapon.class ),
		MIS_T3  ( 0, 0, MissileWeapon.class ),
		MIS_T4  ( 0, 0, MissileWeapon.class ),
		MIS_T5  ( 0, 0, MissileWeapon.class ),

		WAND	( 1, 1, Wand.class ),
		RING	( 1, 0, Ring.class ),
		ARTIFACT( 0, 1, Artifact.class),

		FOOD	( 0, 0, Food.class ),

		POTION	( 8, 8, Potion.class ),
		SEED	( 1, 1, Plant.Seed.class ),

		SCROLL	( 8, 8, Scroll.class ),
		STONE   ( 1, 1, Runestone.class),

		GOLD	( 10, 10,   Gold.class );

		public Class<?>[] classes;

		//the canonical, name-carrying source of truth for this category's items: each entry pairs
		//a class directly with its weight(s), so class<->weight can't drift apart the way two
		//hand-maintained parallel arrays (classes[i] / defaultProbs[i]) could. classes/defaultProbs/
		//defaultProbs2 below are derived from this in one place (see setEntries*) and kept only
		//because a few external call sites still read them directly (see Generator.Category.ItemEntry).
		public ItemEntry[] entries;

		//some item types use a deck-based system, where the probs decrement as items are picked
		// until they are all 0, and then they reset. Those generator classes should define
		// defaultProbs. If defaultProbs is null then a deck system isn't used.
		//Artifacts in particular don't reset, no duplicates!
		public float[] probs;
		public float[] defaultProbs = null;

		//some items types have two decks and swap between them
		// this enforces more consistency while still allowing for better precision
		public float[] defaultProbs2 = null;
		public boolean using2ndProbs = false;
		//but in such cases we still need a reference to the full deck in case of non-deck generation
		public float[] defaultProbsTotal = null;

		//These variables are used as a part of the deck system, to ensure that drops are consistent
		// regardless of when they occur (either as part of seeded levelgen, or random item drops)
		public Long seed = null;
		public int dropped = 0;

		//game has two decks of 35 items for overall category probs
		//one deck has a ring and extra armor, the other has an artifact and extra thrown weapon
		//Note that pure random drops only happen as part of levelgen atm, so no seed is needed here
		public float firstProb;
		public float secondProb;
		public Class<? extends Item> superClass;

		private Category( float firstProb, float secondProb, Class<? extends Item> superClass ) {
			this.firstProb = firstProb;
			this.secondProb = secondProb;
			this.superClass = superClass;
		}

		//pairs an item class with its drop weight(s). This is the unit categories are defined with
		//below, instead of writing a class into one array literal and its weight into a separate
		//array literal at the same position - see the audit finding this replaces (WEP_T3 using
		//WEP_T1's defaultProbs by accident, undetected because nothing ties a weight to "its" class).
		public static class ItemEntry {
			public final Class<?> cls;
			public final float prob;
			//only meaningful for categories with a second deck (see defaultProbs2); 0 otherwise
			public final float prob2;

			private ItemEntry( Class<?> cls, float prob ) {
				this( cls, prob, 0 );
			}

			private ItemEntry( Class<?> cls, float prob, float prob2 ) {
				this.cls = cls;
				this.prob = prob;
				this.prob2 = prob2;
			}

			//prob+prob2, i.e. the weight used when both decks of a 2-deck category are collapsed
			//into one (see defaultProbsTotal)
			public float totalProb() {
				return prob + prob2;
			}
		}

		//package-private (not private): populated from ItemRegistry.registerGeneratorEntries(),
		//which lives in this same package - see that method's class comment for why the data entry
		//point moved there (audit finding #1)
		static ItemEntry entry( Class<?> cls, float prob ) {
			return new ItemEntry( cls, prob );
		}

		static ItemEntry entry( Class<?> cls, float prob, float prob2 ) {
			return new ItemEntry( cls, prob, prob2 );
		}

		//single-deck category: only defaultProbs is populated, probs starts as a copy of it
		void setEntries( ItemEntry... entries ) {
			this.entries = entries;
			classes = new Class<?>[entries.length];
			defaultProbs = new float[entries.length];
			for (int i = 0; i < entries.length; i++) {
				classes[i] = entries[i].cls;
				defaultProbs[i] = entries[i].prob;
			}
			probs = defaultProbs.clone();
		}

		//two-deck category: defaultProbs2 is also populated, from each entry's prob2
		void setEntriesWithSecondDeck( ItemEntry... entries ) {
			setEntries( entries );
			defaultProbs2 = new float[entries.length];
			for (int i = 0; i < entries.length; i++) {
				defaultProbs2[i] = entries[i].prob2;
			}
		}

		//no-deck category (GOLD, ARMOR, and the WEAPON/MISSILE placeholders): only classes/probs
		//are populated directly, defaultProbs stays null, which is what excludes these categories
		//from the deck-reset/save logic below (see e.g. reset(), storeInBundle())
		void setEntriesNoDeck( ItemEntry... entries ) {
			this.entries = entries;
			classes = new Class<?>[entries.length];
			probs = new float[entries.length];
			for (int i = 0; i < entries.length; i++) {
				classes[i] = entries[i].cls;
				probs[i] = entries[i].prob;
			}
		}

		//some generator categories can have ordering within that category as well
		// note that sub category ordering doesn't need to always include items that belong
		// to that categories superclass, e.g. bombs are ordered within thrown weapons
		private static HashMap<Class, ArrayList<Class>> subOrderings = new HashMap<>();
		static {
			subOrderings.put(Trinket.class, new ArrayList<>(Arrays.asList(Trinket.class, TrinketCatalyst.class)));
			subOrderings.put(MissileWeapon.class, new ArrayList<>(Arrays.asList(MissileWeapon.class, Bomb.class)));
			subOrderings.put(Potion.class, new ArrayList<>(Arrays.asList(Waterskin.class, Potion.class, ExoticPotion.class, Brew.class, Elixir.class, LiquidMetal.class)));
			subOrderings.put(Scroll.class, new ArrayList<>(Arrays.asList(Scroll.class, ExoticScroll.class, Spell.class, ArcaneResin.class)));
		}

		//in case there are multiple matches, this will return the latest match
		public static int order( Item item ) {
			int catResult = -1, subResult = 0;
			for (int i=0; i < values().length; i++) {
				ArrayList<Class> subOrdering = subOrderings.get(values()[i].superClass);
				if (subOrdering != null){
					for (int j=0; j < subOrdering.size(); j++){
						if (subOrdering.get(j).isInstance(item)){
							catResult = i;
							subResult = j;
						}
					}
				} else {
					if (values()[i].superClass.isInstance(item)) {
						catResult = i;
						subResult = 0;
					}
				}
			}
			if (catResult != -1) return catResult*100 + subResult;

			//items without a category-defined order are sorted based on the spritesheet
			return Short.MAX_VALUE+item.image();
		}

		//item<->weight pairing itself lives in ItemRegistry.registerGeneratorEntries() now (audit
		//finding #1) - this just triggers it (and derives defaultProbsTotal, which isn't per-item
		//data, it's generic bookkeeping over whatever entries ended up set)
		static {
			ItemRegistry.registerGeneratorEntries();

			for (Category cat : Category.values()){
				if (cat.defaultProbs2 != null){
					cat.defaultProbsTotal = new float[cat.defaultProbs.length];
					for (int i = 0; i < cat.defaultProbs.length; i++){
						cat.defaultProbsTotal[i] = cat.defaultProbs[i] + cat.defaultProbs2[i];
					}
				}
			}
		}
	}

	private static final float[][] floorSetTierProbs = new float[][] {
			{0, 75, 20,  4,  1},
			{0, 25, 50, 20,  5},
			{0,  0, 40, 50, 10},
			{0,  0, 20, 40, 40},
			{0,  0,  0, 20, 80}
	};

	private static boolean usingFirstDeck = false;
	private static HashMap<Category,Float> defaultCatProbs = new LinkedHashMap<>();
	private static HashMap<Category,Float> categoryProbs = new LinkedHashMap<>();

	public static void fullReset() {
		usingFirstDeck = Random.Int(2) == 0;
		generalReset();
		for (Category cat : Category.values()) {
			cat.using2ndProbs =  cat.defaultProbs2 != null && Random.Int(2) == 0;
			reset(cat);
			if (cat.defaultProbs != null) {
				cat.seed = Random.Long();
				cat.dropped = 0;
			}
		}
	}

	public static void generalReset(){
		for (Category cat : Category.values()) {
			categoryProbs.put( cat, usingFirstDeck ? cat.firstProb : cat.secondProb );
			defaultCatProbs.put( cat, cat.firstProb + cat.secondProb );
		}
	}

	public static void reset(Category cat){
		if (cat.defaultProbs != null) {
			if (cat.defaultProbs2 != null){
				cat.using2ndProbs = !cat.using2ndProbs;
				cat.probs = cat.using2ndProbs ? cat.defaultProbs2.clone() : cat.defaultProbs.clone();
			} else {
				cat.probs = cat.defaultProbs.clone();
			}
		}
	}

	//reverts changes to drop chances generates by this item
	//equivalent of shuffling the card back into the deck, does not preserve order!
	public static void undoDrop(Item item){
		undoDrop(item.getClass());
	}

	public static void undoDrop(Class cls){
		for (Category cat : Category.values()){
			if (cls.isAssignableFrom(cat.superClass)){
				if (cat.defaultProbs == null) continue;
				for (int i = 0; i < cat.classes.length; i++){
					if (cls == cat.classes[i]){
						cat.probs[i]++;
					}
				}
			}
		}
	}

	public static Item random() {
		Category cat = Random.chances( categoryProbs );
		if (cat == null){
			usingFirstDeck = !usingFirstDeck;
			generalReset();
			cat = Random.chances( categoryProbs );
		}
		categoryProbs.put( cat, categoryProbs.get( cat ) - 1);

		if (cat == Category.SEED) {
			//We specifically use defaults for seeds here because, unlike other item categories
			// their predominant source of drops is grass, not levelgen. This way the majority
			// of seed drops still use a deck, but the few that are spawned by levelgen are consistent
			return randomUsingDefaults(cat);
		} else {
			return random(cat);
		}
	}

	public static Item randomUsingDefaults(){
		return randomUsingDefaults(Random.chances( defaultCatProbs ));
	}

	public static Item random( Category cat ) {
		switch (cat) {
			case ARMOR:
				return randomArmor();
			case WEAPON:
				return randomWeapon();
			case MISSILE:
				return randomMissile();
			case ARTIFACT:
				Item item = randomArtifact();
				//if we're out of artifacts, return a ring instead.
				return item != null ? item : random(Category.RING);
			default:
				if (cat.defaultProbs != null && cat.seed != null){
					Random.pushGenerator(cat.seed);
					for (int i = 0; i < cat.dropped; i++) Random.Long();
				}

				int i = Random.chances(cat.probs);
				if (i == -1) {
					reset(cat);
					i = Random.chances(cat.probs);
				}
				if (cat.defaultProbs != null) cat.probs[i]--;
				Class<?> itemCls = cat.classes[i];

				if (cat.defaultProbs != null && cat.seed != null){
					Random.popGenerator();
					cat.dropped++;
				}

				if (ExoticPotion.regToExo.containsKey(itemCls)){
					if (Random.Float() < ExoticCrystals.consumableExoticChance()){
						itemCls = ExoticPotion.regToExo.get(itemCls);
					}
				} else if (ExoticScroll.regToExo.containsKey(itemCls)){
					if (Random.Float() < ExoticCrystals.consumableExoticChance()){
						itemCls = ExoticScroll.regToExo.get(itemCls);
					}
				}

				return ((Item) Reflection.newInstance(itemCls)).random();
		}
	}

	//overrides any deck systems and always uses default probs
	// except for artifacts, which must always use a deck
	public static Item randomUsingDefaults( Category cat ){
		if (cat == Category.WEAPON){
			return randomWeapon(true);
		} else if (cat == Category.MISSILE){
			return randomMissile(true);
		} else if (cat.defaultProbs == null || cat == Category.ARTIFACT) {
			return random(cat);
		} else if (cat.defaultProbsTotal != null){
			return ((Item) Reflection.newInstance(cat.classes[Random.chances(cat.defaultProbsTotal)])).random();
		} else {
			Class<?> itemCls = cat.classes[Random.chances(cat.defaultProbs)];

			if (ExoticPotion.regToExo.containsKey(itemCls)){
				if (Random.Float() < ExoticCrystals.consumableExoticChance()){
					itemCls = ExoticPotion.regToExo.get(itemCls);
				}
			} else if (ExoticScroll.regToExo.containsKey(itemCls)){
				if (Random.Float() < ExoticCrystals.consumableExoticChance()){
					itemCls = ExoticScroll.regToExo.get(itemCls);
				}
			}

			return ((Item) Reflection.newInstance(itemCls)).random();
		}
	}

	public static Item random( Class<? extends Item> cl ) {
		return Reflection.newInstance(cl).random();
	}

	public static Armor randomArmor(){
		return randomArmor(Dungeon.depth / 5);
	}

	public static Armor randomArmor(int floorSet) {

		floorSet = (int)GameMath.gate(0, floorSet, floorSetTierProbs.length-1);

		Armor a = (Armor)Reflection.newInstance(Category.ARMOR.classes[Random.chances(floorSetTierProbs[floorSet])]);
		a.random();
		return a;
	}

	public static final Category[] wepTiers = new Category[]{
			Category.WEP_T1,
			Category.WEP_T2,
			Category.WEP_T3,
			Category.WEP_T4,
			Category.WEP_T5
	};

	public static MeleeWeapon randomWeapon(){
		return randomWeapon(Dungeon.depth / 5);
	}

	public static MeleeWeapon randomWeapon(int floorSet) {
		return randomWeapon(floorSet, false);
	}

	public static MeleeWeapon randomWeapon(boolean useDefaults) {
		return randomWeapon(Dungeon.depth / 5, useDefaults);
	}

	public static MeleeWeapon randomWeapon(int floorSet, boolean useDefaults) {

		floorSet = (int)GameMath.gate(0, floorSet, floorSetTierProbs.length-1);

		MeleeWeapon w;
		if (useDefaults){
			w = (MeleeWeapon) randomUsingDefaults(wepTiers[Random.chances(floorSetTierProbs[floorSet])]);
		} else {
			w = (MeleeWeapon) random(wepTiers[Random.chances(floorSetTierProbs[floorSet])]);
		}
		return w;
	}

	public static final Category[] misTiers = new Category[]{
			Category.MIS_T1,
			Category.MIS_T2,
			Category.MIS_T3,
			Category.MIS_T4,
			Category.MIS_T5
	};

	public static MissileWeapon randomMissile(){
		return randomMissile(Dungeon.depth / 5);
	}

	public static MissileWeapon randomMissile(int floorSet) {
		return randomMissile(floorSet, false);
	}

	public static MissileWeapon randomMissile(boolean useDefaults) {
		return randomMissile(Dungeon.depth / 5, useDefaults);
	}

	public static MissileWeapon randomMissile(int floorSet, boolean useDefaults) {

		floorSet = (int)GameMath.gate(0, floorSet, floorSetTierProbs.length-1);

		MissileWeapon w;
		if (useDefaults){
			w = (MissileWeapon)randomUsingDefaults(misTiers[Random.chances(floorSetTierProbs[floorSet])]);
		} else {
			w = (MissileWeapon)random(misTiers[Random.chances(floorSetTierProbs[floorSet])]);
		}
		return w;
	}

	//enforces uniqueness of artifacts throughout a run.
	public static Artifact randomArtifact() {

		Category cat = Category.ARTIFACT;

		if (cat.defaultProbs != null && cat.seed != null){
			Random.pushGenerator(cat.seed);
			for (int i = 0; i < cat.dropped; i++) Random.Long();
		}

		int i = Random.chances( cat.probs );

		if (cat.defaultProbs != null && cat.seed != null){
			Random.popGenerator();
			cat.dropped++;
		}

		//if no artifacts are left, return null
		if (i == -1){
			return null;
		}

		cat.probs[i]--;
		return (Artifact) Reflection.newInstance((Class<? extends Artifact>) cat.classes[i]).random();

	}

	public static boolean removeArtifact(Class<?extends Artifact> artifact) {
		Category cat = Category.ARTIFACT;
		for (int i = 0; i < cat.classes.length; i++){
			if (cat.classes[i].equals(artifact) && cat.probs[i] > 0) {
				cat.probs[i] = 0;
				return true;
			}
		}
		return false;
	}

	private static final String FIRST_DECK = "first_deck";
	private static final String GENERAL_PROBS = "general_probs";
	private static final String CATEGORY_PROBS = "_probs";
	private static final String CATEGORY_USING_PROBS2 = "_using_probs2";
	private static final String CATEGORY_SEED = "_seed";
	private static final String CATEGORY_DROPPED = "_dropped";

	//storage is name-keyed rather than positional: both the category list and each category's item
	//list are matched by name on restore, not by array index. This means a future insertion/removal
	//of a category or item can't silently shift a stored weight onto the wrong one - a mismatch just
	//falls back to the default (already set by fullReset() below) instead of misattributing data.
	//No migration path is needed for older saves - this fork has none yet (clean cut).
	public static void storeInBundle(Bundle bundle) {
		bundle.put(FIRST_DECK, usingFirstDeck);

		Bundle genProbs = new Bundle();
		for (Category cat : Category.values()){
			genProbs.put(cat.name(), categoryProbs.get(cat));
		}
		bundle.put(GENERAL_PROBS, genProbs);

		for (Category cat : Category.values()){
			if (cat.defaultProbs == null) continue;

			Bundle catProbs = new Bundle();
			for (int i = 0; i < cat.classes.length; i++){
				catProbs.put(cat.classes[i].getName(), cat.probs[i]);
			}
			bundle.put(cat.name().toLowerCase() + CATEGORY_PROBS, catProbs);

			if (cat.defaultProbs2 != null){
				bundle.put(cat.name().toLowerCase() + CATEGORY_USING_PROBS2, cat.using2ndProbs);
			}

			if (cat.seed != null) {
				bundle.put(cat.name().toLowerCase() + CATEGORY_SEED, cat.seed);
				bundle.put(cat.name().toLowerCase() + CATEGORY_DROPPED, cat.dropped);
			}
		}
	}

	public static void restoreFromBundle(Bundle bundle) {
		fullReset();

		usingFirstDeck = bundle.getBoolean(FIRST_DECK);

		if (bundle.contains(GENERAL_PROBS)){
			Bundle genProbs = bundle.getBundle(GENERAL_PROBS);
			for (Category cat : Category.values()){
				if (genProbs.contains(cat.name())){
					categoryProbs.put(cat, genProbs.getFloat(cat.name()));
				}
			}
		}

		for (Category cat : Category.values()){
			if (bundle.contains(cat.name().toLowerCase() + CATEGORY_PROBS)){
				if (cat.defaultProbs != null){
					Bundle catProbs = bundle.getBundle(cat.name().toLowerCase() + CATEGORY_PROBS);
					for (int i = 0; i < cat.classes.length; i++){
						String clsName = cat.classes[i].getName();
						if (catProbs.contains(clsName)){
							cat.probs[i] = catProbs.getFloat(clsName);
						}
					}
				}
				if (bundle.contains(cat.name().toLowerCase() + CATEGORY_USING_PROBS2)){
					cat.using2ndProbs = bundle.getBoolean(cat.name().toLowerCase() + CATEGORY_USING_PROBS2);
				} else {
					cat.using2ndProbs = false;
				}
				if (bundle.contains(cat.name().toLowerCase() + CATEGORY_SEED)){
					cat.seed = bundle.getLong(cat.name().toLowerCase() + CATEGORY_SEED);
					cat.dropped = bundle.getInt(cat.name().toLowerCase() + CATEGORY_DROPPED);
				}
			}
		}

	}
}

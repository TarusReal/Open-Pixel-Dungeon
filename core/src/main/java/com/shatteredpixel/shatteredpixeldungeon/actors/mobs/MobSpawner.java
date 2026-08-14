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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.RatSkull;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.HashMap;

public class MobSpawner extends Actor {
	{
		actPriority = BUFF_PRIO; //as if it were a buff.
	}

	@Override
	protected boolean act() {

		if (Dungeon.level.mobCount() < Dungeon.level.mobLimit()) {

			if (Dungeon.level.spawnMob(12)){
				spend(Dungeon.level.respawnCooldown());
			} else {
				//try again in 1 turn
				spend(TICK);
			}

		} else {
			spend(Dungeon.level.respawnCooldown());
		}

		return true;
	}

	public void resetCooldown(){
		spend(-cooldown());
		spend(Dungeon.level.respawnCooldown());
	}

	public static ArrayList<Class<? extends Mob>> getMobRotation(int depth ){
		ArrayList<Class<? extends Mob>> mobs = standardMobRotation( depth );
		addRareMobs(depth, mobs);
		swapMobAlts(mobs);
		Random.shuffle(mobs);
		return mobs;
	}

	//returns a rotation of standard mobs, unshuffled. Weights/composition come from
	//MobRegistry.ROTATION - see that class for the depth-bracket table this switch used to be.
	private static ArrayList<Class<? extends Mob>> standardMobRotation( int depth ){
		int bracket = MobRegistry.bracketOfDepth(depth);
		ArrayList<Class<? extends Mob>> mobs = new ArrayList<>();
		for (MobRegistry.MobEntry e : MobRegistry.ROTATION) {
			int count = e.counts[bracket];
			for (int i = 0; i < count; i++) {
				mobs.add(e.resolver != null ? e.resolver.get() : e.cls);
			}
		}
		return mobs;
	}

	//has a chance to add a rarely spawned mobs to the rotation - data lives in MobRegistry.RARE_BONUS
	public static void addRareMobs( int depth, ArrayList<Class<?extends Mob>> rotation ){
		for (MobRegistry.RareBonusEntry e : MobRegistry.RARE_BONUS) {
			if (e.depth == depth) {
				if (Random.Float() < e.chance) rotation.add(e.cls);
				return;
			}
		}
	}

	//switches out regular mobs for their alt versions when appropriate
	private static void swapMobAlts(ArrayList<Class<?extends Mob>> rotation) {
		float altChance = 1 / 50f * RatSkull.exoticChanceMultiplier();
		for (int i = 0; i < rotation.size(); i++) {
			if (Random.Float() < altChance) {
				Class<? extends Mob> cl = rotation.get(i);
				Class<? extends Mob> alt = RARE_ALTS.get(cl);
				if (alt != null) {
					rotation.set(i, alt);
				}
			}
		}
	}

	//canonical data lives in MobRegistry.ALT_MAP; this field stays on MobSpawner (name/type
	//unchanged) since DistortionTrap.java reads it directly
	public static final HashMap<Class<?extends Mob>, Class<?extends Mob>> RARE_ALTS =
			new HashMap<>(MobRegistry.ALT_MAP);
}

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

package com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.watabou.utils.Point;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

public abstract class StandardRoom extends Room {
	
	public enum SizeCategory {
		
		NORMAL(4, 10, 1),
		LARGE(10, 14, 2),
		GIANT(14, 18, 3);
		
		public final int minDim, maxDim;
		public final int roomValue;
		
		SizeCategory(int min, int max, int val){
			minDim = min;
			maxDim = max;
			roomValue = val;
		}
		
	}
	
	public SizeCategory sizeCat;
	{ setSizeCat(); }
	
	//Note that if a room wishes to allow itself to be forced to a certain size category,
	//but would (effectively) never roll that size category, consider using Float.MIN_VALUE
	public float[] sizeCatProbs(){
		//always normal by default
		return new float[]{1, 0, 0};
	}
	
	public boolean setSizeCat(){
		return setSizeCat(0, SizeCategory.values().length-1);
	}
	
	//assumes room value is always ordinal+1
	public boolean setSizeCat( int maxRoomValue ){
		return setSizeCat(0, maxRoomValue-1);
	}
	
	//returns false if size cannot be set
	public boolean setSizeCat( int minOrdinal, int maxOrdinal ) {
		float[] probs = sizeCatProbs();
		SizeCategory[] categories = SizeCategory.values();
		
		if (probs.length != categories.length) return false;
		
		for (int i = 0; i < minOrdinal; i++)                    probs[i] = 0;
		for (int i = maxOrdinal+1; i < categories.length; i++)  probs[i] = 0;
		
		int ordinal = Random.chances(probs);
		
		if (ordinal != -1){
			sizeCat = categories[ordinal];
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public int minWidth() { return sizeCat.minDim; }
	public int maxWidth() { return sizeCat.maxDim; }
	
	@Override
	public int minHeight() { return sizeCat.minDim; }
	public int maxHeight() { return sizeCat.maxDim; }

	//larger standard rooms generally count as multiple rooms for various counting/weighting purposes
	//but there can be exceptions
	public int sizeFactor(){
		return sizeCat.roomValue;
	}

	public int mobSpawnWeight(){
		if (isEntrance()){
			return 1; //entrance rooms don't have higher mob spawns even if they're larger
		}
		return sizeFactor();
	}

	public int connectionWeight(){
		return sizeFactor() * sizeFactor();
	}

	@Override
	public boolean canMerge(Level l, Room other, Point p, int mergeTerrain) {
		int cell = l.pointToCell(pointInside(p, 1));
		return (Terrain.flags[l.map[cell]] & Terrain.SOLID) == 0;
	}

	//pairs a room class with its weight in each of the 7 depth "brackets" that StandardRoom's
	//selection actually distinguishes (see BRACKET_DEPTHS below) - the room equivalent of
	//Generator.Category.ItemEntry (audit finding #1: this used to be a room class list plus 26
	//hand-maintained float[35] literals, held together only by shared array index; see
	//docs/testing.md and StandardRoomGoldenMasterTest, which locks down that today's distribution
	//is unchanged by this refactor).
	private static class RoomEntry {
		final Class<? extends StandardRoom> cls;
		//[depth1, depth2to4, depth5, depth6to10, depth11to15, depth16to20, depth21to26]
		final float[] weights;

		private RoomEntry( Class<? extends StandardRoom> cls, float d1, float d2to4, float d5,
							float d6to10, float d11to15, float d16to20, float d21to26 ) {
			this.cls = cls;
			this.weights = new float[]{d1, d2to4, d5, d6to10, d11to15, d16to20, d21to26};
		}
	}

	private static RoomEntry entry( Class<? extends StandardRoom> cls, float d1, float d2to4, float d5,
									 float d6to10, float d11to15, float d16to20, float d21to26 ) {
		return new RoomEntry(cls, d1, d2to4, d5, d6to10, d11to15, d16to20, d21to26);
	}

	//deliberately an array, not a Map<Class,RoomEntry>: RegionDecoPatchRoom appears twice (once
	//per region, with different weights each time) - a class-keyed structure couldn't represent
	//that, an array of pairs (same shape as Generator.Category.ItemEntry[]) just works, exactly
	//as it does today via two separate list positions.
	private static final RoomEntry[] rooms = new RoomEntry[] {
			entry(SewerPipeRoom.class,       16, 16, 16, 0, 0, 0, 0),
			entry(RingRoom.class,             8,  8,  8, 0, 0, 0, 0),
			entry(WaterBridgeRoom.class,      8,  8,  8, 0, 0, 0, 0),
			entry(RegionDecoPatchRoom.class,  4,  4,  4, 0, 0, 0, 0),
			entry(CircleBasinRoom.class,      4,  4,  0, 0, 0, 0, 0),

			entry(RegionDecoLineRoom.class,   0, 0, 0, 10, 0, 0, 0),
			entry(SegmentedRoom.class,        0, 0, 0, 10, 0, 0, 0),
			entry(PillarsRoom.class,          0, 0, 0, 10, 0, 0, 0),
			entry(ChasmBridgeRoom.class,      0, 0, 0,  5, 0, 0, 0),
			entry(CellBlockRoom.class,        0, 0, 0,  5, 0, 0, 0),

			entry(CaveRoom.class,             0, 0, 0, 0, 16, 0, 0),
			entry(RegionDecoBridgeRoom.class, 0, 0, 0, 0,  8, 0, 0),
			entry(CavesFissureRoom.class,     0, 0, 0, 0,  8, 0, 0),
			entry(CirclePitRoom.class,        0, 0, 0, 0,  4, 0, 0),
			entry(CircleWallRoom.class,       0, 0, 0, 0,  4, 0, 0),

			entry(HallwayRoom.class,          0, 0, 0, 0, 0, 10, 0),
			entry(LibraryHallRoom.class,      0, 0, 0, 0, 0, 10, 0),
			entry(LibraryRingRoom.class,      0, 0, 0, 0, 0, 10, 0),
			entry(StatuesRoom.class,          0, 0, 0, 0, 0,  5, 0),
			entry(SegmentedLibraryRoom.class, 0, 0, 0, 0, 0,  5, 0),

			entry(RuinsRoom.class,            0, 0, 0, 0, 0, 0, 10),
			//2nd RegionDecoPatchRoom entry, not a copy-paste dupe of the one above: confirmed via
			//git history that the two were added in separate commits for separate, non-overlapping
			//depth brackets (d1-5 above vs. d21-26 here) - same generic room reused per-region.
			//Upstream (00-Evan/shattered-pixel-dungeon) carries the identical double entry.
			entry(RegionDecoPatchRoom.class,  0, 0, 0, 0, 0, 0, 10),
			entry(ChasmRoom.class,            0, 0, 0, 0, 0, 0, 10),
			entry(SkullsRoom.class,           0, 0, 0, 0, 0, 0,  5),
			entry(RitualRoom.class,           0, 0, 0, 0, 0, 0,  5),

			entry(PlantsRoom.class,           1, 1, 0, 1, 1, 1, 1),
			entry(AquariumRoom.class,         0, 1, 0, 1, 1, 1, 1),
			entry(PlatformRoom.class,         1, 1, 0, 1, 1, 1, 1),
			entry(BurnedRoom.class,           0, 1, 0, 1, 1, 1, 1),
			entry(FissureRoom.class,          1, 1, 0, 1, 1, 1, 1),
			entry(GrassyGraveRoom.class,      0, 1, 0, 1, 1, 1, 1),
			entry(StripedRoom.class,          1, 1, 0, 1, 1, 1, 1),
			entry(StudyRoom.class,            1, 1, 0, 1, 1, 1, 1),
			entry(SuspiciousChestRoom.class,  0, 1, 0, 1, 1, 1, 1),
			entry(MinefieldRoom.class,        0, 1, 0, 1, 1, 1, 1),
	};

	//depths sharing each bracket in the weights arrays above, in the same order - used only to
	//expand into chances[27][] below
	private static final int[][] BRACKET_DEPTHS = new int[][] {
			{1},
			{2, 3, 4},
			{5},
			{6, 7, 8, 9, 10},
			{11, 12, 13, 14, 15},
			{16, 17, 18, 19, 20},
			{21, 22, 23, 24, 25, 26},
	};

	private static float[][] chances = new float[27][];
	static {
		//one array object per bracket, assigned to every depth in that bracket - reproduces
		//today's array-aliasing (e.g. chances[2]==chances[3]==chances[4]) for every bracket that
		//spans more than one depth, not just the one CLAUDE.md calls out as an example. Random.chances
		//(SPD-classes/.../Random.java) only ever reads these arrays, never mutates them, so sharing
		//one object across several depths has no behavioral effect either way - it's purely a
		//construction-time choice, preserved here rather than normalized away.
		for (int bracket = 0; bracket < BRACKET_DEPTHS.length; bracket++) {
			float[] weights = new float[rooms.length];
			for (int i = 0; i < rooms.length; i++) {
				weights[i] = rooms[i].weights[bracket];
			}
			for (int depth : BRACKET_DEPTHS[bracket]) {
				chances[depth] = weights;
			}
		}
	}


	public static StandardRoom createRoom(){
		return Reflection.newInstance(rooms[Random.chances(chances[Dungeon.depth])].cls);
	}

}

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

package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.journal.Document;

/**
 * Single place a main-path (branch 0) region is wired up (docs/depth0-findings.md Segment 4a):
 * its depth range, its regular/boss level classes, its lore document, and its interlevel-loading
 * splash asset - replaces the parallel, hand-synced switches this used to take
 * ({@code Dungeon.newLevel()}, {@code Dungeon.bossLevel(int)}, {@code RegularLevel}'s region-
 * document switch, {@code InterlevelScene}'s splash-asset switch).
 *
 * <p>Depths 1-26 are unchanged in behavior by this table - it's a structural consolidation, not
 * a balance change (see CLAUDE.md Arbeitsregeln). Two things it deliberately does NOT cover:
 *
 * <ul>
 * <li>Depth 26 ({@code LastLevel}) - not a region of its own (no 5-floor block, no boss, no lore
 * document; both the old document switch and splash switch already fell through to their
 * {@code default} branch for it). Handled as an explicit special case by callers instead of a
 * {@link #REGIONS} entry - see {@code Dungeon.newLevel()}.</li>
 * <li>{@code ShopRoom.generateItems()}'s switch - deliberately NOT migrated here. It mixes two
 * unrelated concerns (the real per-region shop at a region's first floor, keyed by
 * {@code Dungeon.shopOnLevel()} = depth 6/11/16, and the Imp shop's tier at the City boss floor,
 * depth 20, which is one tier ABOVE what "region 4's shop tier" would suggest) - collapsing both
 * into one config value per region risks silently changing the depth-20 Imp shop's loot tier.
 * See docs/depth0-implementation.md for the full trace.</li>
 * </ul>
 *
 * <p>Region 0 (the surface) is the only entry with no boss level and no lore document - see
 * {@link Region0Level}.
 */
public class RegionDefinition {

	public final int index;
	public final int firstDepth;
	public final int floorCount;
	public final Class<? extends Level> levelClass;
	//null = no boss floor in this region (only Region 0 today)
	public final Class<? extends Level> bossLevelClass;
	//null = no lore document for this region (only Region 0 today)
	public final Document loreDocument;
	public final String splashAsset;
	//optional display name; null = none assigned (see docs/depth0-findings.md - final naming is
	// Fork-2's job, this only carries a mechanism + a temporary placeholder for Region 0)
	public final String displayName;

	private RegionDefinition(int index, int firstDepth, int floorCount,
							  Class<? extends Level> levelClass, Class<? extends Level> bossLevelClass,
							  Document loreDocument, String splashAsset, String displayName) {
		this.index = index;
		this.firstDepth = firstDepth;
		this.floorCount = floorCount;
		this.levelClass = levelClass;
		this.bossLevelClass = bossLevelClass;
		this.loreDocument = loreDocument;
		this.splashAsset = splashAsset;
		this.displayName = displayName;
	}

	public int lastDepth(){
		return firstDepth + floorCount - 1;
	}

	public static final RegionDefinition[] REGIONS = new RegionDefinition[]{
			//Region 0 reuses the Sewers splash as a placeholder (no dedicated surface art exists
			// yet); "???" is a placeholder display name, real naming is Fork-2 content work.
			new RegionDefinition(0, 0, 1, Region0Level.class, null,
					null, Assets.Splashes.SEWERS, "???"),
			new RegionDefinition(1, 1, 5, SewerLevel.class, SewerBossLevel.class,
					Document.SEWERS_GUARD, Assets.Splashes.SEWERS, null),
			new RegionDefinition(2, 6, 5, PrisonLevel.class, PrisonBossLevel.class,
					Document.PRISON_WARDEN, Assets.Splashes.PRISON, null),
			new RegionDefinition(3, 11, 5, CavesLevel.class, CavesBossLevel.class,
					Document.CAVES_EXPLORER, Assets.Splashes.CAVES, null),
			new RegionDefinition(4, 16, 5, CityLevel.class, CityBossLevel.class,
					Document.CITY_WARLOCK, Assets.Splashes.CITY, null),
			new RegionDefinition(5, 21, 5, HallsLevel.class, HallsBossLevel.class,
					Document.HALLS_KING, Assets.Splashes.HALLS, null),
	};

	//range lookup, not index-by-depth: regions have different floorCounts in principle (Region 0
	// is 1 floor, the rest are 5), so a flat depth-indexed array (as StandardRoom/MobRegistry use
	// for their finer-grained brackets) doesn't fit as naturally here
	public static RegionDefinition regionOf(int depth){
		for (RegionDefinition region : REGIONS){
			if (depth >= region.firstDepth && depth <= region.lastDepth()){
				return region;
			}
		}
		return null;
	}
}

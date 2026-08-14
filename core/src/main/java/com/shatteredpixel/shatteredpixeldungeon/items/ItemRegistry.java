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

import com.shatteredpixel.shatteredpixeldungeon.items.Generator.Category;

import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClericArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.DuelistArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.HuntressArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.MageArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.MailArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.PlateArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.RogueArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ScaleArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.WarriorArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.AlchemistsToolkit;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.ChaliceOfBlood;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.DriedRose;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.EtherealChains;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.MasterThievesArmband;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SandalsOfNature;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SkeletonKey;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TalismanOfForesight;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TimekeepersHourglass;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.UnstableSpellbook;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.food.MysteryMeat;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Pasty;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfExperience;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfFrost;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHaste;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfInvisibility;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLevitation;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLiquidFlame;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfMindVision;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfParalyticGas;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfPurity;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.Pickaxe;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfAccuracy;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfArcana;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfElements;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfEnergy;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfEvasion;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfForce;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfFuror;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfHaste;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfMight;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfSharpshooting;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfTenacity;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfWealth;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfLullaby;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMirrorImage;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRage;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRemoveCurse;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRetribution;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTerror;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTransmutation;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfAggression;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfAugmentation;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfBlast;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfBlink;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfClairvoyance;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfDeepSleep;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfDetectMagic;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfEnchantment;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfFear;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfFlock;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfIntuition;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfShock;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ChaoticCenser;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.CrackedSpyglass;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.DimensionalSundial;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ExoticCrystals;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.EyeOfNewt;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.FerretTuft;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.MimicTooth;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.MossyClump;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ParchmentScrap;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.PetrifiedSeed;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.RatSkull;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.SaltCube;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ShardOfOblivion;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ThirteenLeafClover;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.TrapMechanism;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.VialOfBlood;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.WondrousResin;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfCorrosion;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfCorruption;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfDisintegration;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFireblast;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFrost;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLightning;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfPrismaticLight;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfRegrowth;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfTransfusion;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfWarding;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.AssassinsBlade;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.BattleAxe;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Crossbow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Cudgel;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dagger;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dirk;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Flail;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Gauntlet;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Glaive;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Gloves;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greataxe;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatshield;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatsword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.HandAxe;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Katana;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Longsword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Mace;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Quarterstaff;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Rapier;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.RoundShield;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.RunicBlade;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sai;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Scimitar;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Shortsword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sickle;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Spear;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WarHammer;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WarScythe;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Whip;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.Bolas;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.FishingSpear;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ForceCube;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.HeavyBoomerang;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.Javelin;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.Kunai;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.Shuriken;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingClub;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingHammer;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingSpear;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingSpike;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingStone;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.Tomahawk;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.Trident;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart;
import com.shatteredpixel.shatteredpixeldungeon.plants.Blindweed;
import com.shatteredpixel.shatteredpixeldungeon.plants.Earthroot;
import com.shatteredpixel.shatteredpixeldungeon.plants.Fadeleaf;
import com.shatteredpixel.shatteredpixeldungeon.plants.Firebloom;
import com.shatteredpixel.shatteredpixeldungeon.plants.Icecap;
import com.shatteredpixel.shatteredpixeldungeon.plants.Mageroyal;
import com.shatteredpixel.shatteredpixeldungeon.plants.Rotberry;
import com.shatteredpixel.shatteredpixeldungeon.plants.Sorrowmoss;
import com.shatteredpixel.shatteredpixeldungeon.plants.Starflower;
import com.shatteredpixel.shatteredpixeldungeon.plants.Stormvine;
import com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass;
import com.shatteredpixel.shatteredpixeldungeon.plants.Swiftthistle;

import com.shatteredpixel.shatteredpixeldungeon.items.bombs.ArcaneBomb;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Firebomb;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.FlashBangBomb;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.FrostBomb;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.HolyBomb;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Noisemaker;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.RegrowthBomb;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.ShrapnelBomb;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.SmokeBomb;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.WoollyBomb;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.AquaBrew;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.BlizzardBrew;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.CausticBrew;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.InfernalBrew;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.ShockingBrew;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.UnstableBrew;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfAquaticRejuvenation;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfArcaneArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfDragonsBlood;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfFeatherFall;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfHoneyedHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfIcyTouch;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfMight;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfToxicEssence;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.Alchemize;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.BeaconOfReturning;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.CurseInfusion;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.MagicalInfusion;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.PhaseShift;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.ReclaimTrap;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.Recycle;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.SummonElemental;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.TelekineticGrab;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.UnstableSpell;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.WildEnergy;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.MagicalHolster;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.PotionBandolier;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.ScrollHolder;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.VelvetPouch;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.CrystalKey;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.GoldenKey;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.IronKey;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.WornKey;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.CeremonialCandle;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.CorpseDust;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.DarkGold;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.DwarfToken;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.Embers;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.GooBlob;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.MetalShard;
import com.shatteredpixel.shatteredpixeldungeon.items.remains.BowFragment;
import com.shatteredpixel.shatteredpixeldungeon.items.remains.BrokenHilt;
import com.shatteredpixel.shatteredpixeldungeon.items.remains.BrokenStaff;
import com.shatteredpixel.shatteredpixeldungeon.items.remains.CloakScrap;
import com.shatteredpixel.shatteredpixeldungeon.items.remains.SealShard;
import com.shatteredpixel.shatteredpixeldungeon.items.remains.TornPage;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.TrinketCatalyst;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Berry;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Blandfruit;
import com.shatteredpixel.shatteredpixeldungeon.items.food.ChargrilledMeat;
import com.shatteredpixel.shatteredpixeldungeon.items.food.FrozenCarpaccio;
import com.shatteredpixel.shatteredpixeldungeon.items.food.MeatPie;
import com.shatteredpixel.shatteredpixeldungeon.items.food.PhantomMeat;
import com.shatteredpixel.shatteredpixeldungeon.items.food.SmallRation;
import com.shatteredpixel.shatteredpixeldungeon.items.food.StewedMeat;
import com.shatteredpixel.shatteredpixeldungeon.items.food.SupplyRation;

import static com.shatteredpixel.shatteredpixeldungeon.items.Generator.Category.entry;

/**
 * Single place a new item is wired up (audit finding #1): its {@link Generator} loot-table
 * category and weight(s), and/or its {@link com.shatteredpixel.shatteredpixeldungeon.journal.Catalog}
 * bucket membership for items that don't drop via a Generator category at all.
 *
 * <p><b>Generator side ({@link #registerGeneratorEntries()}):</b> this is the exact content that
 * used to live in {@code Generator.Category}'s own static block, moved here unchanged (same
 * {@code entry(...)}/{@code setEntries*(...)} calls, same order, same weights - WEP_T3's former
 * bug (an extra assignment after {@code setEntries()} clobbered its probs with WEP_T1's) has since
 * been fixed by simply deleting that stray line; {@code setEntries()} alone already produces the
 * correct, uniform weights, see {@code GeneratorGoldenMasterTest}). {@code Generator.java}
 * now just calls {@link #registerGeneratorEntries()} to trigger this; {@code Category.setEntries}
 * and friends were widened from {@code private} to package-private so this class (same package)
 * can call them.
 *
 * <p><b>Catalog side (the {@code Class<?>[]} constants below):</b> most Catalog buckets need no
 * entry here at all - {@code Catalog.java} derives their membership straight from the matching
 * {@code Generator.Category.X.classes} (e.g. {@code MELEE_WEAPONS} from {@code WEP_T1..WEP_T5}),
 * so an item that already has a Generator entry above needs nothing further. These constants only
 * cover items with *no* Generator category - things placed by specific game logic rather than
 * drawn from the loot table (bombs, brews/elixirs, spells, misc equipment/consumables, and a few
 * food items beyond the three the FOOD category already covers). Deliberately plain
 * {@code Class<?>[]} fields, not keyed by the {@code Catalog} enum: referencing a {@code Catalog}
 * constant here would trigger {@code Catalog}'s own class init while this class is still
 * mid-init, since {@code Catalog.java} calls back into this class - a real circular static-init
 * risk between two top-level classes in different packages, avoided by not creating the edge in
 * the first place.
 */
public class ItemRegistry {

	private ItemRegistry(){}

	//catalog buckets with no Generator presence - see class comment. Where an item family has
	//*some* members covered by a Generator category and some not (FOOD, MISC_CONSUMABLES/Gold),
	//only the extra, non-Generator members are listed here; Catalog.java adds the Generator-side
	//classes separately via Generator.Category.X.classes, so nothing is listed twice.

	public static final Class<?>[] BOMBS = {
			Bomb.class, FrostBomb.class, Firebomb.class, SmokeBomb.class, RegrowthBomb.class,
			WoollyBomb.class, Noisemaker.class, FlashBangBomb.class, HolyBomb.class,
			ArcaneBomb.class, ShrapnelBomb.class
	};

	public static final Class<?>[] BREWS_ELIXIRS = {
			UnstableBrew.class, InfernalBrew.class, BlizzardBrew.class, ShockingBrew.class,
			CausticBrew.class, AquaBrew.class, ElixirOfHoneyedHealing.class,
			ElixirOfAquaticRejuvenation.class, ElixirOfArcaneArmor.class, ElixirOfDragonsBlood.class,
			ElixirOfIcyTouch.class, ElixirOfToxicEssence.class, ElixirOfMight.class,
			ElixirOfFeatherFall.class
	};

	public static final Class<?>[] SPELLS = {
			UnstableSpell.class, WildEnergy.class, TelekineticGrab.class, PhaseShift.class,
			Alchemize.class, CurseInfusion.class, MagicalInfusion.class, Recycle.class,
			ReclaimTrap.class, SummonElemental.class, BeaconOfReturning.class
	};

	public static final Class<?>[] MISC_EQUIPMENT = {
			BrokenSeal.class, SpiritBow.class, Waterskin.class, VelvetPouch.class,
			PotionBandolier.class, ScrollHolder.class, MagicalHolster.class, Amulet.class
	};

	//Gold itself stays out of this list - it's Generator.Category.GOLD.classes' one entry,
	//Catalog.java adds it from there
	public static final Class<?>[] MISC_CONSUMABLES = {
			EnergyCrystal.class, Dewdrop.class, IronKey.class, GoldenKey.class, CrystalKey.class,
			WornKey.class, TrinketCatalyst.class, Stylus.class, Torch.class, Honeypot.class,
			Ankh.class, CorpseDust.class, Embers.class, CeremonialCandle.class, DarkGold.class,
			DwarfToken.class, GooBlob.class, TengusMask.class, MetalShard.class, KingsCrown.class,
			LiquidMetal.class, ArcaneResin.class, SealShard.class, BrokenStaff.class,
			CloakScrap.class, BowFragment.class, BrokenHilt.class, TornPage.class
	};

	//Food/Pasty/MysteryMeat stay out of this list - they're Generator.Category.FOOD.classes,
	//Catalog.java adds them from there
	public static final Class<?>[] FOOD_EXTRA = {
			ChargrilledMeat.class, StewedMeat.class, FrozenCarpaccio.class, SmallRation.class,
			Berry.class, SupplyRation.class, Blandfruit.class, PhantomMeat.class, MeatPie.class
	};

	//moved verbatim from Generator.Category's own static block - see class comment
	static void registerGeneratorEntries() {

		Category.GOLD.setEntriesNoDeck(
				entry(Gold.class, 1)
		);

		Category.POTION.setEntriesWithSecondDeck(
				entry(PotionOfStrength.class, 0, 0), //2 drop every chapter, see Dungeon.posNeeded()
				entry(PotionOfHealing.class, 3, 3),
				entry(PotionOfMindVision.class, 2, 2),
				entry(PotionOfFrost.class, 1, 2),
				entry(PotionOfLiquidFlame.class, 2, 1),
				entry(PotionOfToxicGas.class, 1, 2),
				entry(PotionOfHaste.class, 1, 1),
				entry(PotionOfInvisibility.class, 1, 1),
				entry(PotionOfLevitation.class, 1, 1),
				entry(PotionOfParalyticGas.class, 1, 1),
				entry(PotionOfPurity.class, 1, 1),
				entry(PotionOfExperience.class, 1, 0)
		);

		Category.SEED.setEntries(
				entry(Rotberry.Seed.class, 0), //quest item
				entry(Sungrass.Seed.class, 2),
				entry(Fadeleaf.Seed.class, 2),
				entry(Icecap.Seed.class, 2),
				entry(Firebloom.Seed.class, 2),
				entry(Sorrowmoss.Seed.class, 2),
				entry(Swiftthistle.Seed.class, 2),
				entry(Blindweed.Seed.class, 2),
				entry(Stormvine.Seed.class, 2),
				entry(Earthroot.Seed.class, 2),
				entry(Mageroyal.Seed.class, 2),
				entry(Starflower.Seed.class, 1)
		);

		Category.SCROLL.setEntriesWithSecondDeck(
				entry(ScrollOfUpgrade.class, 0, 0), //3 drop every chapter, see Dungeon.souNeeded()
				entry(ScrollOfIdentify.class, 3, 3),
				entry(ScrollOfRemoveCurse.class, 2, 2),
				entry(ScrollOfMirrorImage.class, 1, 2),
				entry(ScrollOfRecharging.class, 2, 1),
				entry(ScrollOfTeleportation.class, 1, 2),
				entry(ScrollOfLullaby.class, 1, 1),
				entry(ScrollOfMagicMapping.class, 1, 1),
				entry(ScrollOfRage.class, 1, 1),
				entry(ScrollOfRetribution.class, 1, 1),
				entry(ScrollOfTerror.class, 1, 1),
				entry(ScrollOfTransmutation.class, 1, 0)
		);

		Category.STONE.setEntries(
				entry(StoneOfEnchantment.class, 0),   //1 is guaranteed to drop on floors 6-19
				entry(StoneOfIntuition.class, 2),     //1 additional stone is also dropped on floors 1-3
				entry(StoneOfDetectMagic.class, 2),
				entry(StoneOfFlock.class, 2),
				entry(StoneOfShock.class, 2),
				entry(StoneOfBlink.class, 2),
				entry(StoneOfDeepSleep.class, 2),
				entry(StoneOfClairvoyance.class, 2),
				entry(StoneOfAggression.class, 2),
				entry(StoneOfBlast.class, 2),
				entry(StoneOfFear.class, 2),
				entry(StoneOfAugmentation.class, 0)  //1 is sold in each shop
		);

		Category.WAND.setEntries(
				entry(WandOfMagicMissile.class, 3),
				entry(WandOfLightning.class, 3),
				entry(WandOfDisintegration.class, 3),
				entry(WandOfFireblast.class, 3),
				entry(WandOfCorrosion.class, 3),
				entry(WandOfBlastWave.class, 3),
				entry(WandOfLivingEarth.class, 3),
				entry(WandOfFrost.class, 3),
				entry(WandOfPrismaticLight.class, 3),
				entry(WandOfWarding.class, 3),
				entry(WandOfTransfusion.class, 3),
				entry(WandOfCorruption.class, 3),
				entry(WandOfRegrowth.class, 3)
		);

		//see Generator.randomWeapon
		Category.WEAPON.setEntriesNoDeck();

		Category.WEP_T1.setEntries(
				entry(WornShortsword.class, 2),
				entry(MagesStaff.class, 0),
				entry(Dagger.class, 2),
				entry(Gloves.class, 2),
				entry(Rapier.class, 2),
				entry(Cudgel.class, 2)
		);

		Category.WEP_T2.setEntries(
				entry(Shortsword.class, 2),
				entry(HandAxe.class, 2),
				entry(Spear.class, 2),
				entry(Quarterstaff.class, 2),
				entry(Dirk.class, 2),
				entry(Sickle.class, 2),
				entry(Pickaxe.class, 0)
		);

		Category.WEP_T3.setEntries(
				entry(Sword.class, 2),
				entry(Mace.class, 2),
				entry(Scimitar.class, 2),
				entry(RoundShield.class, 2),
				entry(Sai.class, 2),
				entry(Whip.class, 2)
		);

		Category.WEP_T4.setEntries(
				entry(Longsword.class, 2),
				entry(BattleAxe.class, 2),
				entry(Flail.class, 2),
				entry(RunicBlade.class, 2),
				entry(AssassinsBlade.class, 2),
				entry(Crossbow.class, 2),
				entry(Katana.class, 2)
		);

		Category.WEP_T5.setEntries(
				entry(Greatsword.class, 2),
				entry(WarHammer.class, 2),
				entry(Glaive.class, 2),
				entry(Greataxe.class, 2),
				entry(Greatshield.class, 2),
				entry(Gauntlet.class, 2),
				entry(WarScythe.class, 2)
		);

		//see Generator.randomArmor - it selects purely via floorSetTierProbs against this classes
		//list, never per-item weights, so (unlike WEP_T*/MIS_T*) there's no entry() weight to pair
		//with each class here - just the classes themselves.
		Category.ARMOR.classes = new Class<?>[]{
				ClothArmor.class,
				LeatherArmor.class,
				MailArmor.class,
				ScaleArmor.class,
				PlateArmor.class,
				WarriorArmor.class,
				MageArmor.class,
				RogueArmor.class,
				HuntressArmor.class,
				DuelistArmor.class,
				ClericArmor.class
		};

		//see Generator.randomMissile
		Category.MISSILE.setEntriesNoDeck();

		Category.MIS_T1.setEntries(
				entry(ThrowingStone.class, 3),
				entry(ThrowingKnife.class, 3),
				entry(ThrowingSpike.class, 3),
				entry(Dart.class, 0)
		);

		Category.MIS_T2.setEntries(
				entry(FishingSpear.class, 3),
				entry(ThrowingClub.class, 3),
				entry(Shuriken.class, 3)
		);

		Category.MIS_T3.setEntries(
				entry(ThrowingSpear.class, 3),
				entry(Kunai.class, 3),
				entry(Bolas.class, 3)
		);

		Category.MIS_T4.setEntries(
				entry(Javelin.class, 3),
				entry(Tomahawk.class, 3),
				entry(HeavyBoomerang.class, 3)
		);

		Category.MIS_T5.setEntries(
				entry(Trident.class, 3),
				entry(ThrowingHammer.class, 3),
				entry(ForceCube.class, 3)
		);

		Category.FOOD.setEntries(
				entry(Food.class, 4),
				entry(Pasty.class, 1),
				entry(MysteryMeat.class, 0)
		);

		Category.RING.setEntries(
				entry(RingOfAccuracy.class, 3),
				entry(RingOfArcana.class, 3),
				entry(RingOfElements.class, 3),
				entry(RingOfEnergy.class, 3),
				entry(RingOfEvasion.class, 3),
				entry(RingOfForce.class, 3),
				entry(RingOfFuror.class, 3),
				entry(RingOfHaste.class, 3),
				entry(RingOfMight.class, 3),
				entry(RingOfSharpshooting.class, 3),
				entry(RingOfTenacity.class, 3),
				entry(RingOfWealth.class, 3)
		);

		Category.ARTIFACT.setEntries(
				entry(AlchemistsToolkit.class, 1),
				entry(ChaliceOfBlood.class, 1),
				entry(CloakOfShadows.class, 0),
				entry(DriedRose.class, 1),
				entry(EtherealChains.class, 1),
				entry(HolyTome.class, 0),
				entry(HornOfPlenty.class, 1),
				entry(MasterThievesArmband.class, 1),
				entry(SandalsOfNature.class, 1),
				entry(SkeletonKey.class, 1),
				entry(TalismanOfForesight.class, 1),
				entry(TimekeepersHourglass.class, 1),
				entry(UnstableSpellbook.class, 1)
		);

		//Trinkets are unique like artifacts, but unlike them you can only have one at once
		//So we don't need the same enforcement of uniqueness
		Category.TRINKET.setEntries(
				entry(RatSkull.class, 1),
				entry(ParchmentScrap.class, 1),
				entry(PetrifiedSeed.class, 1),
				entry(ExoticCrystals.class, 1),
				entry(MossyClump.class, 1),
				entry(DimensionalSundial.class, 1),
				entry(ThirteenLeafClover.class, 1),
				entry(TrapMechanism.class, 1),
				entry(MimicTooth.class, 1),
				entry(WondrousResin.class, 1),
				entry(EyeOfNewt.class, 1),
				entry(SaltCube.class, 1),
				entry(VialOfBlood.class, 1),
				entry(ShardOfOblivion.class, 1),
				entry(ChaoticCenser.class, 1),
				entry(FerretTuft.class, 1),
				entry(CrackedSpyglass.class, 1)
		);
	}
}

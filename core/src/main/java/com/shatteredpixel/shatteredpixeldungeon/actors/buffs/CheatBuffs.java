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

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.watabou.utils.Bundle;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.LinkedHashSet;

//Registry of status effects that the cheat menu can apply to the hero,
//plus a hidden buff that keeps 'permanent' effects active.
public class CheatBuffs {

	//applies a status effect for the given number of turns (or amount, for shields/bleeding)
	public static abstract class Entry {
		public final Class<? extends Buff> cls;

		public Entry(Class<? extends Buff> cls){
			this.cls = cls;
		}

		public abstract void apply(Hero hero, int turns);

		//a throwaway instance, used for name/icon/type in the menu
		public Buff sample(){
			return Reflection.newInstance(cls);
		}
	}

	//most timed effects
	private static class Timed extends Entry {
		Timed(Class<? extends FlavourBuff> cls){ super(cls); }

		@Override
		@SuppressWarnings("unchecked")
		public void apply(Hero hero, int turns) {
			Buff.prolong(hero, (Class<? extends FlavourBuff>) cls, turns);
		}
	}

	public static final ArrayList<Entry> ENTRIES = new ArrayList<>();

	private static void timed(Class<? extends FlavourBuff> cls){
		ENTRIES.add(new Timed(cls));
	}

	static {
		//positive
		timed(Adrenaline.class);
		timed(Bless.class);
		timed(BlobImmunity.class);
		timed(EnhancedRings.class);
		timed(Foresight.class);
		timed(FrostImbue.class);
		timed(Haste.class);
		timed(Invisibility.class);
		timed(Invulnerability.class);
		timed(Levitation.class);
		timed(Light.class);
		timed(MagicalSight.class);
		timed(MindVision.class);
		timed(Recharging.class);
		timed(Stamina.class);

		ENTRIES.add(new Entry(GreaterHaste.class){
			public void apply(Hero hero, int turns){ Buff.affect(hero, GreaterHaste.class).set(turns); }
		});
		ENTRIES.add(new Entry(FireImbue.class){
			public void apply(Hero hero, int turns){ Buff.affect(hero, FireImbue.class).set(turns); }
		});
		ENTRIES.add(new Entry(ToxicImbue.class){
			public void apply(Hero hero, int turns){ Buff.affect(hero, ToxicImbue.class).set(turns); }
		});
		ENTRIES.add(new Entry(Barkskin.class){
			public void apply(Hero hero, int turns){ Buff.affect(hero, Barkskin.class).set(Math.max(5, hero.lvl), turns); }
		});
		ENTRIES.add(new Entry(ArcaneArmor.class){
			public void apply(Hero hero, int turns){ Buff.affect(hero, ArcaneArmor.class).set(Math.max(5, hero.lvl), turns); }
		});
		ENTRIES.add(new Entry(AdrenalineSurge.class){
			public void apply(Hero hero, int turns){ Buff.affect(hero, AdrenalineSurge.class).reset(2, turns); }
		});
		ENTRIES.add(new Entry(ArtifactRecharge.class){
			public void apply(Hero hero, int turns){ Buff.affect(hero, ArtifactRecharge.class).set(turns); }
		});
		ENTRIES.add(new Entry(ScrollEmpower.class){
			public void apply(Hero hero, int turns){ Buff.affect(hero, ScrollEmpower.class).reset(turns); }
		});
		//shield amount instead of turns
		ENTRIES.add(new Entry(Barrier.class){
			public void apply(Hero hero, int turns){ Buff.affect(hero, Barrier.class).setShield(turns); }
		});
		ENTRIES.add(new Entry(WellFed.class){
			public void apply(Hero hero, int turns){ Buff.affect(hero, WellFed.class).reset(); }
		});

		//negative
		timed(Blindness.class);
		timed(Chill.class);
		timed(Cripple.class);
		timed(Daze.class);
		timed(Degrade.class);
		timed(Drowsy.class);
		timed(Frost.class);
		timed(Hex.class);
		timed(MagicImmune.class);
		timed(Paralysis.class);
		timed(Roots.class);
		timed(Slow.class);
		timed(Vertigo.class);
		timed(Vulnerable.class);
		timed(Weakness.class);

		ENTRIES.add(new Entry(Poison.class){
			public void apply(Hero hero, int turns){ Buff.affect(hero, Poison.class).set(turns); }
		});
		ENTRIES.add(new Entry(Burning.class){
			public void apply(Hero hero, int turns){ Buff.affect(hero, Burning.class).reignite(hero, turns); }
		});
		ENTRIES.add(new Entry(Ooze.class){
			public void apply(Hero hero, int turns){ Buff.affect(hero, Ooze.class).set(turns); }
		});
		ENTRIES.add(new Entry(Corrosion.class){
			public void apply(Hero hero, int turns){ Buff.affect(hero, Corrosion.class).set(turns, 1); }
		});
		//bleed damage amount instead of turns
		ENTRIES.add(new Entry(Bleeding.class){
			public void apply(Hero hero, int turns){ Buff.affect(hero, Bleeding.class).set(turns); }
		});
	}

	public static Entry find(Class<?> cls){
		for (Entry e : ENTRIES){
			if (e.cls == cls) return e;
		}
		return null;
	}

	//*** permanent effects ***

	public static void makePermanent(Hero hero, Entry e){
		Buff.affect(hero, Keeper.class).classes.add(e.cls);
		e.apply(hero, Keeper.REFRESH);
	}

	public static boolean isPermanent(Hero hero, Class<?> cls){
		Keeper k = hero.buff(Keeper.class);
		return k != null && k.classes.contains(cls);
	}

	//removes every instance of the effect, and stops keeping it permanent
	public static void remove(Hero hero, Class<? extends Buff> cls){
		Keeper k = hero.buff(Keeper.class);
		if (k != null){
			k.classes.remove(cls);
			if (k.classes.isEmpty()) k.detach();
		}
		for (Buff b : hero.buffs()){
			if (b.getClass() == cls) b.detach();
		}
	}

	//hidden buff that re-applies permanent effects whenever they run low or get removed
	public static class Keeper extends Buff {

		public static final int REFRESH = 20;

		{
			revivePersists = true;
		}

		public LinkedHashSet<Class<? extends Buff>> classes = new LinkedHashSet<>();

		@Override
		public boolean act() {
			if (target instanceof Hero){
				for (Class<? extends Buff> cls : new ArrayList<>(classes)){
					Entry e = find(cls);
					if (e == null) continue;
					Buff existing = target.buff(cls);
					if (existing == null
							|| (existing instanceof FlavourBuff && existing.cooldown() < REFRESH/2f)){
						e.apply((Hero) target, REFRESH);
					}
				}
			}
			spend(TICK);
			return true;
		}

		private static final String CLASSES = "classes";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(CLASSES, classes.toArray(new Class[0]));
		}

		@Override
		@SuppressWarnings("unchecked")
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			classes.clear();
			if (bundle.contains(CLASSES)){
				for (Class<?> c : bundle.getClassArray(CLASSES)){
					if (c != null && Buff.class.isAssignableFrom(c)) classes.add((Class<? extends Buff>) c);
				}
			}
		}
	}

	//true for effects the hero is currently affected by that show an icon
	public static boolean isVisible(Buff b){
		return b.icon() != com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.NONE;
	}
}

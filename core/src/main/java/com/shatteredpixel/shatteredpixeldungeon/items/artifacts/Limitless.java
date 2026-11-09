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

package com.shatteredpixel.shatteredpixeldungeon.items.artifacts;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Regeneration;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.Beam;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BlastParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.EnergyParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.PurpleParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfEnergy;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Gojo's blindfold. Holds his cursed energy: while worn it grants Infinity (reduced damage
//from attackers) and lets him spend charge on his techniques.
public class Limitless extends Artifact {

	public static final String AC_TECHNIQUE = "TECHNIQUE";

	public static final int BLUE = 0, RED = 1, PURPLE = 2, DOMAIN = 3;
	public static final int[] COSTS = {2, 3, 6, 10};
	private static final String[] KEYS = {"blue", "red", "purple", "domain"};

	{
		image = ItemSpriteSheet.LIMITLESS;

		levelCap = 10;
		charge = 10;
		chargeCap = 10;

		unique = true;
		bones = false;

		defaultAction = AC_TECHNIQUE;
	}

	private int pending = -1;

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		if (isEquipped(hero) && !cursed && hero.buff(MagicImmune.class) == null){
			actions.add(AC_TECHNIQUE);
		}
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);

		if (!action.equals(AC_TECHNIQUE)) return;

		if (!isEquipped(hero)){
			GLog.i(Messages.get(Artifact.class, "need_to_equip"));
			return;
		}
		if (cursed || hero.buff(MagicImmune.class) != null){
			GLog.w(Messages.get(this, "cursed"));
			return;
		}

		String[] options = new String[KEYS.length];
		for (int i = 0; i < KEYS.length; i++){
			options[i] = Messages.get(this, KEYS[i]) + " (" + COSTS[i] + ")";
		}
		GameScene.show(new WndOptions(new ItemSprite(this),
				Messages.titleCase(name()),
				Messages.get(this, "choose", charge, chargeCap),
				options){
			@Override
			protected boolean enabled(int index) {
				return charge >= COSTS[index];
			}

			@Override
			protected void onSelect(int index) {
				if (charge < COSTS[index]) return;
				if (index == DOMAIN){
					domainExpansion(Dungeon.hero);
				} else {
					pending = index;
					GameScene.selectCell(targeter);
				}
			}
		});
	}

	private final CellSelector.Listener targeter = new CellSelector.Listener() {
		@Override
		public void onSelect(final Integer cell) {
			final Hero hero = Dungeon.hero;
			final int technique = pending;
			pending = -1;
			if (cell == null || cell == hero.pos || technique < 0 || charge < COSTS[technique]) return;

			hero.busy();
			hero.sprite.attack(cell, new Callback() {
				@Override
				public void call() {
					switch (technique){
						case BLUE:   blue(hero, cell);   break;
						case RED:    red(hero, cell);    break;
						case PURPLE: purple(hero, cell); break;
					}
					spend(technique);
					hero.spendAndNext(1f);
				}
			});
		}

		@Override
		public String prompt() {
			return Messages.get(Limitless.class, "prompt");
		}
	};

	private static boolean isFoe(Char ch){
		return ch != null && !(ch instanceof Hero) && ch.alignment != Char.Alignment.ALLY;
	}

	//Blue (attraction): pulls nearby enemies into the target point and crushes them
	private void blue(Hero hero, int center){
		int lvl = hero.lvl;
		Sample.INSTANCE.play(Assets.Sounds.CHARGEUP);
		CellEmitter.center(center).burst(EnergyParticle.FACTORY, 30);
		GLog.p(Messages.get(this, "blue_shout"));

		for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])){
			if (!isFoe(mob) || !Dungeon.level.heroFOV[mob.pos]) continue;
			if (Dungeon.level.distance(mob.pos, center) > 3) continue;

			mob.damage(Random.NormalIntRange(2 + lvl/3, 6 + lvl), this);
			if (mob.isAlive() && mob.pos != center){
				Ballistica pull = new Ballistica(mob.pos, center, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID);
				int power = pull.dist - 1;
				if (power > 0){
					WandOfBlastWave.throwChar(mob, pull, power, false, false, this);
				}
			}
			if (mob.sprite != null) mob.sprite.centerEmitter().burst(EnergyParticle.FACTORY, 6);
		}
	}

	//Red (repulsion): an explosion that blasts enemies away from the target point
	private void red(Hero hero, int center){
		int lvl = hero.lvl;
		Sample.INSTANCE.play(Assets.Sounds.BLAST);
		CellEmitter.center(center).burst(BlastParticle.FACTORY, 30);
		GLog.p(Messages.get(this, "red_shout"));

		Ballistica line = new Ballistica(hero.pos, center, Ballistica.STOP_SOLID);
		for (int n : PathFinder.NEIGHBOURS9){
			int cell = center + n;
			Char ch = Actor.findChar(cell);
			if (!isFoe(ch)) continue;

			ch.damage(Random.NormalIntRange(4 + lvl/2, 10 + 3*lvl/2), this);
			if (!ch.isAlive()) continue;

			Ballistica push = null;
			if (n != 0){
				push = new Ballistica(cell, cell + n, Ballistica.MAGIC_BOLT);
			} else {
				int idx = line.path.indexOf(center);
				if (idx >= 0 && idx + 1 < line.path.size()){
					push = new Ballistica(center, line.path.get(idx + 1), Ballistica.MAGIC_BOLT);
				}
			}
			if (push != null){
				WandOfBlastWave.throwChar(ch, push, 3, false, true, this);
			}
		}
	}

	//Hollow Purple: a beam of imaginary mass that tears through everything in a line
	private void purple(Hero hero, int target){
		int lvl = hero.lvl;
		Ballistica path = new Ballistica(hero.pos, target, Ballistica.STOP_SOLID);
		hero.sprite.parent.add(new Beam.DeathRay(hero.sprite.center(),
				DungeonTilemap.raisedTileCenterToWorld(path.collisionPos)));
		Sample.INSTANCE.play(Assets.Sounds.RAY);
		Sample.INSTANCE.play(Assets.Sounds.BLAST, 0.8f);
		GLog.p(Messages.get(this, "purple_shout"));

		ArrayList<Char> targets = new ArrayList<>();
		for (int c : path.subPath(1, path.dist)){
			Char ch = Actor.findChar(c);
			if (isFoe(ch)) targets.add(ch);
			CellEmitter.center(c).burst(PurpleParticle.BURST, 3);
		}
		for (Char ch : targets){
			ch.damage(Random.NormalIntRange(10 + 3*lvl, 20 + 5*lvl), this);
			if (ch.sprite != null) ch.sprite.flash();
		}
	}

	//Domain Expansion, Unlimited Void: everything in sight is overwhelmed by infinite information
	private void domainExpansion(Hero hero){
		int turns = 5 + hero.lvl/4;
		GameScene.flash(0x6633CC, true);
		Sample.INSTANCE.play(Assets.Sounds.TELEPORT);
		GLog.p(Messages.get(this, "domain_shout"));

		for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])){
			if (isFoe(mob) && Dungeon.level.heroFOV[mob.pos]){
				Buff.prolong(mob, Paralysis.class, turns);
				if (mob.sprite != null) mob.sprite.centerEmitter().burst(PurpleParticle.BURST, 8);
			}
		}
		spend(DOMAIN);
		hero.spendAndNext(1f);
	}

	//spends charge and gains experience towards the next level
	private void spend(int technique){
		charge = Math.max(0, charge - COSTS[technique]);
		exp += COSTS[technique];
		int needed = 6 + 2*level();
		if (exp >= needed && level() < levelCap){
			exp -= needed;
			upgrade();
			GLog.p(Messages.get(this, "levelup"));
		}
		updateQuickslot();
	}

	//fraction of damage from attackers that Infinity stops
	public static float infinityReduction(int level){
		return 0.25f + 0.025f*level;
	}

	@Override
	protected ArtifactBuff passiveBuff() {
		return new Infinity();
	}

	@Override
	public void charge(Hero target, float amount) {
		if (cursed || target.buff(MagicImmune.class) != null) return;
		partialCharge += amount;
		while (partialCharge >= 1 && charge < chargeCap){
			charge++;
			partialCharge--;
		}
		if (charge >= chargeCap) partialCharge = 0;
		updateQuickslot();
	}

	@Override
	public String desc() {
		String desc = Messages.get(this, "desc");
		if (isEquipped(Dungeon.hero)){
			desc += "\n\n" + Messages.get(this, "desc_equipped",
					Math.round(100*infinityReduction(level())));
		}
		return desc;
	}

	//Infinity: passive damage reduction, also recharges cursed energy over time
	public class Infinity extends ArtifactBuff {

		public float reduction(){
			return isCursed() ? 0 : infinityReduction(itemLevel());
		}

		@Override
		public boolean act() {
			if (charge < chargeCap && !cursed && target.buff(MagicImmune.class) == null
					&& Regeneration.regenOn()){
				//one charge every 6 turns at +0, every 3 turns at +10
				partialCharge += RingOfEnergy.artifactChargeMultiplier(target) / (6f - 0.3f*level());
				while (partialCharge >= 1 && charge < chargeCap){
					charge++;
					partialCharge--;
				}
				if (charge >= chargeCap) partialCharge = 0;
				updateQuickslot();
			}
			spend(TICK);
			return true;
		}
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		pending = -1;
	}
}

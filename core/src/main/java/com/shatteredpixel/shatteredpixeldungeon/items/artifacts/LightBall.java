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
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Lightning;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfEnergy;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Pikachu's light ball: stores electricity that powers its moves.
public class LightBall extends Artifact {

	public static final String AC_MOVE = "MOVE";

	public static final int QUICK_ATTACK = 0, THUNDERBOLT = 1, VOLT_TACKLE = 2, THUNDER = 3;
	public static final int[] COSTS = {1, 3, 5, 8};
	private static final String[] KEYS = {"quick_attack", "thunderbolt", "volt_tackle", "thunder"};
	//how far the dash moves can travel
	private static final int DASH_RANGE = 5;

	{
		image = ItemSpriteSheet.LIGHT_BALL;

		levelCap = 10;
		charge = 10;
		chargeCap = 10;

		unique = true;
		bones = false;

		defaultAction = AC_MOVE;
	}

	private int pending = -1;

	private static final ItemSprite.Glowing YELLOW = new ItemSprite.Glowing( 0xFFE040, 1f );

	@Override
	public ItemSprite.Glowing glowing() {
		return YELLOW;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		if (isEquipped(hero) && !cursed && hero.buff(MagicImmune.class) == null){
			actions.add(AC_MOVE);
		}
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);

		if (!action.equals(AC_MOVE)) return;

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
				if (index == THUNDER){
					thunder(Dungeon.hero);
				} else {
					pending = index;
					GameScene.selectCell(targeter);
				}
			}
		});
	}

	private final CellSelector.Listener targeter = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			Hero hero = Dungeon.hero;
			int move = pending;
			pending = -1;
			if (cell == null || cell == hero.pos || move < 0 || charge < COSTS[move]) return;

			switch (move){
				case QUICK_ATTACK: quickAttack(hero, cell); break;
				case THUNDERBOLT:  thunderbolt(hero, cell); break;
				case VOLT_TACKLE:  voltTackle(hero, cell);  break;
			}
		}

		@Override
		public String prompt() {
			return Messages.get(LightBall.class, "prompt");
		}
	};

	private static boolean isFoe(Char ch){
		return ch != null && !(ch instanceof Hero) && ch.alignment != Char.Alignment.ALLY;
	}

	//last free cell on a dash path towards the target, and the first enemy that blocks it (if any)
	private static int dashEnd(Hero hero, Ballistica path, int range){
		int end = hero.pos;
		int max = Math.min(path.dist, range);
		for (int i = 1; i <= max; i++){
			int c = path.path.get(i);
			if (Dungeon.level.solid[c] || !Dungeon.level.passable[c] || Actor.findChar(c) != null) break;
			end = c;
		}
		return end;
	}

	private static void landAt(Hero hero, int dest){
		hero.move(dest);
		Dungeon.level.occupyCell(hero);
		Dungeon.observe();
		GameScene.updateFog();
	}

	//Quick Attack: dash next to an enemy and strike first
	private void quickAttack(final Hero hero, int cell){
		final Char target = Actor.findChar(cell);
		if (!isFoe(target)){
			GLog.w(Messages.get(this, "need_target"));
			return;
		}
		Ballistica path = new Ballistica(hero.pos, cell, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID);
		if (path.dist > DASH_RANGE + 1 || path.collisionPos != cell){
			GLog.w(Messages.get(this, "too_far"));
			return;
		}
		final int dest = dashEnd(hero, path, path.dist - 1);
		GLog.p(Messages.get(this, "quick_attack_shout"));
		spendCharge(QUICK_ATTACK);
		hero.busy();
		hero.sprite.jump(hero.pos, dest, 0, 0.1f, new Callback() {
			@Override
			public void call() {
				if (dest != hero.pos) landAt(hero, dest);
				if (target.isAlive() && Dungeon.level.adjacent(hero.pos, target.pos)){
					hero.sprite.attack(target.pos, new Callback() {
						@Override
						public void call() {
							hero.attack(target, 1.25f, 0, Char.INFINITE_ACCURACY);
							hero.spendAndNext(hero.attackDelay() / 2f);
						}
					});
				} else {
					hero.spendAndNext(0.5f);
				}
			}
		});
	}

	//Thunderbolt: a lightning strike that may paralyze, arcing to an enemy next to the target
	private void thunderbolt(final Hero hero, int cell){
		Ballistica bolt = new Ballistica(hero.pos, cell, Ballistica.MAGIC_BOLT);
		final Char target = Actor.findChar(bolt.collisionPos);
		final int lvl = hero.lvl;

		final ArrayList<Lightning.Arc> arcs = new ArrayList<>();
		arcs.add(new Lightning.Arc(hero.sprite.center(), bolt.collisionPos));
		final ArrayList<Char> hits = new ArrayList<>();
		if (isFoe(target)){
			hits.add(target);
			for (int n : com.watabou.utils.PathFinder.NEIGHBOURS8){
				Char other = Actor.findChar(target.pos + n);
				if (isFoe(other)){
					hits.add(other);
					arcs.add(new Lightning.Arc(target.pos, other.pos));
					break;
				}
			}
		}
		GLog.p(Messages.get(this, "thunderbolt_shout"));
		spendCharge(THUNDERBOLT);
		hero.busy();
		final int impact = bolt.collisionPos;
		hero.sprite.attack(impact, new Callback() {
			@Override
			public void call() {
				hero.sprite.parent.addToFront(new Lightning(arcs, null));
				Sample.INSTANCE.play(Assets.Sounds.LIGHTNING);
				for (int i = 0; i < hits.size(); i++){
					Char ch = hits.get(i);
					int dmg = Random.NormalIntRange(5 + lvl, 10 + 2*lvl);
					if (i > 0) dmg /= 2; //the arc is weaker
					ch.damage(dmg, LightBall.this);
					if (ch.isAlive() && Random.Int(2) == 0) Buff.prolong(ch, Paralysis.class, 2f);
					if (ch.sprite != null) ch.sprite.centerEmitter().burst(SparkParticle.FACTORY, 6);
				}
				CellEmitter.center(impact).burst(SparkParticle.FACTORY, 4);
				hero.spendAndNext(1f);
			}
		});
	}

	//Volt Tackle: charge through everything in a line, at the cost of some recoil damage
	private void voltTackle(final Hero hero, int cell){
		Ballistica path = new Ballistica(hero.pos, cell, Ballistica.STOP_SOLID);
		final int lvl = hero.lvl;
		int range = Math.min(path.dist, DASH_RANGE + 1);

		final ArrayList<Char> hits = new ArrayList<>();
		int dest = hero.pos;
		for (int i = 1; i <= range; i++){
			int c = path.path.get(i);
			if (Dungeon.level.solid[c] || !Dungeon.level.passable[c]) break;
			Char ch = Actor.findChar(c);
			if (isFoe(ch)) hits.add(ch);
			else if (ch == null) dest = c;
		}
		GLog.p(Messages.get(this, "volt_tackle_shout"));
		spendCharge(VOLT_TACKLE);
		hero.busy();
		final int landing = dest;
		hero.sprite.jump(hero.pos, landing, 0, 0.15f, new Callback() {
			@Override
			public void call() {
				if (landing != hero.pos) landAt(hero, landing);
				Sample.INSTANCE.play(Assets.Sounds.LIGHTNING);
				int dealt = 0;
				for (Char ch : hits){
					int dmg = Random.NormalIntRange(8 + 2*lvl, 16 + 3*lvl);
					ch.damage(dmg, LightBall.this);
					dealt += dmg;
					if (ch.sprite != null) ch.sprite.centerEmitter().burst(SparkParticle.FACTORY, 8);
				}
				//recoil
				int recoil = dealt / 5;
				if (recoil > 0){
					hero.damage(recoil, LightBall.this);
					GLog.w(Messages.get(LightBall.class, "recoil", recoil));
				}
				hero.spendAndNext(1f);
			}
		});
	}

	//Thunder: lightning crashes down on every enemy in sight
	private void thunder(Hero hero){
		int lvl = hero.lvl;
		ArrayList<Lightning.Arc> arcs = new ArrayList<>();
		ArrayList<Char> targets = new ArrayList<>();
		for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])){
			if (isFoe(mob) && Dungeon.level.heroFOV[mob.pos]){
				targets.add(mob);
				arcs.add(new Lightning.Arc(hero.sprite.center(), mob.pos));
			}
		}
		GLog.p(Messages.get(this, "thunder_shout"));
		spendCharge(THUNDER);
		GameScene.flash(0xFFFF80, true);
		Sample.INSTANCE.play(Assets.Sounds.LIGHTNING);
		if (!arcs.isEmpty()) hero.sprite.parent.addToFront(new Lightning(arcs, null));
		for (Char ch : targets){
			ch.damage(Random.NormalIntRange(4 + lvl, 8 + 2*lvl), this);
			if (ch.isAlive()) Buff.prolong(ch, Paralysis.class, 3f);
			if (ch.sprite != null) ch.sprite.centerEmitter().burst(SparkParticle.FACTORY, 6);
		}
		hero.spendAndNext(1f);
	}

	//spends charge and gains experience towards the next level
	private void spendCharge(int move){
		charge = Math.max(0, charge - COSTS[move]);
		exp += COSTS[move];
		int needed = 6 + 2*level();
		if (exp >= needed && level() < levelCap){
			exp -= needed;
			upgrade();
			GLog.p(Messages.get(this, "levelup"));
		}
		updateQuickslot();
	}

	@Override
	protected ArtifactBuff passiveBuff() {
		return new Recharge();
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
		return Messages.get(this, "desc");
	}

	public class Recharge extends ArtifactBuff {
		@Override
		public boolean act() {
			if (charge < chargeCap && !cursed && target.buff(MagicImmune.class) == null
					&& Regeneration.regenOn()){
				//one charge every 5 turns at +0, every 2.5 turns at +10
				partialCharge += RingOfEnergy.artifactChargeMultiplier(target) / (5f - 0.25f*level());
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

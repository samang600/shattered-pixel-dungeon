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

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.Beam;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Artoria's sword. Strikes are hidden by a veil of wind (more accurate), and once fully
//charged it can unleash its true name: a beam of light that cuts through every enemy in a line.
public class Excalibur extends MeleeWeapon {

	public static final String AC_EXCALIBUR = "EXCALIBUR";

	public static final int MAX_CHARGE = 100;

	{
		image = ItemSpriteSheet.EXCALIBUR;
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 1.1f;

		tier = 2;

		unique = true;
		bones = false;

		defaultAction = AC_EXCALIBUR;
	}

	public int charge = MAX_CHARGE;
	private float partialCharge = 0f;

	private static final ItemSprite.Glowing GOLDEN = new ItemSprite.Glowing( 0xFFE680, 1.5f );

	@Override
	public ItemSprite.Glowing glowing() {
		ItemSprite.Glowing ench = super.glowing();
		return ench != null ? ench : GOLDEN;
	}

	//uses the strength requirement of a tier-1 weapon, so it can be wielded from the start
	@Override
	public int STRReq(int lvl) {
		return STRReq(1, lvl);
	}

	//Invisible Air: the blade can't be seen, so strikes are harder to avoid
	@Override
	public float accuracyFactor(Char owner, Char target) {
		return super.accuracyFactor(owner, target) * 1.25f;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_EXCALIBUR);
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_EXCALIBUR)){
			return Messages.get(this, "ac_excalibur");
		}
		return super.actionName(action, hero);
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);

		if (action.equals(AC_EXCALIBUR)){
			if (!isEquipped(hero)){
				GLog.w(Messages.get(this, "not_equipped"));
			} else if (charge < MAX_CHARGE){
				GLog.w(Messages.get(this, "not_charged", charge));
			} else {
				GameScene.selectCell(targeter);
			}
		}
	}

	@Override
	public void activate(Char ch) {
		super.activate(ch);
		Buff.affect(ch, Charger.class);
	}

	public int beamDamage(){
		return 3*Random.NormalIntRange(min(), max()) + 5 + 2*buffedLvl();
	}

	private final CellSelector.Listener targeter = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			if (cell == null) return;
			final Hero hero = Dungeon.hero;
			if (cell == hero.pos || charge < MAX_CHARGE) return;

			final Ballistica path = new Ballistica(hero.pos, cell, Ballistica.STOP_SOLID);
			hero.busy();
			hero.sprite.attack(cell, new Callback() {
				@Override
				public void call() {
					fireBeam(hero, path);
				}
			});
		}

		@Override
		public String prompt() {
			return Messages.get(Excalibur.class, "prompt");
		}
	};

	private void fireBeam(Hero hero, Ballistica path){
		charge = 0;
		partialCharge = 0;
		updateQuickslot();

		hero.sprite.parent.add(new Beam.LightRay(hero.sprite.center(),
				DungeonTilemap.raisedTileCenterToWorld(path.collisionPos)));
		Sample.INSTANCE.play(Assets.Sounds.RAY);
		Sample.INSTANCE.play(Assets.Sounds.BLAST, 0.6f);
		GLog.p(Messages.get(this, "true_name"));

		ArrayList<Char> targets = new ArrayList<>();
		for (int c : path.subPath(1, path.dist)){
			Char ch = Actor.findChar(c);
			if (ch != null && ch.alignment != Char.Alignment.ALLY){
				if (ch instanceof Mob && ((Mob) ch).state == ((Mob) ch).PASSIVE
						&& !(Dungeon.level.mapped[c] || Dungeon.level.visited[c])){
					continue; //don't harm undiscovered passive chars
				}
				targets.add(ch);
			}
			CellEmitter.center(c).burst(SparkParticle.FACTORY, 2);
		}

		for (Char ch : targets){
			ch.damage(beamDamage(), this);
			if (ch.sprite != null){
				ch.sprite.flash();
				ch.sprite.centerEmitter().burst(SparkParticle.FACTORY, 6);
			}
		}

		hero.spendAndNext(1f);
	}

	@Override
	public String status() {
		if (charge >= MAX_CHARGE) return null;
		return Messages.format("%d%%", charge);
	}

	@Override
	public String desc() {
		String desc = Messages.get(this, "desc");
		desc += "\n\n" + Messages.get(this, "charge_desc", charge);
		return desc;
	}

	private static final String CHARGE = "np_charge";
	private static final String PARTIAL = "np_partial";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(CHARGE, charge);
		bundle.put(PARTIAL, partialCharge);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		if (bundle.contains(CHARGE)) charge = bundle.getInt(CHARGE);
		partialCharge = bundle.getFloat(PARTIAL);
	}

	//recharges the noble phantasm while Excalibur is held: 100 turns at +0, faster when upgraded
	public static class Charger extends Buff {

		{
			revivePersists = true;
		}

		@Override
		public boolean act() {
			if (target instanceof Hero){
				Hero hero = (Hero) target;
				Excalibur sword = null;
				if (hero.belongings.weapon() instanceof Excalibur)          sword = (Excalibur) hero.belongings.weapon();
				else if (hero.belongings.secondWep() instanceof Excalibur)  sword = (Excalibur) hero.belongings.secondWep();

				if (sword != null && sword.charge < MAX_CHARGE){
					sword.partialCharge += 1f + 0.1f*sword.buffedLvl();
					if (sword.partialCharge >= 1){
						sword.charge = Math.min(MAX_CHARGE, sword.charge + (int)sword.partialCharge);
						sword.partialCharge -= (int)sword.partialCharge;
						updateQuickslot();
					}
				}
			}
			spend(TICK);
			return true;
		}

		@Override
		public int icon() {
			return BuffIndicator.NONE;
		}
	}
}

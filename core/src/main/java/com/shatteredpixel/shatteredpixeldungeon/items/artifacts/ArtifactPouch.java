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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndArtifactPouch;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

//A special artifact that holds rings and other artifacts.
//While the pouch is equipped, everything stored inside works as if it were equipped,
//and stored artifacts can be used from the pouch window.
public class ArtifactPouch extends Artifact {

	public static final String AC_OPEN = "OPEN";

	public static final int CAPACITY = 12;

	{
		image = ItemSpriteSheet.BACKPACK;

		levelCap = 0;
		unique = true;
		bones = false;

		levelKnown = true;
		cursedKnown = true;

		defaultAction = AC_OPEN;
	}

	public ArrayList<Item> contents = new ArrayList<>();

	private static final ItemSprite.Glowing GOLD = new ItemSprite.Glowing( 0xFFCC33 );

	@Override
	public ItemSprite.Glowing glowing() {
		return GOLD;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_OPEN );
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ) {
		super.execute( hero, action );
		if (action.equals( AC_OPEN )) {
			GameScene.show( new WndArtifactPouch( this ) );
		}
	}

	//*** equipping ***

	@Override
	public void activate( Char ch ) {
		//the pouch itself has no passive effect, it activates its contents instead
		for (Item i : contents){
			activateItem( i, ch );
		}
		refreshHero( ch );
	}

	//some ring effects (e.g. ring of might) are only recalculated when equipping/unequipping
	private static void refreshHero( Char ch ){
		if (ch instanceof Hero){
			((Hero) ch).updateHT( false );
		}
	}

	@Override
	public boolean doUnequip( Hero hero, boolean collect, boolean single ) {
		if (super.doUnequip( hero, collect, single )){
			for (Item i : contents){
				deactivateItem( i );
			}
			refreshHero( hero );
			return true;
		}
		return false;
	}

	private static void activateItem( Item i, Char ch ){
		if (i instanceof Ring)          ((Ring) i).activate( ch );
		else if (i instanceof Artifact) ((Artifact) i).activate( ch );
	}

	private static void deactivateItem( Item i ){
		if (i instanceof Ring)          ((Ring) i).deactivate();
		else if (i instanceof Artifact) ((Artifact) i).deactivate();
	}

	public boolean isActive( Hero hero ){
		return hero != null && (hero.belongings.artifact() == this || hero.belongings.misc() == this);
	}

	//*** storing items ***

	public boolean canStore( Hero hero, Item item ){
		if (!(item instanceof Ring || item instanceof Artifact)) return false;
		if (item instanceof ArtifactPouch) return false;
		if (contents.size() >= CAPACITY) return false;
		if (contents.contains( item )) return false;
		if (item instanceof Artifact && ((Artifact) item).isEquipped( hero )) return false;
		if (item instanceof Ring && ((Ring) item).isEquipped( hero )) return false;

		//artifacts can't be doubled up, same as when equipping normally
		if (item instanceof Artifact){
			if (hero.belongings.artifact != null && hero.belongings.artifact.getClass() == item.getClass()) return false;
			if (hero.belongings.misc != null && hero.belongings.misc.getClass() == item.getClass()) return false;
			for (Item i : contents){
				if (i.getClass() == item.getClass()) return false;
			}
		}
		return true;
	}

	public void store( Hero hero, Item item ){
		if (!canStore( hero, item )) return;
		item.detachAll( hero.belongings.backpack );
		contents.add( item );
		if (isActive( hero )){
			activateItem( item, hero );
			refreshHero( hero );
		}
		updateQuickslot();
	}

	public void takeOut( Hero hero, Item item ){
		if (!contents.contains( item )) return;
		deactivateItem( item );
		contents.remove( item );
		refreshHero( hero );
		if (!item.collect( hero.belongings.backpack )){
			Dungeon.level.drop( item, hero.pos ).sprite.drop();
		}
		updateQuickslot();
	}

	//*** lookups used by the rest of the game ***

	//the pouch the hero is carrying (equipped or in their pack) that holds the given item, if any
	public static ArtifactPouch findPouchHolding( Hero hero, Item item ){
		if (hero == null || hero.belongings == null || item == null) return null;
		if (hero.belongings.artifact instanceof ArtifactPouch
				&& ((ArtifactPouch) hero.belongings.artifact).contents.contains( item )){
			return (ArtifactPouch) hero.belongings.artifact;
		}
		if (hero.belongings.misc instanceof ArtifactPouch
				&& ((ArtifactPouch) hero.belongings.misc).contents.contains( item )){
			return (ArtifactPouch) hero.belongings.misc;
		}
		if (hero.belongings.backpack != null) {
			for (Item i : hero.belongings.backpack) {
				if (i instanceof ArtifactPouch && ((ArtifactPouch) i).contents.contains( item )) {
					return (ArtifactPouch) i;
				}
			}
		}
		return null;
	}

	public static boolean isInEquippedPouch( Hero hero, Item item ){
		if (hero == null || hero.belongings == null) return false;
		ArtifactPouch p = null;
		if (hero.belongings.artifact() instanceof ArtifactPouch)    p = (ArtifactPouch) hero.belongings.artifact();
		if (p != null && p.contents.contains( item )) return true;
		if (hero.belongings.misc() instanceof ArtifactPouch)        p = (ArtifactPouch) hero.belongings.misc();
		return p != null && p.contents.contains( item );
	}

	//*** misc ***

	@Override
	public void onHeroGainExp( float levelPercent, Hero hero ) {
		super.onHeroGainExp( levelPercent, hero );
		//lets stored rings identify themselves and similar exp-based effects work
		for (Item i : contents){
			i.onHeroGainExp( levelPercent, hero );
		}
	}

	@Override
	public String status() {
		return Messages.format( "%d/%d", contents.size(), CAPACITY );
	}

	@Override
	public String desc() {
		String desc = Messages.get( this, "desc", CAPACITY );
		if (isActive( Dungeon.hero )){
			desc += "\n\n" + Messages.get( this, "desc_active" );
		}
		return desc;
	}

	@Override
	public int value() {
		return 0;
	}

	private static final String CONTENTS = "contents";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( CONTENTS, contents );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		contents.clear();
		if (bundle.contains( CONTENTS )) {
			for (Bundlable b : bundle.getCollection( CONTENTS )) {
				if (b instanceof Item) contents.add( (Item) b );
			}
		}
	}
}

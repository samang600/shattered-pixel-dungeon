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

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.ArtifactPouch;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.ItemButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.ColorBlock;

public class WndArtifactPouch extends Window {

	private static final int WIDTH = 120;
	private static final int SLOT_SIZE = 22;
	private static final int COLS = 5;

	private final ArtifactPouch pouch;

	public WndArtifactPouch( final ArtifactPouch pouch ){
		super();
		this.pouch = pouch;

		IconTitle title = new IconTitle( pouch );
		title.setRect( 0, 0, WIDTH, 0 );
		add( title );

		RenderedTextBlock info = PixelScene.renderTextBlock(
				Messages.get( this, pouch.isActive( Dungeon.hero ) ? "active" : "inactive" ), 6 );
		info.maxWidth( WIDTH );
		info.setPos( 0, title.bottom() + 3 );
		add( info );

		float pos = info.bottom() + 4;

		float gap = (WIDTH - COLS*SLOT_SIZE) / (float)(COLS-1);
		for (int i = 0; i < ArtifactPouch.CAPACITY; i++){
			int col = i % COLS;
			int row = i / COLS;
			float sx = col*(SLOT_SIZE+gap);
			float sy = pos + row*(SLOT_SIZE+gap);

			if (i >= pouch.contents.size()){
				//empty slot
				ColorBlock empty = new ColorBlock( SLOT_SIZE, SLOT_SIZE, 0x9953564D );
				empty.x = sx;
				empty.y = sy;
				add( empty );
				continue;
			}

			final Item item = pouch.contents.get(i);
			ItemButton slot = new ItemButton(){
				@Override
				protected void onClick() {
					GameScene.show( new WndPouchItem( WndArtifactPouch.this, pouch, item ) );
				}
			};
			slot.item( item );
			slot.setRect( sx, sy, SLOT_SIZE, SLOT_SIZE );
			add( slot );
		}
		int rows = (ArtifactPouch.CAPACITY + COLS - 1) / COLS;
		pos += rows*(SLOT_SIZE+gap) + 2;

		RedButton btnStore = new RedButton( Messages.get( this, "store" ) ){
			@Override
			protected void onClick() {
				hide();
				GameScene.selectItem( new WndBag.ItemSelector() {
					@Override
					public String textPrompt() {
						return Messages.get( WndArtifactPouch.class, "store_prompt" );
					}

					@Override
					public boolean itemSelectable( Item item ) {
						return pouch.canStore( Dungeon.hero, item );
					}

					@Override
					public void onSelect( Item item ) {
						if (item != null){
							pouch.store( Dungeon.hero, item );
							GLog.i( Messages.get( WndArtifactPouch.class, "stored", item.name() ) );
						}
						GameScene.show( new WndArtifactPouch( pouch ) );
					}
				} );
			}
		};
		btnStore.enable( pouch.contents.size() < ArtifactPouch.CAPACITY );
		btnStore.setRect( 0, pos, WIDTH, 18 );
		add( btnStore );

		resize( WIDTH, (int)btnStore.bottom() );
	}

	//item info + actions (e.g. using an artifact), plus a button to take the item back out
	public static class WndPouchItem extends WndUseItem {

		public WndPouchItem( final WndArtifactPouch owner, final ArtifactPouch pouch, final Item item ){
			super( owner, item );

			RedButton btnTake = new RedButton( Messages.get( WndArtifactPouch.class, "take_out" ), 8 ){
				@Override
				protected void onClick() {
					hide();
					if (owner.parent != null) owner.hide();
					pouch.takeOut( Dungeon.hero, item );
					GLog.i( Messages.get( WndArtifactPouch.class, "taken", item.name() ) );
					GameScene.show( new WndArtifactPouch( pouch ) );
				}
			};
			float bw = (width - 1) / 2f;

			//quickslot assignment, since the normal quickslot picker only shows the backpack
			final boolean slotted = Dungeon.quickslot.contains( item );
			RedButton btnQuick = new RedButton( Messages.get( WndArtifactPouch.class,
					slotted ? "quickslot_remove" : "quickslot_add" ), 8 ){
				@Override
				protected void onClick() {
					hide();
					if (slotted){
						Dungeon.quickslot.clearItem( item );
						Item.updateQuickslot();
						GameScene.show( new WndArtifactPouch( pouch ) );
						if (owner.parent != null) owner.hide();
						return;
					}
					int slots = Math.max(1, QuickSlotButton.lastVisible);
					String[] options = new String[slots];
					for (int i = 0; i < slots; i++){
						Item cur = Dungeon.quickslot.getItem( i );
						options[i] = (i+1) + ": " + (cur == null ? "---" : Messages.titleCase( cur.name() ));
					}
					GameScene.show( new WndOptions( new ItemSprite( item ),
							Messages.titleCase( item.name() ),
							Messages.get( WndArtifactPouch.class, "quickslot_prompt" ),
							options ){
						@Override
						protected void onSelect( int index ) {
							QuickSlotButton.set( index, item );
							GLog.i( Messages.get( WndArtifactPouch.class, "quickslot_set", item.name(), index+1 ) );
						}
					});
				}
			};
			btnQuick.enable( slotted || item.defaultAction() != null );
			btnQuick.setRect( 0, height + 2, bw, 16 );
			add( btnQuick );

			btnTake.setRect( bw + 1, height + 2, bw, 16 );
			add( btnTake );

			resize( width, (int)btnTake.bottom() );
		}
	}
}

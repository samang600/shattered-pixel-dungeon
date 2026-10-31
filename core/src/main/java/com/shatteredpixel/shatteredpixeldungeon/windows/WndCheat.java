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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.EnergyCrystal;
import com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollingGridPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

//Cheat / editor menu, opened from the floating CheatButton in the game scene
public class WndCheat extends WndTabbed {

	private static final int WIDTH  = 144;
	private static final int HEIGHT = 190;

	private static int lastIdx = 0;

	private StatsTab stats;
	private SpawnTab spawn;
	private InventoryTab inventory;

	public WndCheat() {
		super();

		resize(WIDTH, HEIGHT);

		stats = new StatsTab();
		add(stats);
		stats.setRect(0, 0, WIDTH, HEIGHT);

		spawn = new SpawnTab();
		add(spawn);
		spawn.setRect(0, 0, WIDTH, HEIGHT);

		inventory = new InventoryTab();
		add(inventory);
		inventory.setRect(0, 0, WIDTH, HEIGHT);

		add(new IconTab(Icons.get(Icons.STATS)) {
			protected void select(boolean value) {
				super.select(value);
				if (selected) {
					lastIdx = 0;
					stats.refresh();
				}
				stats.visible = stats.active = selected;
			}
		});
		add(new IconTab(Icons.get(Icons.CATALOG)) {
			protected void select(boolean value) {
				super.select(value);
				if (selected) lastIdx = 1;
				spawn.visible = spawn.active = selected;
			}
		});
		add(new IconTab(Icons.get(Icons.BACKPACK_LRG)) {
			protected void select(boolean value) {
				super.select(value);
				if (selected) {
					lastIdx = 2;
					inventory.refresh();
				}
				inventory.visible = inventory.active = selected;
			}
		});

		layoutTabs();
		select(lastIdx);
	}

	private static void afterChange(){
		Item.updateQuickslot();
		GameScene.updateItemDisplays = true;
	}

	//*****************************
	// Number editing row
	//*****************************

	//label, current value (tap to type an exact value), and -10/-1/+1/+10 buttons
	public static abstract class NumberRow extends Component {

		public static final int HEIGHT = 16;

		private RenderedTextBlock label;
		private RedButton value;
		private RedButton[] steps;
		private static final int[] STEP_AMTS = {-10, -1, 1, 10};

		public NumberRow(String title){
			super();
			label = PixelScene.renderTextBlock(title, 6);
			add(label);
			refresh();
		}

		protected abstract int get();
		protected abstract void set(int value);

		protected int min(){ return 0; }
		protected int max(){ return 999_999; }

		private void apply(int v){
			v = Math.max(min(), Math.min(max(), v));
			set(v);
			afterChange();
			onChanged();
		}

		//called after the value is modified, so parents can refresh
		protected void onChanged(){
			refresh();
		}

		public void refresh(){
			value.text(Integer.toString(get()));
		}

		@Override
		protected void createChildren() {
			super.createChildren();

			value = new RedButton("0", 7){
				@Override
				protected void onClick() {
					GameScene.show(new WndTextInput(label.text(), null, Integer.toString(get()),
							9, false, "OK", "취소"){
						@Override
						public void onSelect(boolean positive, String text) {
							if (!positive) return;
							try {
								apply(Integer.parseInt(text.trim()));
							} catch (NumberFormatException e){
								GLog.w("숫자를 입력하세요.");
							}
						}
					});
				}
			};
			add(value);

			steps = new RedButton[STEP_AMTS.length];
			for (int i = 0; i < STEP_AMTS.length; i++){
				final int amt = STEP_AMTS[i];
				steps[i] = new RedButton(amt > 0 ? "+" + amt : Integer.toString(amt), 6){
					@Override
					protected void onClick() {
						apply(get() + amt);
					}
				};
				add(steps[i]);
			}
		}

		@Override
		protected void layout() {
			float stepW = 17;
			float stepsW = steps.length * (stepW + 1);
			float labelW = 42;
			float valueW = width - labelW - stepsW;

			label.maxWidth((int)labelW);
			label.setPos(x, y + (HEIGHT - label.height())/2f);
			PixelScene.align(label);

			value.setRect(x + labelW, y, valueW, HEIGHT);

			float px = x + labelW + valueW + 1;
			for (RedButton b : steps){
				b.setRect(px, y, stepW, HEIGHT);
				px += stepW + 1;
			}
			height = HEIGHT;
		}
	}

	//*****************************
	// Hero stats tab
	//*****************************

	private class StatsTab extends Component {

		private ArrayList<NumberRow> rows;
		private RedButton btnHeal;
		private RedButton btnIdentify;

		@Override
		protected void createChildren() {
			super.createChildren();

			//note: field initializers run after createChildren, so this must be set here
			rows = new ArrayList<>();
			rows.add(new NumberRow("체력"){
				protected int get(){ return Dungeon.hero.HP; }
				protected void set(int v){ Dungeon.hero.HP = v; }
				protected int min(){ return 1; }
				protected void onChanged(){ StatsTab.this.refresh(); }
			});
			rows.add(new NumberRow("최대 체력"){
				protected int get(){ return Dungeon.hero.HT; }
				protected void set(int v){
					Hero h = Dungeon.hero;
					h.HTBoost += v - h.HT;
					h.updateHT(false);
				}
				protected int min(){ return 1; }
				protected void onChanged(){ StatsTab.this.refresh(); }
			});
			rows.add(new NumberRow("힘"){
				protected int get(){ return Dungeon.hero.STR; }
				protected void set(int v){ Dungeon.hero.STR = v; }
				protected int max(){ return 999; }
			});
			rows.add(new NumberRow("레벨"){
				protected int get(){ return Dungeon.hero.lvl; }
				protected void set(int v){
					Hero h = Dungeon.hero;
					h.lvl = v;
					h.exp = 0;
					h.updateHT(true);
				}
				protected int min(){ return 1; }
				protected int max(){ return Hero.MAX_LEVEL; }
				protected void onChanged(){ StatsTab.this.refresh(); }
			});
			rows.add(new NumberRow("경험치"){
				protected int get(){ return Dungeon.hero.exp; }
				protected void set(int v){
					Dungeon.hero.exp = 0;
					//handles level-ups properly if enough exp is given
					Dungeon.hero.earnExp(v, Hero.class);
				}
				protected void onChanged(){ StatsTab.this.refresh(); }
			});
			rows.add(new NumberRow("골드"){
				protected int get(){ return Dungeon.gold; }
				protected void set(int v){ Dungeon.gold = v; }
				protected int max(){ return 99_999_999; }
			});
			rows.add(new NumberRow("연금 에너지"){
				protected int get(){ return Dungeon.energy; }
				protected void set(int v){ Dungeon.energy = v; }
				protected int max(){ return 99_999_999; }
			});

			for (NumberRow r : rows){
				add(r);
			}

			btnHeal = new RedButton("완전 회복", 7){
				@Override
				protected void onClick() {
					Hero h = Dungeon.hero;
					h.HP = h.HT;
					Hunger hunger = h.buff(Hunger.class);
					if (hunger != null) hunger.satisfy(Hunger.STARVING);
					h.sprite.showStatus(0x00FF00, "회복!");
					afterChange();
					StatsTab.this.refresh();
				}
			};
			add(btnHeal);

			btnIdentify = new RedButton("전체 감정", 7){
				@Override
				protected void onClick() {
					for (Item i : Dungeon.hero.belongings){
						i.identify();
					}
					GLog.p("소지품을 모두 감정했습니다.");
					afterChange();
				}
			};
			add(btnIdentify);
		}

		public void refresh(){
			for (NumberRow r : rows){
				r.refresh();
			}
		}

		@Override
		protected void layout() {
			float pos = y + 2;
			for (NumberRow r : rows){
				r.setRect(x, pos, width, NumberRow.HEIGHT);
				pos += NumberRow.HEIGHT + 2;
			}
			pos += 4;
			float bw = (width - 2) / 2f;
			btnHeal.setRect(x, pos, bw, 18);
			btnIdentify.setRect(x + bw + 2, pos, bw, 18);
		}
	}

	//*****************************
	// Item spawning tab
	//*****************************

	private class SpawnTab extends Component {

		private ScrollingGridPane grid;

		@Override
		protected void createChildren() {
			super.createChildren();
			grid = new ScrollingGridPane();
			add(grid);

			grid.addHeader("_아이템 생성_", 9, true);
			grid.addHeader("아이템을 눌러 개수/강화 수치를 정하고 지급합니다.", 6, true);

			ArrayList<Catalog> cats = new ArrayList<>(Catalog.equipmentCatalogs);
			cats.addAll(Catalog.consumableCatalogs);
			for (Catalog cat : cats){
				//enchantments and glyphs are not items
				if (cat == Catalog.ENCHANTMENTS || cat == Catalog.GLYPHS) continue;

				grid.addHeader(cat.title());
				for (Class<?> cls : cat.items()){
					if (!Item.class.isAssignableFrom(cls)) continue;
					final Item sample;
					try {
						sample = (Item) Reflection.newInstanceUnhandled(cls);
					} catch (Exception e){
						continue;
					}
					grid.addItem(new ScrollingGridPane.GridItem(new ItemSprite(sample)){
						@Override
						public boolean onClick(float x, float y) {
							if (inside(x, y)){
								GameScene.show(new WndSpawnItem(cls));
								return true;
							}
							return false;
						}
					});
				}
			}
		}

		@Override
		protected void layout() {
			super.layout();
			grid.setRect(x, y, width, height);
		}
	}

	public static class WndSpawnItem extends Window {

		private static final int WIDTH = 140;

		private int quantity = 1;
		private int level = 0;

		public WndSpawnItem(final Class<?> cls){
			super();

			final Item preview = (Item) Reflection.newInstance(cls);

			IconTitle title = new IconTitle(preview);
			title.setRect(0, 0, WIDTH, 0);
			add(title);

			float pos = title.bottom() + 4;

			if (preview.stackable || preview instanceof Gold || preview instanceof EnergyCrystal){
				NumberRow qty = new NumberRow("개수"){
					protected int get(){ return quantity; }
					protected void set(int v){ quantity = v; }
					protected int min(){ return 1; }
				};
				qty.setRect(0, pos, WIDTH, NumberRow.HEIGHT);
				add(qty);
				pos = qty.bottom() + 2;
			}

			if (preview.isUpgradable()){
				NumberRow lvl = new NumberRow("강화"){
					protected int get(){ return level; }
					protected void set(int v){ level = v; }
					protected int min(){ return -10; }
					protected int max(){ return 999; }
				};
				lvl.setRect(0, pos, WIDTH, NumberRow.HEIGHT);
				add(lvl);
				pos = lvl.bottom() + 2;
			}

			RedButton give = new RedButton("지급하기"){
				@Override
				protected void onClick() {
					giveItem(cls, quantity, level);
				}
			};
			give.setRect(0, pos + 2, WIDTH, 18);
			add(give);

			resize(WIDTH, (int)give.bottom());
		}

		private static void giveItem(Class<?> cls, int quantity, int level){
			Hero hero = Dungeon.hero;
			Item item = (Item) Reflection.newInstance(cls);
			if (item == null) return;

			if (item instanceof Gold){
				Dungeon.gold += quantity;
				GLog.p("골드 %d 획득", quantity);
				afterChange();
				return;
			} else if (item instanceof EnergyCrystal){
				Dungeon.energy += quantity;
				GLog.p("연금 에너지 %d 획득", quantity);
				afterChange();
				return;
			}

			if (item.stackable){
				item.quantity(quantity);
			}
			if (item.isUpgradable() && level != 0){
				item.level(level);
			}
			item.identify(false);
			item.cursed = false;

			if (!item.collect()){
				Dungeon.level.drop(item, hero.pos).sprite.drop();
				GLog.w("가방이 가득 차서 바닥에 떨어졌습니다.");
			}
			GLog.p("%s 획득", item.name());
			afterChange();
		}
	}

	//*****************************
	// Inventory editing tab
	//*****************************

	private class InventoryTab extends Component {

		private ScrollingGridPane grid;

		@Override
		protected void createChildren() {
			super.createChildren();
			grid = new ScrollingGridPane();
			add(grid);
		}

		public void refresh(){
			grid.clear();
			grid.addHeader("_소지품 편집_", 9, true);
			grid.addHeader("아이템을 눌러 개수, 강화, 저주 등을 수정합니다.", 6, true);

			Hero hero = Dungeon.hero;
			ArrayList<Item> equipped = new ArrayList<>();
			if (hero.belongings.weapon != null)     equipped.add(hero.belongings.weapon);
			if (hero.belongings.armor != null)      equipped.add(hero.belongings.armor);
			if (hero.belongings.artifact != null)   equipped.add(hero.belongings.artifact);
			if (hero.belongings.misc != null)       equipped.add(hero.belongings.misc);
			if (hero.belongings.ring != null)       equipped.add(hero.belongings.ring);
			if (hero.belongings.secondWep != null)  equipped.add(hero.belongings.secondWep);

			if (!equipped.isEmpty()){
				grid.addHeader("장착 중");
				for (Item i : equipped) addItem(i);
			}

			addBag(hero.belongings.backpack, "가방");
			grid.setRect(x, y, width, height);
		}

		private void addBag(Bag bag, String title){
			grid.addHeader(title);
			ArrayList<Bag> subBags = new ArrayList<>();
			for (Item i : bag.items){
				if (i instanceof Bag){
					subBags.add((Bag) i);
				} else {
					addItem(i);
				}
			}
			for (Bag b : subBags){
				addBag(b, Messages.titleCase(b.name()));
			}
		}

		private void addItem(final Item item){
			ScrollingGridPane.GridItem gi = new ScrollingGridPane.GridItem(new ItemSprite(item)){
				@Override
				public boolean onClick(float x, float y) {
					if (inside(x, y)){
						GameScene.show(new WndEditItem(item){
							@Override
							public void hide() {
								super.hide();
								InventoryTab.this.refresh();
							}
						});
						return true;
					}
					return false;
				}
			};
			String badge = null;
			if (item.quantity() > 1){
				badge = Integer.toString(item.quantity());
			} else if (item.isUpgradable() && item.level() != 0){
				badge = (item.level() > 0 ? "+" : "") + item.level();
			}
			if (badge != null){
				BitmapText txt = new BitmapText(badge, PixelScene.pixelFont);
				txt.measure();
				gi.addSecondIcon(txt);
			}
			grid.addItem(gi);
		}

		@Override
		protected void layout() {
			super.layout();
			grid.setRect(x, y, width, height);
		}
	}

	public static class WndEditItem extends Window {

		private static final int WIDTH = 140;

		private final Item item;
		private IconTitle title;

		public WndEditItem(final Item item){
			super();
			this.item = item;

			title = new IconTitle(item);
			title.setRect(0, 0, WIDTH, 0);
			add(title);

			float pos = title.bottom() + 4;

			if (item.stackable){
				NumberRow qty = new NumberRow("개수"){
					protected int get(){ return item.quantity(); }
					protected void set(int v){ item.quantity(v); }
					protected int min(){ return 1; }
					protected void onChanged(){ super.onChanged(); updateTitle(); }
				};
				qty.setRect(0, pos, WIDTH, NumberRow.HEIGHT);
				add(qty);
				pos = qty.bottom() + 2;
			}

			if (item.isUpgradable()){
				NumberRow lvl = new NumberRow("강화"){
					protected int get(){ return item.level(); }
					protected void set(int v){
						item.level(v);
						item.levelKnown = true;
					}
					protected int min(){ return -10; }
					protected int max(){ return 999; }
					protected void onChanged(){ super.onChanged(); updateTitle(); }
				};
				lvl.setRect(0, pos, WIDTH, NumberRow.HEIGHT);
				add(lvl);
				pos = lvl.bottom() + 2;
			}

			float bw = (WIDTH - 2) / 2f;

			RedButton identify = new RedButton("감정", 7){
				@Override
				protected void onClick() {
					item.identify();
					afterChange();
					updateTitle();
				}
			};
			identify.setRect(0, pos + 2, bw, 18);
			add(identify);

			RedButton curse = new RedButton(item.cursed ? "저주 해제" : "저주 걸기", 7){
				@Override
				protected void onClick() {
					item.cursed = !item.cursed;
					item.cursedKnown = true;
					text(item.cursed ? "저주 해제" : "저주 걸기");
					afterChange();
					updateTitle();
				}
			};
			curse.setRect(bw + 2, pos + 2, bw, 18);
			add(curse);

			pos = curse.bottom() + 2;

			RedButton delete = new RedButton("삭제", 7){
				@Override
				protected void onClick() {
					deleteItem(item);
					WndEditItem.this.hide();
				}
			};
			delete.setRect(0, pos, WIDTH, 18);
			add(delete);

			resize(WIDTH, (int)delete.bottom());
		}

		private void updateTitle(){
			title.icon(new ItemSprite(item));
			title.label(item.title());
		}

		private static void deleteItem(Item item){
			Hero hero = Dungeon.hero;
			if (item instanceof EquipableItem && ((EquipableItem) item).isEquipped(hero)){
				item.cursed = false;
				((EquipableItem) item).doUnequip(hero, false, false);
			} else {
				item.detachAll(hero.belongings.backpack);
			}
			GLog.i("%s 삭제됨", item.name());
			afterChange();
		}
	}
}

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
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.ArtifactPouch;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.Trinket;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
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
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollingListPane;
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

	private static String txt(String key, Object... args){
		return Messages.get(WndCheat.class, key, args);
	}

	//*****************************
	// Level / enchantment helpers
	//*****************************

	//artifacts, trinkets and the spirit bow can't normally be upgraded with scrolls, but can be edited here
	public static boolean canEditLevel(Item i){
		return i.isUpgradable() || (i instanceof Artifact && ((Artifact) i).levelCap() > 0)
				|| i instanceof Trinket || i instanceof SpiritBow;
	}

	public static int minLevel(Item i){
		if (i instanceof Artifact || i instanceof Trinket) return 0;
		if (i instanceof SpiritBow) return i.level() - ((SpiritBow) i).bonusLevel;
		return -10;
	}

	public static int maxLevel(Item i){
		if (i instanceof Artifact) return ((Artifact) i).levelCap();
		if (i instanceof Trinket) return 3;
		return 999;
	}

	public static String levelLabel(Item i){
		if (i instanceof Artifact || i instanceof Trinket){
			return txt("upgrade_max", maxLevel(i));
		}
		return txt("upgrade");
	}

	public static void setLevel(Item i, int v){
		if (i instanceof SpiritBow){
			SpiritBow bow = (SpiritBow) i;
			bow.bonusLevel += v - bow.level();
		} else if (i instanceof Artifact){
			//artifacts update charge caps and such in upgrade(), so step up one level at a time
			int cur = i.level();
			if (v > cur){
				for (int n = cur; n < v; n++) i.upgrade();
			} else {
				i.level(v);
			}
		} else {
			i.level(v);
			if (i instanceof MagesStaff){
				((MagesStaff) i).updateWand(false);
			}
		}
		i.levelKnown = true;
	}

	public static boolean canEnchant(Item i){
		return i instanceof Weapon || i instanceof Armor;
	}

	public static Class<?> currentEnchant(Item i){
		if (i instanceof Weapon && ((Weapon) i).enchantment != null) return ((Weapon) i).enchantment.getClass();
		if (i instanceof Armor && ((Armor) i).glyph != null) return ((Armor) i).glyph.getClass();
		return null;
	}

	//applies an enchantment (weapons) or glyph (armor). Returns true if it is a curse.
	public static boolean applyEnchant(Item i, Class<?> cls){
		if (i instanceof Weapon){
			Weapon.Enchantment e = cls == null ? null : (Weapon.Enchantment) Reflection.newInstance(cls);
			((Weapon) i).enchant(e);
			return e != null && e.curse();
		} else if (i instanceof Armor){
			Armor.Glyph g = cls == null ? null : (Armor.Glyph) Reflection.newInstance(cls);
			((Armor) i).inscribe(g);
			return g != null && g.curse();
		}
		return false;
	}

	public static String enchantName(Class<?> cls){
		if (cls == null) return txt("none");
		Object o = Reflection.newInstance(cls);
		if (o instanceof Weapon.Enchantment) return Messages.titleCase(((Weapon.Enchantment) o).name());
		if (o instanceof Armor.Glyph) return Messages.titleCase(((Armor.Glyph) o).name());
		return cls.getSimpleName();
	}

	public static String enchantButtonText(Item i, Class<?> cls){
		return txt(i instanceof Armor ? "glyph_btn" : "enchant_btn", enchantName(cls));
	}

	//scrolling list of every enchantment (for weapons) or glyph (for armor), including curses
	public static abstract class WndEnchantPicker extends Window {

		private static final int WIDTH = 130;
		private static final int HEIGHT = 160;
		private static final int ITEM_HEIGHT = 16;

		public WndEnchantPicker(Item item){
			super();
			boolean armor = item instanceof Armor;

			RenderedTextBlock title = PixelScene.renderTextBlock(txt(armor ? "pick_glyph" : "pick_enchant"), 9);
			title.hardlight(TITLE_COLOR);
			title.setPos((WIDTH - title.width())/2f, 2);
			add(title);

			ScrollingListPane list = new ScrollingListPane();
			add(list);

			addEntry(list, item, null);

			Class<?>[][] tiers = armor
					? new Class<?>[][]{Armor.Glyph.common, Armor.Glyph.uncommon, Armor.Glyph.rare, Armor.Glyph.curses}
					: new Class<?>[][]{Weapon.Enchantment.common, Weapon.Enchantment.uncommon, Weapon.Enchantment.rare, Weapon.Enchantment.curses};
			String[] tierNames = {"tier_common", "tier_uncommon", "tier_rare", "tier_curse"};
			for (int t = 0; t < tiers.length; t++){
				list.addTitle(txt(tierNames[t]));
				for (Class<?> cls : tiers[t]){
					addEntry(list, item, cls);
				}
			}

			float top = title.bottom() + 3;
			resize(WIDTH, HEIGHT);
			list.setRect(0, top, WIDTH, HEIGHT - top);
		}

		private void addEntry(ScrollingListPane list, Item item, final Class<?> cls){
			ItemSprite icon = new ItemSprite(item.image());
			ScrollingListPane.ListItem entry = new ScrollingListPane.ListItem(icon, enchantName(cls)){
				@Override
				public boolean onClick(float x, float y) {
					if (inside(x, y)){
						onPick(cls);
						WndEnchantPicker.this.hide();
						return true;
					}
					return false;
				}
			};
			if (cls != null){
				Object o = Reflection.newInstance(cls);
				ItemSprite.Glowing glow = null;
				if (o instanceof Weapon.Enchantment) glow = ((Weapon.Enchantment) o).glowing();
				if (o instanceof Armor.Glyph) glow = ((Armor.Glyph) o).glowing();
				if (glow != null) entry.hardlightIcon(glow.color);
			}
			if (cls == currentEnchant(item)) entry.hardlight(TITLE_COLOR);
			entry.setSize(WIDTH, ITEM_HEIGHT);
			list.addItem(entry);
		}

		protected abstract void onPick(Class<?> cls);
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
							9, false, "OK", txt("cancel")){
						@Override
						public void onSelect(boolean positive, String text) {
							if (!positive) return;
							try {
								apply(Integer.parseInt(text.trim()));
							} catch (NumberFormatException e){
								GLog.w(txt("not_number"));
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
			rows.add(new NumberRow(txt("hp")){
				protected int get(){ return Dungeon.hero.HP; }
				protected void set(int v){ Dungeon.hero.HP = v; }
				protected int min(){ return 1; }
				protected void onChanged(){ StatsTab.this.refresh(); }
			});
			rows.add(new NumberRow(txt("max_hp")){
				protected int get(){ return Dungeon.hero.HT; }
				protected void set(int v){
					Hero h = Dungeon.hero;
					h.HTBoost += v - h.HT;
					h.updateHT(false);
				}
				protected int min(){ return 1; }
				protected void onChanged(){ StatsTab.this.refresh(); }
			});
			rows.add(new NumberRow(txt("str")){
				protected int get(){ return Dungeon.hero.STR; }
				protected void set(int v){ Dungeon.hero.STR = v; }
				protected int max(){ return 999; }
			});
			rows.add(new NumberRow(txt("level")){
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
			rows.add(new NumberRow(txt("exp")){
				protected int get(){ return Dungeon.hero.exp; }
				protected void set(int v){
					Dungeon.hero.exp = 0;
					//handles level-ups properly if enough exp is given
					Dungeon.hero.earnExp(v, Hero.class);
				}
				protected void onChanged(){ StatsTab.this.refresh(); }
			});
			rows.add(new NumberRow(txt("gold")){
				protected int get(){ return Dungeon.gold; }
				protected void set(int v){ Dungeon.gold = v; }
				protected int max(){ return 99_999_999; }
			});
			rows.add(new NumberRow(txt("energy")){
				protected int get(){ return Dungeon.energy; }
				protected void set(int v){ Dungeon.energy = v; }
				protected int max(){ return 99_999_999; }
			});

			for (NumberRow r : rows){
				add(r);
			}

			btnHeal = new RedButton(txt("heal"), 7){
				@Override
				protected void onClick() {
					Hero h = Dungeon.hero;
					h.HP = h.HT;
					Hunger hunger = h.buff(Hunger.class);
					if (hunger != null) hunger.satisfy(Hunger.STARVING);
					h.sprite.showStatus(0x00FF00, txt("healed"));
					afterChange();
					StatsTab.this.refresh();
				}
			};
			add(btnHeal);

			btnIdentify = new RedButton(txt("identify_all"), 7){
				@Override
				protected void onClick() {
					for (Item i : Dungeon.hero.belongings){
						i.identify();
					}
					GLog.p(txt("identified_all"));
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

			grid.addHeader("_" + txt("spawn_title") + "_", 9, true);
			grid.addHeader(txt("spawn_desc"), 6, true);

			grid.addHeader(txt("special"));
			addSpawnItem(ArtifactPouch.class);

			ArrayList<Catalog> cats = new ArrayList<>(Catalog.equipmentCatalogs);
			cats.addAll(Catalog.consumableCatalogs);
			for (Catalog cat : cats){
				//enchantments and glyphs are not items
				if (cat == Catalog.ENCHANTMENTS || cat == Catalog.GLYPHS) continue;

				grid.addHeader(cat.title());
				for (Class<?> cls : cat.items()){
					addSpawnItem(cls);
				}
			}
		}

		private void addSpawnItem(final Class<?> cls){
			if (!Item.class.isAssignableFrom(cls)) return;
			final Item sample;
			try {
				sample = (Item) Reflection.newInstanceUnhandled(cls);
			} catch (Exception e){
				return;
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
		private Class<?> enchant = null;

		public WndSpawnItem(final Class<?> cls){
			super();

			final Item preview = (Item) Reflection.newInstance(cls);

			IconTitle title = new IconTitle(preview);
			title.setRect(0, 0, WIDTH, 0);
			add(title);

			float pos = title.bottom() + 4;

			if (preview.stackable || preview instanceof Gold || preview instanceof EnergyCrystal){
				NumberRow qty = new NumberRow(txt("quantity")){
					protected int get(){ return quantity; }
					protected void set(int v){ quantity = v; }
					protected int min(){ return 1; }
				};
				qty.setRect(0, pos, WIDTH, NumberRow.HEIGHT);
				add(qty);
				pos = qty.bottom() + 2;
			}

			if (canEditLevel(preview)){
				final int baseLevel = preview.level();
				level = baseLevel;
				NumberRow lvl = new NumberRow(levelLabel(preview)){
					protected int get(){ return level; }
					protected void set(int v){ level = v; }
					protected int min(){ return minLevel(preview); }
					protected int max(){ return maxLevel(preview); }
				};
				lvl.setRect(0, pos, WIDTH, NumberRow.HEIGHT);
				add(lvl);
				pos = lvl.bottom() + 2;
			}

			if (canEnchant(preview)){
				RedButton ench = new RedButton(enchantButtonText(preview, null), 7){
					@Override
					protected void onClick() {
						final RedButton btn = this;
						GameScene.show(new WndEnchantPicker(preview){
							@Override
							protected void onPick(Class<?> cls) {
								enchant = cls;
								btn.text(enchantButtonText(preview, cls));
							}
						});
					}
				};
				ench.setRect(0, pos + 2, WIDTH, 18);
				add(ench);
				pos = ench.bottom();
			}

			RedButton give = new RedButton(txt("give")){
				@Override
				protected void onClick() {
					giveItem(cls, quantity, level, enchant);
				}
			};
			give.setRect(0, pos + 2, WIDTH, 18);
			add(give);

			resize(WIDTH, (int)give.bottom());
		}

		private static void giveItem(Class<?> cls, int quantity, int level, Class<?> enchant){
			Hero hero = Dungeon.hero;
			Item item = (Item) Reflection.newInstance(cls);
			if (item == null) return;

			if (item instanceof Gold){
				Dungeon.gold += quantity;
				GLog.p(txt("got_gold", quantity));
				afterChange();
				return;
			} else if (item instanceof EnergyCrystal){
				Dungeon.energy += quantity;
				GLog.p(txt("got_energy", quantity));
				afterChange();
				return;
			}

			if (item.stackable){
				item.quantity(quantity);
			}
			if (canEditLevel(item) && level != item.level()){
				setLevel(item, level);
			}
			item.identify(false);
			item.cursed = false;
			if (item instanceof Artifact && ((Artifact) item).chargeCap() > 0){
				((Artifact) item).setCharge(((Artifact) item).chargeCap());
			}
			if (enchant != null && canEnchant(item)){
				item.cursed = applyEnchant(item, enchant);
			}

			if (!item.collect()){
				Dungeon.level.drop(item, hero.pos).sprite.drop();
				GLog.w(txt("bag_full"));
			}
			GLog.p(txt("got_item", item.name()));
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
			grid.addHeader("_" + txt("inv_title") + "_", 9, true);
			grid.addHeader(txt("inv_desc"), 6, true);

			Hero hero = Dungeon.hero;
			ArrayList<Item> equipped = new ArrayList<>();
			if (hero.belongings.weapon != null)     equipped.add(hero.belongings.weapon);
			if (hero.belongings.armor != null)      equipped.add(hero.belongings.armor);
			if (hero.belongings.artifact != null)   equipped.add(hero.belongings.artifact);
			if (hero.belongings.misc != null)       equipped.add(hero.belongings.misc);
			if (hero.belongings.ring != null)       equipped.add(hero.belongings.ring);
			if (hero.belongings.secondWep != null)  equipped.add(hero.belongings.secondWep);

			if (!equipped.isEmpty()){
				grid.addHeader(txt("equipped"));
				for (Item i : equipped) addItem(i);
			}

			addBag(hero.belongings.backpack, txt("backpack"));

			for (Item i : hero.belongings){
				if (i instanceof ArtifactPouch && !((ArtifactPouch) i).contents.isEmpty()){
					grid.addHeader(Messages.titleCase(i.name()));
					for (Item c : ((ArtifactPouch) i).contents) addItem(c);
				}
			}
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
			} else if (canEditLevel(item) && item.level() != 0){
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
		private RedButton curseBtn;

		public WndEditItem(final Item item){
			super();
			this.item = item;

			title = new IconTitle(item);
			title.setRect(0, 0, WIDTH, 0);
			add(title);

			float pos = title.bottom() + 4;

			if (item.stackable){
				NumberRow qty = new NumberRow(txt("quantity")){
					protected int get(){ return item.quantity(); }
					protected void set(int v){ item.quantity(v); }
					protected int min(){ return 1; }
					protected void onChanged(){ super.onChanged(); updateTitle(); }
				};
				qty.setRect(0, pos, WIDTH, NumberRow.HEIGHT);
				add(qty);
				pos = qty.bottom() + 2;
			}

			if (canEditLevel(item)){
				NumberRow lvl = new NumberRow(levelLabel(item)){
					protected int get(){ return item.level(); }
					protected void set(int v){ setLevel(item, v); }
					protected int min(){ return minLevel(item); }
					protected int max(){ return maxLevel(item); }
					protected void onChanged(){ super.onChanged(); updateTitle(); }
				};
				lvl.setRect(0, pos, WIDTH, NumberRow.HEIGHT);
				add(lvl);
				pos = lvl.bottom() + 2;
			}

			if (item instanceof Artifact && ((Artifact) item).chargeCap() > 0){
				final Artifact art = (Artifact) item;
				NumberRow chg = new NumberRow(txt("charge_max", art.chargeCap())){
					protected int get(){ return art.getCharge(); }
					protected void set(int v){ art.setCharge(v); }
					protected int max(){ return art.chargeCap(); }
					protected void onChanged(){ super.onChanged(); updateTitle(); }
				};
				chg.setRect(0, pos, WIDTH, NumberRow.HEIGHT);
				add(chg);
				pos = chg.bottom() + 2;
			}

			if (item instanceof Wand){
				final Wand wand = (Wand) item;
				NumberRow chg = new NumberRow(txt("charge")){
					protected int get(){ return wand.curCharges; }
					protected void set(int v){ wand.curCharges = v; }
					protected int max(){ return 99; }
					protected void onChanged(){ super.onChanged(); updateTitle(); }
				};
				chg.setRect(0, pos, WIDTH, NumberRow.HEIGHT);
				add(chg);
				pos = chg.bottom() + 2;
			}

			if (canEnchant(item)){
				RedButton ench = new RedButton(enchantButtonText(item, currentEnchant(item)), 7){
					@Override
					protected void onClick() {
						final RedButton btn = this;
						GameScene.show(new WndEnchantPicker(item){
							@Override
							protected void onPick(Class<?> cls) {
								boolean curse = applyEnchant(item, cls);
								if (curse){
									item.cursed = true;
									item.cursedKnown = true;
								}
								btn.text(enchantButtonText(item, cls));
								if (curseBtn != null) curseBtn.text(item.cursed ? txt("uncurse") : txt("curse"));
								afterChange();
								updateTitle();
							}
						});
					}
				};
				ench.setRect(0, pos + 2, WIDTH, 18);
				add(ench);
				pos = ench.bottom();
			}

			float bw = (WIDTH - 2) / 2f;

			RedButton identify = new RedButton(txt("identify"), 7){
				@Override
				protected void onClick() {
					item.identify();
					afterChange();
					updateTitle();
				}
			};
			identify.setRect(0, pos + 2, bw, 18);
			add(identify);

			RedButton curse = new RedButton(item.cursed ? txt("uncurse") : txt("curse"), 7){
				@Override
				protected void onClick() {
					item.cursed = !item.cursed;
					item.cursedKnown = true;
					text(item.cursed ? txt("uncurse") : txt("curse"));
					afterChange();
					updateTitle();
				}
			};
			curse.setRect(bw + 2, pos + 2, bw, 18);
			add(curse);
			curseBtn = curse;

			pos = curse.bottom() + 2;

			RedButton delete = new RedButton(txt("delete"), 7){
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
			ArtifactPouch holder = ArtifactPouch.findPouchHolding(hero, item);
			if (holder != null){
				holder.takeOut(hero, item);
				item.detachAll(hero.belongings.backpack);
			} else if (item instanceof EquipableItem && ((EquipableItem) item).isEquipped(hero)){
				item.cursed = false;
				((EquipableItem) item).doUnequip(hero, false, false);
			} else {
				item.detachAll(hero.belongings.backpack);
			}
			GLog.i(txt("deleted", item.name()));
			afterChange();
		}
	}
}

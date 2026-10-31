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

package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndCheat;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.PointerArea;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.PointF;

//A small floating button that can be dragged anywhere on screen. Tapping it opens the cheat menu.
public class CheatButton extends Component {

	public static final int SIZE = 20;

	//remembered between scene reloads (e.g. changing floors)
	private static float savedX = -1;
	private static float savedY = -1;

	private NinePatch bg;
	private Image icon;
	private PointerArea hotArea;

	private boolean dragging = false;
	private PointF grabOffset = new PointF();

	@Override
	protected void createChildren() {
		bg = Chrome.get(Chrome.Type.RED_BUTTON);
		add(bg);

		icon = Icons.get(Icons.PREFS);
		add(icon);

		hotArea = new PointerArea(0, 0, 0, 0){
			@Override
			protected void onPointerDown(PointerEvent event) {
				dragging = false;
				PointF p = camera().screenToCamera((int)event.current.x, (int)event.current.y);
				grabOffset.set(p.x - CheatButton.this.x, p.y - CheatButton.this.y);
				bg.brightness(1.2f);
			}

			@Override
			protected void onPointerUp(PointerEvent event) {
				bg.resetColor();
			}

			@Override
			protected void onDrag(PointerEvent event) {
				if (!dragging && PointF.distance(event.current, event.start) > PixelScene.defaultZoom * 4){
					dragging = true;
				}
				if (dragging) {
					PointF p = camera().screenToCamera((int)event.current.x, (int)event.current.y);
					moveTo(p.x - grabOffset.x, p.y - grabOffset.y);
				}
			}

			@Override
			protected void onClick(PointerEvent event) {
				if (dragging){
					dragging = false;
					return;
				}
				if (Dungeon.hero == null || !Dungeon.hero.isAlive()) return;
				Sample.INSTANCE.play(Assets.Sounds.CLICK);
				GameScene.show(new WndCheat());
			}
		};
		add(hotArea);

		width = height = SIZE;
	}

	//places the button at its remembered spot, or a default one along the right edge
	public void placeDefault(){
		if (savedX < 0 || savedY < 0){
			moveTo(camera().width - SIZE - 2, camera().height * 0.35f);
		} else {
			moveTo(savedX, savedY);
		}
	}

	private void moveTo(float nx, float ny){
		nx = Math.max(0, Math.min(nx, camera().width - SIZE));
		ny = Math.max(0, Math.min(ny, camera().height - SIZE));
		savedX = nx;
		savedY = ny;
		setPos(Math.round(nx), Math.round(ny));
	}

	@Override
	public void update() {
		super.update();
		//keep it accessible above most other UI elements, but behind windows
		alpha(dragging ? 1f : 0.85f);
	}

	private void alpha(float a){
		bg.alpha(a);
		icon.alpha(a);
	}

	@Override
	protected void layout() {
		bg.x = x;
		bg.y = y;
		bg.size(width, height);

		icon.x = x + (width - icon.width()) / 2f;
		icon.y = y + (height - icon.height()) / 2f;
		PixelScene.align(icon);

		hotArea.x = x;
		hotArea.y = y;
		hotArea.width = width;
		hotArea.height = height;
	}
}

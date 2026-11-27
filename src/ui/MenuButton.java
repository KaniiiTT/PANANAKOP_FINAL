package ui;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import gamestates.Gamestate;
import utilz.LoadSave;

public class MenuButton {
	private int xPos, yPos, index, width, height;
	private Gamestate state;
	private BufferedImage[] imgs;
	private boolean mouseOver, mousePressed;
	private Rectangle bounds;

	public MenuButton(int xPos, int yPos, String assetName, Gamestate state, float scale) {
		this.xPos = xPos;
		this.yPos = yPos;
		this.state = state;
		loadImgs(assetName);
		width = Math.round(imgs[0].getWidth() * scale);
		height = Math.round(imgs[0].getHeight() * scale);
		initBounds();
	}

	private void initBounds() {
		bounds = new Rectangle(xPos - width / 2, yPos, width, height);

	}

	private void loadImgs(String assetName) {
		imgs = new BufferedImage[3];
		String[] states = { "normal", "hover", "pressed" };
		for (int i = 0; i < imgs.length; i++)
			imgs[i] = LoadSave.GetSpriteAtlas("menu/buttons/btn_" + assetName + "_" + states[i] + ".png");
	}

	public void draw(Graphics g) {
		g.drawImage(imgs[index], bounds.x, bounds.y, width, height, null);
	}

	public void update() {
		index = 0;
		if (mouseOver)
			index = 1;
		if (mousePressed)
			index = 2;
	}

	public boolean isMouseOver() {
		return mouseOver;
	}

	public void setMouseOver(boolean mouseOver) {
		this.mouseOver = mouseOver;
	}

	public boolean isMousePressed() {
		return mousePressed;
	}

	public void setMousePressed(boolean mousePressed) {
		this.mousePressed = mousePressed;
	}

	public Rectangle getBounds() {
		return bounds;
	}
 
	public void applyGamestate() {
		Gamestate.state = state;
	}

	public void resetBools() {
		mouseOver = false;
		mousePressed = false;
	}

}

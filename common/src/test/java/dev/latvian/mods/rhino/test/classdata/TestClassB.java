package dev.latvian.mods.rhino.test.classdata;

public class TestClassB extends TestClassA {
	@Override
	public double getPosX() {
		return pos.x * 2D;
	}

	@Override
	public double getPosZ() {
		return pos.z;
	}

	@Override
	public void say(String text) {
		System.out.println("Hello " + text);
	}

	@Override
	public int getStatus() {
		return 0;
	}
}

package dev.latvian.mods.rhino.test.classdata;

import dev.latvian.mods.rhino.util.HideFromJS;
import dev.latvian.mods.rhino.util.RemapForJS;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

public abstract class TestClassA implements TestInterfaceB {
	public Vec3 pos = new Vec3(30, 10, 50);
	private final int age = 0;
	public transient Map<String, Object> customProperties = new HashMap<>();

	@RemapForJS("getX")
	public double getPosX() {
		return pos.x;
	}

	@RemapForJS("getY")
	public double getPosY() {
		return pos.y;
	}

	@HideFromJS
	public double getPosZ() {
		return pos.z;
	}
}

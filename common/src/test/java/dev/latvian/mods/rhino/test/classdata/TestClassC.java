package dev.latvian.mods.rhino.test.classdata;

import net.minecraft.nbt.CompoundTag;

public class TestClassC extends TestClassB implements TestInterfaceC {
	@Override
	public void rhino$setStatus(int status) {
		System.out.println("Status: " + status);
	}

	@Override
	public CompoundTag merge(CompoundTag tag) {
		return new CompoundTag().merge(tag);
	}
}

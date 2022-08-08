package dev.latvian.mods.rhino.test.classdata;

import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import net.minecraft.nbt.CompoundTag;

@RemapPrefixForJS("rhino$")
public interface TestInterfaceC {
	void rhino$setStatus(int status);

	CompoundTag merge(CompoundTag tag);
}

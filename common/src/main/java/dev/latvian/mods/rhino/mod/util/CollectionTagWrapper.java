package dev.latvian.mods.rhino.mod.util;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.util.CustomJavaToJsWrapper;
import net.minecraft.nbt.CollectionTag;

public record CollectionTagWrapper(CollectionTag<?> tag) implements CustomJavaToJsWrapper {
	@Override
	public Scriptable convertJavaToJs(Context cx, Scriptable scope, Class<?> staticType) {
		// FIXME: return new NativeJavaList(scope, tag, tag, Tag.class, NBTUtils.VALUE_UNWRAPPER);
		return null;
	}
}

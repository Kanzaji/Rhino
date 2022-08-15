package dev.latvian.mods.rhino.classdata;

import dev.latvian.mods.rhino.ContextJS;
import dev.latvian.mods.rhino.js.prototype.CastType;
import dev.latvian.mods.rhino.js.prototype.MemberFunctions;
import org.jetbrains.annotations.Nullable;

public record DelegatedMemberFunctions(MemberFunctions parent, Object delegateTo) implements MemberFunctions {
	@Override
	public Object getValue(ContextJS cx, @Nullable Object self, Object key, CastType returnType) {
		return parent.getValue(cx, delegateTo, key, returnType);
	}

	@Override
	public boolean hasValue(ContextJS cx, @Nullable Object self, Object key) {
		return parent.hasValue(cx, delegateTo, key);
	}

	@Override
	public boolean setValue(ContextJS cx, @Nullable Object self, Object key, Object value, CastType valueType) {
		return parent.setValue(cx, delegateTo, key, value, valueType);
	}

	@Override
	public Object invoke(ContextJS cx, @Nullable Object self, Object key, Object[] args, CastType returnType) {
		return parent.invoke(cx, delegateTo, key, args, returnType);
	}

	@Override
	public Object deleteValue(ContextJS cx, @Nullable Object self, Object key, CastType returnType) {
		return parent.deleteValue(cx, delegateTo, key, returnType);
	}
}

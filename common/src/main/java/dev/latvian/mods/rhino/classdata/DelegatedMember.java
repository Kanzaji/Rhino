package dev.latvian.mods.rhino.classdata;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Scriptable;
import org.jetbrains.annotations.Nullable;

public record DelegatedMember(Object delegateTo, BaseMember parent) implements BaseMember {
	@Override
	public MethodSignature getSignature() {
		return parent.getSignature();
	}

	@Override
	public Object get(Context cx, Scriptable scope, @Nullable Object self) throws Exception {
		return parent.get(cx, scope, delegateTo);
	}

	@Override
	public void set(Context cx, Scriptable scope, @Nullable Object self, Object value) throws Exception {
		parent.set(cx, scope, delegateTo, value);
	}

	@Override
	public Object invoke(Context cx, Scriptable scope, @Nullable Object self, Object[] args, MethodSignature argsSig) throws Exception {
		return parent.invoke(cx, scope, delegateTo, args, argsSig);
	}

	@Override
	public boolean isStatic() {
		return parent.isStatic();
	}

	@Override
	public String getName() {
		return parent.getName();
	}

	@Override
	public Class<?> getType() {
		return parent.getType();
	}

	@Override
	public boolean isVarArgs() {
		return parent.isVarArgs();
	}

	@Override
	public Class<?> getDeclaringClass() {
		return parent.getDeclaringClass();
	}
}

package dev.latvian.mods.rhino.classdata;

import dev.latvian.mods.rhino.ContextJS;
import org.jetbrains.annotations.Nullable;

public record DelegatedMember(Object delegateTo, BaseMember parent) implements BaseMember {
	@Override
	public MethodSignature getSignature() {
		return parent.getSignature();
	}

	@Override
	public Object get(ContextJS cx, @Nullable Object self) throws Exception {
		return parent.get(cx, delegateTo);
	}

	@Override
	public boolean set(ContextJS cx, @Nullable Object self, Object value) throws Exception {
		return parent.set(cx, delegateTo, value);
	}

	@Override
	public Object invoke(ContextJS cx, @Nullable Object self, Object[] args) throws Exception {
		return parent.invoke(cx, delegateTo, args);
	}

	@Override
	public Object getJS(ContextJS cx, @Nullable Object self) {
		return parent.getJS(cx, delegateTo);
	}

	@Override
	public void setJS(ContextJS cx, @Nullable Object self, Object value) {
		parent.setJS(cx, delegateTo, value);
	}

	@Override
	public Object invokeJS(ContextJS cx, @Nullable Object self, Object[] args) {
		return parent.invokeJS(cx, delegateTo, args);
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

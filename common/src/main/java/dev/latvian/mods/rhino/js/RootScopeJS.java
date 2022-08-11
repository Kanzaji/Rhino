package dev.latvian.mods.rhino.js;

import org.jetbrains.annotations.Nullable;

public class RootScopeJS extends ScopeJS {
	@Override
	@Nullable
	public ScopeJS getParent() {
		return null;
	}

	@Override
	public void setParent(@Nullable ScopeJS parent) {
	}

	@Override
	public ScopeJS getRoot() {
		return this;
	}
}

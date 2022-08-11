package dev.latvian.mods.rhino.js;

import org.jetbrains.annotations.Nullable;

public class RootScopeJS extends ScopeJS {
	// Keep ClassCache and other shared data here

	@Override
	@Nullable
	public ScopeJS getParent() {
		return null;
	}

	@Override
	public void setParent(@Nullable ScopeJS parent) {
	}

	@Override
	public RootScopeJS getRoot() {
		return this;
	}
}

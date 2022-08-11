package dev.latvian.mods.rhino.js;

import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public class ScopeJS {
	private ScopeJS parent;
	private Map<String, ObjectJS> objects;

	@Nullable
	public ScopeJS getParent() {
		return parent;
	}

	public void setParent(@Nullable ScopeJS parent) {
		this.parent = parent;
	}

	public RootScopeJS getRoot() {
		ScopeJS scope = this;

		while (scope.parent != null) {
			scope = scope.parent;
		}

		return (RootScopeJS) scope;
	}

	public void set(String name, ObjectJS value) {
		if (objects == null) {
			objects = new LinkedHashMap<>();
		}

		objects.put(name, value);
	}

	public void remove(String name) {
		if (objects != null) {
			objects.remove(name);

			if (objects.isEmpty()) {
				objects = null;
			}
		}
	}

	public ObjectJS get(String name) throws Exception {
		var value = objects.get(name);
		return value != null ? value : parent != null ? parent.get(name) : SpecialJS.NOT_FOUND;
	}
}

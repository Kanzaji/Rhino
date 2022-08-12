package dev.latvian.mods.rhino.js;

import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public class ScopeJS {
	private ScopeJS parent;
	private Map<String, Object> objects;

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

	public void set(String name, Object value) {
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

	public Object get(String name) throws Exception {
		ScopeJS s = this;

		while (s != null) {
			var o = s.objects == null ? null : s.objects.get(name);

			if (o != null) {
				return o;
			}

			s = s.parent;
		}

		return UndefinedJS.PROTOTYPE;
	}
}

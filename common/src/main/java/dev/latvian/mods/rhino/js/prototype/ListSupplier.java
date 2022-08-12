package dev.latvian.mods.rhino.js.prototype;

import dev.latvian.mods.rhino.ContextJS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface ListSupplier {
	ListSupplier EMPTY = (cx, prototype, self) -> Collections.emptyList();
	ListSupplier DEFAULT_KEYS = ListSupplier::getDefaultKeys;
	ListSupplier DEFAULT_VALUES = ListSupplier::getDefaultValues;
	ListSupplier DEFAULT_ENTRIES = ListSupplier::getDefaultEntries;

	List<?> get(ContextJS cx, PrototypeJS prototype, Object self);

	static List<?> getDefaultKeys(ContextJS cx, PrototypeJS prototype, Object self) {
		if (self instanceof Map<?, ?> m) {
			return new ArrayList<>(m.keySet());
		} else if (self instanceof Collection<?> c) {
			Object[] keys = new Object[c.size()];

			for (int i = 0; i < keys.length; i++) {
				keys[i] = i;
			}

			return Arrays.asList(keys);
		} else if (self instanceof Iterable<?> itr) {
			int size = 0;

			for (Object o : itr) {
				size++;
			}

			Object[] keys = new Object[size];

			for (int i = 0; i < keys.length; i++) {
				keys[i] = i;
			}

			return Arrays.asList(keys);
		}

		return List.of();
	}

	static List<?> getDefaultValues(ContextJS cx, PrototypeJS prototype, Object self) {
		if (self instanceof List<?> list) {
			return list;
		} else if (self instanceof Map<?, ?> m) {
			return new ArrayList<>(m.values());
		} else if (self instanceof Collection<?> c) {
			return new ArrayList<>(c);
		} else if (self instanceof Iterable<?> itr) {
			List<Object> list = new ArrayList<>();

			for (Object o : itr) {
				list.add(o);
			}

			return list;
		}

		return List.of();
	}

	static List<?> getDefaultEntries(ContextJS cx, PrototypeJS prototype, Object self) {
		var keys = prototype.getKeyList(cx, self);
		var values = prototype.getValueList(cx, self);

		if (keys.size() == values.size()) {
			Object[] entries = new Object[keys.size()];

			for (int i = 0; i < keys.size(); i++) {
				var v = values.get(i);

				if (v == null) {
					entries[i] = Arrays.asList(keys.get(i), null);
				} else {
					entries[i] = List.of(keys.get(i), v);
				}
			}

			return Arrays.asList(entries);
		}

		return List.of();
	}
}

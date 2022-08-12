package dev.latvian.mods.rhino.mod.util;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author LatvianModder
 */
public class CountingMap {
	private final Object2LongOpenHashMap<Object> map;

	public CountingMap() {
		map = new Object2LongOpenHashMap<>();
		map.defaultReturnValue(0L);
	}

	public long get(Object key) {
		return map.getLong(key);
	}

	public long set(Object key, long value) {
		if (value <= 0L) {
			return map.removeLong(key);
		} else {
			return map.put(key, value);
		}
	}

	public long add(Object key, long value) {
		return set(key, get(key) + value);
	}

	public void clear() {
		map.clear();
	}

	public int getSize() {
		return map.size();
	}

	public void forEach(Consumer<Object2LongMap.Entry<Object>> forEach) {
		map.object2LongEntrySet().fastForEach(forEach);
	}

	public List<Object[]> getEntries() {
		List<Object[]> list = new ArrayList<>(map.size());
		map.object2LongEntrySet().fastForEach(e -> list.add(new Object[]{e.getKey(), e.getLongValue()}));
		return list;
	}

	public Set<Object> getKeys() {
		return map.keySet();
	}

	public Collection<Long> getValues() {
		return map.values();
	}

	public long getTotalCount() {
		final long[] count = {0L};
		forEach(entry -> count[0] += entry.getLongValue());
		return count[0];
	}
}
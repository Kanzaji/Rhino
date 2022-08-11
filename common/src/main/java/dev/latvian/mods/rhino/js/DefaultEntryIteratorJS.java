package dev.latvian.mods.rhino.js;

import java.util.Iterator;
import java.util.List;

public record DefaultEntryIteratorJS(Iterator<ObjectJS> keys, Iterator<ObjectJS> values) implements Iterator<ObjectJS> {
	@Override
	public boolean hasNext() {
		return keys.hasNext() && values.hasNext();
	}

	@Override
	public ObjectJS next() {
		var k = keys.next();
		var v = values.next();
		return new ArrayJS(List.of(k.unwrap(), v.unwrap()), ArrayJS.IMMUTABLE);
	}

	@Override
	public void remove() {
		keys.remove();
		values.remove();
	}
}

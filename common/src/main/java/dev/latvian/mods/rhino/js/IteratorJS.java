package dev.latvian.mods.rhino.js;

import dev.latvian.mods.rhino.ContextJS;

import java.util.Iterator;
import java.util.List;

public abstract class IteratorJS implements Iterator<ObjectJS> {
	public final ObjectJS objectJS;

	public IteratorJS(ObjectJS objectJS) {
		this.objectJS = objectJS;
	}

	public static class Keys extends IteratorJS {
		private final Object[] keys;
		private int index;

		public Keys(ObjectJS objectJS) {
			super(objectJS);
			this.keys = objectJS.getKeys();
		}

		@Override
		public boolean hasNext() {
			return index < keys.length;
		}

		@Override
		public ObjectJS next() {
			return ObjectJS.wrap(keys[index++]);
		}
	}

	public static class ArrayKeys extends IteratorJS {
		private int index;

		public ArrayKeys(ArrayJS objectJS) {
			super(objectJS);
			this.index = 0;
		}

		@Override
		public boolean hasNext() {
			return index < ((ArrayJS) objectJS).list.size();
		}

		@Override
		public ObjectJS next() {
			return NumberJS.of(index++);
		}
	}

	public static class Values extends IteratorJS {
		private final ContextJS cx;
		private final Object[] keys;
		private int index;

		public Values(ContextJS cx, ObjectJS objectJS) {
			super(objectJS);
			this.cx = cx;
			this.keys = objectJS.getKeys();
		}

		@Override
		public boolean hasNext() {
			return index < keys.length;
		}

		@Override
		public ObjectJS next() {
			var k = keys[index++];

			try {
				if (k instanceof Number n) {
					return objectJS.get(cx, n.intValue());
				} else {
					return objectJS.get(cx, k.toString());
				}
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
	}

	public static class ArrayValues extends IteratorJS {
		private int index;

		public ArrayValues(ArrayJS objectJS) {
			super(objectJS);
			this.index = 0;
		}

		@Override
		public boolean hasNext() {
			return index < ((ArrayJS) objectJS).list.size();
		}

		@Override
		public ObjectJS next() {
			return ObjectJS.wrap(((ArrayJS) objectJS).list.get(index++));
		}
	}

	public static class Entries extends IteratorJS {
		public IteratorJS keys, values;

		public Entries(ContextJS cx, ObjectJS objectJS) {
			super(objectJS);
			keys = objectJS.keyIterator(cx);
			values = objectJS.valueIterator(cx);
		}

		@Override
		public boolean hasNext() {
			return keys.hasNext() && values.hasNext();
		}

		@Override
		public ObjectJS next() {
			return new ArrayJS(List.of(keys.next(), values.next()), ArrayJS.IMMUTABLE);
		}
	}
}

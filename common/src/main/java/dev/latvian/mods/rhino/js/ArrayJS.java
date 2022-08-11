package dev.latvian.mods.rhino.js;

import dev.latvian.mods.rhino.ContextJS;
import dev.latvian.mods.rhino.Wrapper;
import dev.latvian.mods.rhino.util.Deletable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings({"unchecked"})
public class ArrayJS extends JavaObjectJS {
	public static final PrototypeJS PROTOTYPE = OBJECT_PROTOTYPE.createSub("Array")
			.constructor(ArrayJS::construct);

	public static final int MUTABLE = 0;
	public static final int IMMUTABLE = 1;
	public static final int IMMUTABLE_SIZE = 2;

	private static ArrayJS construct(ContextJS cx, Object[] args) {
		return new ArrayJS(new ArrayList<>(Arrays.asList(Wrapper.unwrappedArray(args))), MUTABLE);
	}

	public final List<Object> list;
	public final int type;

	public ArrayJS(Object javaObject, List<?> list, int type) {
		super(javaObject);
		this.list = (List<Object>) list;
		this.type = type;
	}

	public ArrayJS(List<?> list, int type) {
		this(list, list, type);
	}

	@Override
	public PrototypeJS getPrototype() {
		return PROTOTYPE;
	}

	@Override
	public TypeJS getType() {
		return TypeJS.ARRAY;
	}

	@Override
	public ObjectJS get(ContextJS cx, int index) throws Exception {
		return wrap(list.get(index));
	}

	@Override
	public void set(ContextJS cx, int index, ObjectJS value) throws Exception {
		list.set(index, Wrapper.unwrapped(value));
	}

	@Override
	public void delete(ContextJS cx, int index) throws Exception {
		Deletable.deleteObject(list.remove(index));
	}

	@Override
	public Object[] getKeys() {
		Object[] keys = new Object[list.size()];

		for (int i = 0; i < keys.length; i++) {
			keys[i] = i;
		}

		return keys;
	}

	@Override
	public Object cast(TypeJS type) {
		return type == TypeJS.ARRAY ? list : super.cast(type);
	}

	@Override
	public ObjectJS castJS(TypeJS type) {
		return type == TypeJS.ARRAY ? this : super.castJS(type);
	}

	@Override
	public IteratorJS keyIterator(ContextJS cx) {
		return new IteratorJS.ArrayKeys(this);
	}

	@Override
	public IteratorJS valueIterator(ContextJS cx) {
		return new IteratorJS.ArrayValues(this);
	}
}

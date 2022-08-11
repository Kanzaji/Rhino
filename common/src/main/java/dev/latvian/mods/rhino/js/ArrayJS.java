package dev.latvian.mods.rhino.js;

import dev.latvian.mods.rhino.ContextJS;
import dev.latvian.mods.rhino.Wrapper;

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

	public ArrayJS(List<?> list, int type) {
		super(list);
		this.list = (List<Object>) javaObject;
		this.type = type;
	}

	@Override
	public PrototypeJS getPrototype() {
		return PROTOTYPE;
	}
}

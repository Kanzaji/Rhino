package dev.latvian.mods.rhino.js;

import dev.latvian.mods.rhino.ContextJS;
import dev.latvian.mods.rhino.Wrapper;
import dev.latvian.mods.rhino.util.JavaSetWrapper;
import dev.latvian.mods.rhino.util.NativeArrayWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * New experimental replacement for {@link dev.latvian.mods.rhino.Scriptable} classes
 */
public abstract class ObjectJS implements AsJS, Wrapper {
	public static ObjectJS wrap(ContextJS cx, @Nullable Object object) {
		if (object == null) {
			return SpecialJS.NULL;
		} else if (object instanceof AsJS o) {
			return o.asJS(cx);
		} else if (object instanceof CharSequence o) {
			return StringJS.of(o);
		} else if (object instanceof Number o) {
			return NumberJS.of(o.doubleValue());
		} else if (object instanceof Boolean o) {
			return BooleanJS.of(o);
		} else if (object instanceof Class<?> o) {
			return new JavaClassJS(o);
		} else if (object instanceof List<?> o) {
			return new ArrayJS(o, ArrayJS.MUTABLE);
		} else if (object instanceof Set<?> o) {
			return new ArrayJS(new JavaSetWrapper<>(o), ArrayJS.MUTABLE);
		} else if (object.getClass().isArray()) {
			return new ArrayJS(NativeArrayWrapper.of(object), ArrayJS.IMMUTABLE_SIZE);
		} else {
			return new JavaObjectJS(object);
		}
	}

	public PrototypeJS getPrototype() {
		return PrototypeJS.EMPTY;
	}

	public boolean isNull() {
		return false;
	}

	public String asString() {
		return String.valueOf(unwrap());
	}

	public double asNumber() {
		return Double.NaN;
	}

	@Override
	public final ObjectJS asJS(ContextJS cx) {
		return this;
	}

	@Override
	public Object unwrap() {
		throw new RuntimeException("unwrap()");
	}

	public ObjectJS get(ContextJS cx, String name) throws Exception {
		var prop = getPrototype().getProperty(name);

		if (prop != null) {
			return prop.get(cx, this);
		}

		return SpecialJS.NOT_FOUND;
	}

	public void set(ContextJS cx, String name, ObjectJS value) throws Exception {
		throw new RuntimeException("set(name)");
	}

	public void delete(ContextJS cx, String name) throws Exception {
		throw new RuntimeException("delete(name)");
	}

	public ObjectJS get(ContextJS cx, int index) throws Exception {
		throw new RuntimeException("get(index)");
	}

	public void set(ContextJS cx, int index, ObjectJS value) throws Exception {
		throw new RuntimeException("set(index)");
	}

	public void delete(ContextJS cx, int index) throws Exception {
		throw new RuntimeException("delete(index)");
	}

	public ObjectJS invoke(ContextJS cx, String name, ObjectJS[] args) throws Exception {
		var func = getPrototype().getFunction(name);

		if (func != null) {
			return func.invoke(cx, this, args);
		}

		return SpecialJS.NOT_FOUND;
	}

	public Iterator<ObjectJS> valueIterator(ContextJS cx) {
		return Collections.singletonList(this).iterator();
	}

	public Iterator<ObjectJS> keyIterator(ContextJS cx) {
		return Collections.emptyIterator();
	}

	@SuppressWarnings({"CastCanBeRemovedNarrowingVariableType", "unchecked"})
	public Iterator<ObjectJS> entryIterator(ContextJS cx) {
		Iterator<?> keyIterator = keyIterator(cx);

		if (keyIterator == Collections.emptyIterator()) {
			return Collections.emptyIterator();
		}

		return new DefaultEntryIteratorJS((Iterator<ObjectJS>) keyIterator, valueIterator(cx));
	}
}

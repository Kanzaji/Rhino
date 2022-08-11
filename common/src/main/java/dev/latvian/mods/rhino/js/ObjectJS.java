package dev.latvian.mods.rhino.js;

import dev.latvian.mods.rhino.ContextJS;
import dev.latvian.mods.rhino.ScriptRuntime;
import dev.latvian.mods.rhino.Wrapper;
import dev.latvian.mods.rhino.util.JavaSetWrapper;
import dev.latvian.mods.rhino.util.NativeArrayWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * New experimental replacement for {@link dev.latvian.mods.rhino.Scriptable} classes
 */
public abstract class ObjectJS implements AsJS, Wrapper {
	public static ObjectJS wrap(@Nullable Object object) {
		if (object == null) {
			return SpecialJS.NULL;
		} else if (object instanceof AsJS o) {
			return o.asJS();
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

	public TypeJS getType() {
		return TypeJS.OBJECT;
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

	public boolean asBoolean() {
		return true;
	}

	@Override
	public final ObjectJS asJS() {
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

		return SpecialJS.UNDEFINED;
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

		return SpecialJS.UNDEFINED;
	}

	public Object[] getKeys() {
		return ScriptRuntime.EMPTY_OBJECTS;
	}

	public boolean isInstanceOf(ObjectJS rhs) {
		return getType() == rhs.getType();
	}

	public Object cast(TypeJS type) {
		if (type == TypeJS.STRING) {
			return asString();
		} else if (type == TypeJS.NUMBER) {
			return asNumber();
		} else if (type == TypeJS.BOOLEAN) {
			return asBoolean();
		} else if (type == TypeJS.ARRAY) {
			return Collections.singletonList(unwrap());
		} else {
			return unwrap();
		}
	}

	public ObjectJS castJS(TypeJS type) {
		if (type == TypeJS.STRING) {
			return StringJS.of(asString());
		} else if (type == TypeJS.NUMBER) {
			return NumberJS.of(asNumber());
		} else if (type == TypeJS.BOOLEAN) {
			return BooleanJS.of(asBoolean());
		} else if (type == TypeJS.ARRAY) {
			return new ArrayJS(Collections.singletonList(unwrap()), ArrayJS.IMMUTABLE);
		} else {
			return this;
		}
	}

	public IteratorJS keyIterator(ContextJS cx) {
		return new IteratorJS.Keys(this);
	}

	public IteratorJS valueIterator(ContextJS cx) {
		return new IteratorJS.Values(cx, this);
	}

	public IteratorJS entryIterator(ContextJS cx) {
		return new IteratorJS.Entries(cx, this);
	}
}

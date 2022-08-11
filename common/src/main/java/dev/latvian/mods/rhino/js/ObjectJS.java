package dev.latvian.mods.rhino.js;

import dev.latvian.mods.rhino.ContextJS;
import dev.latvian.mods.rhino.Wrapper;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;

/**
 * New experimental replacement for {@link dev.latvian.mods.rhino.Scriptable} classes
 */
public abstract class ObjectJS implements Wrapper {
	public static ObjectJS wrap(@Nullable Object object) {
		if (object == null) {
			return SpecialJS.NULL;
		} else if (object instanceof ObjectJS o) {
			return o;
		} else if (object instanceof CharSequence o) {
			return StringJS.of(o);
		} else if (object instanceof Number o) {
			return NumberJS.of(o.doubleValue());
		} else if (object instanceof Boolean o) {
			return BooleanJS.of(o);
		} else {
			return SpecialJS.NOT_FOUND;
		}
	}

	public final PrototypeJS prototype;
	public int attributes;

	public ObjectJS(PrototypeJS prototype) {
		this.prototype = prototype;
		this.attributes = 0;
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
	public Object unwrap() {
		throw new RuntimeException("unwrap()");
	}

	public ObjectJS get(ContextJS cx, String name) throws Exception {
		var prop = prototype.getProperty(name);

		if (prop != null) {
			return prop.get(cx, this);
		}

		return SpecialJS.NOT_FOUND;
	}

	public void set(ContextJS cx, String name, ObjectJS value) throws Exception {
	}

	public void delete(ContextJS cx, String name) throws Exception {
	}

	public ObjectJS get(ContextJS cx, int index) throws Exception {
		return SpecialJS.NOT_FOUND;
	}

	public void set(ContextJS cx, int index, ObjectJS value) throws Exception {
	}

	public void delete(ContextJS cx, int index) throws Exception {
	}

	public ObjectJS getProperty(ContextJS cx) throws Exception {
		return this;
	}

	public ObjectJS invoke(ContextJS cx, String name, ObjectJS[] args) throws Exception {
		var func = prototype.getFunction(name);

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

	public Iterator<ObjectJS> entryIterator(ContextJS cx) {
		return Collections.emptyIterator();
	}
}

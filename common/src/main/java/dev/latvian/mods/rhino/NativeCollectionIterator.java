package dev.latvian.mods.rhino;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.Collections;
import java.util.Iterator;

public class NativeCollectionIterator extends ES6Iterator {
	@Serial
	private static final long serialVersionUID = 7094840979404373443L;
	private String className;
	private Type type;
	private transient Iterator<Hashtable.Entry> iterator = Collections.emptyIterator();

	enum Type {
		KEYS, VALUES, BOTH
	}

	static void init(Context cx, ScriptableObject scope, String tag, boolean sealed) {
		init(cx, scope, sealed, new NativeCollectionIterator(tag), tag);
	}

	public NativeCollectionIterator(String tag) {
		this.className = tag;
		this.iterator = Collections.emptyIterator();
		this.type = Type.BOTH;
	}

	public NativeCollectionIterator(Context cx, Scriptable scope, String className, Type type, Iterator<Hashtable.Entry> iterator) {
		super(cx, scope, className);
		this.className = className;
		this.iterator = iterator;
		this.type = type;
	}

	@Override
	public String getClassName() {
		return className;
	}

	@Override
	protected boolean isDone(Context cx, Scriptable scope) {
		return !iterator.hasNext();
	}

	@Override
	protected Object nextValue(Context cx, Scriptable scope) {
		final Hashtable.Entry e = iterator.next();
		return switch (type) {
			case KEYS -> e.key;
			case VALUES -> e.value;
			case BOTH -> cx.newArray(scope, new Object[]{e.key, e.value});
			default -> throw new AssertionError();
		};
	}

	@Serial
	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		stream.defaultReadObject();
		className = (String) stream.readObject();
		type = (Type) stream.readObject();
		iterator = Collections.emptyIterator();
	}

	@Serial
	private void writeObject(ObjectOutputStream stream) throws IOException {
		stream.defaultWriteObject();
		stream.writeObject(className);
		stream.writeObject(type);
	}
}

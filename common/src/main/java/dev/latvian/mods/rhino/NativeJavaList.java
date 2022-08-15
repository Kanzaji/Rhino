package dev.latvian.mods.rhino;

import java.util.List;

public class NativeJavaList extends NativeJavaObject {
	public NativeJavaList(Scriptable scope, Object javaObject) {
		super(scope, javaObject, List.class);
	}

	public long getLength() {
		return ((List<?>) javaObject).size();
	}
}

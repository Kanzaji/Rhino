package dev.latvian.mods.rhino;

/**
 * This is the object that is stored in the SlotMap. For historical reasons it remains
 * inside this class. SlotMap references a number of members of this class directly.
 */
public class Slot {
	Object name; // This can change due to caching
	int indexOrHash;
	private short attributes;
	Object value;
	transient Slot next; // next in hash table bucket
	transient Slot orderedNext; // next in linked list

	Slot(Object name, int indexOrHash, int attributes) {
		this.name = name;
		this.indexOrHash = indexOrHash;
		this.attributes = (short) attributes;
	}

	boolean setValue(Context cx, Object value, Scriptable owner, Scriptable start) {
		if ((attributes & ScriptableObject.READONLY) != 0) {
			if (cx.isStrictMode()) {
				throw ScriptRuntime.typeError1("msg.modify.readonly", name);
			}
			return true;
		}
		if (owner == start) {
			this.value = value;
			return true;
		}
		return false;
	}

	Object getValue(Context cx, Scriptable start) {
		return value;
	}

	int getAttributes() {
		return attributes;
	}

	synchronized void setAttributes(int value) {
		ScriptableObject.checkValidAttributes(value);
		attributes = (short) value;
	}

	ScriptableObject getPropertyDescriptor(Context cx, Scriptable scope) {
		return ScriptableObject.buildDataDescriptor(cx, scope, value, attributes);
	}

}
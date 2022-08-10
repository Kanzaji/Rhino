package dev.latvian.mods.rhino;


import dev.latvian.mods.rhino.classdata.BaseMember;
import dev.latvian.mods.rhino.classdata.DelegatedMember;
import dev.latvian.mods.rhino.classdata.MethodSignature;

/**
 * A GetterSlot is a specialication of a Slot for properties that are assigned functions
 * via Object.defineProperty() and its friends instead of regular values.
 */
public class GetterSlot extends Slot {
	Object getter;
	Object setter;

	GetterSlot(Object name, int indexOrHash, int attributes) {
		super(name, indexOrHash, attributes);
	}

	@Override
	ScriptableObject getPropertyDescriptor(Context cx, Scriptable scope) {
		int attr = getAttributes();
		ScriptableObject desc = new NativeObject();
		ScriptRuntime.setBuiltinProtoAndParent(cx, desc, scope, TopLevel.Builtins.Object);
		desc.defineProperty(cx, "enumerable", (attr & ScriptableObject.DONTENUM) == 0, ScriptableObject.EMPTY);
		desc.defineProperty(cx, "configurable", (attr & ScriptableObject.PERMANENT) == 0, ScriptableObject.EMPTY);
		if (getter == null && setter == null) {
			desc.defineProperty(cx, "writable", (attr & ScriptableObject.READONLY) == 0, ScriptableObject.EMPTY);
		}

		String fName = name == null ? "f" : name.toString();
		if (getter != null) {
			if (getter instanceof BaseMember m) {
				desc.defineProperty(cx, "get", new FunctionObject(cx, fName, m, scope), ScriptableObject.EMPTY);
			} else {
				desc.defineProperty(cx, "get", getter, ScriptableObject.EMPTY);
			}
		}

		if (setter != null) {
			if (setter instanceof BaseMember m) {
				desc.defineProperty(cx, "set", new FunctionObject(cx, fName, m, scope), ScriptableObject.EMPTY);
			} else {
				desc.defineProperty(cx, "set", setter, ScriptableObject.EMPTY);
			}
		}
		return desc;
	}

	@Override
	boolean setValue(Context cx, Object value, Scriptable owner, Scriptable start) {
		if (setter == null) {
			if (getter != null) {
				if (cx.isStrictMode()) {

					String prop = "";
					if (name != null) {
						prop = "[" + start.getClassName() + "]." + name;
					}
					throw ScriptRuntime.typeError2("msg.set.prop.no.setter", prop, ScriptRuntime.toString(cx, value));
				}
				// Assignment to a property with only a getter defined. The
				// assignment is ignored. See bug 478047.
				return true;
			}
		} else {
			if (setter instanceof BaseMember nativeSetter) {
				Class<?>[] pTypes = nativeSetter.getSignature().types;
				// XXX: cache tag since it is already calculated in
				// defineProperty ?
				Class<?> valueType = pTypes[pTypes.length - 1];
				int tag = FunctionObject.getTypeTag(valueType);
				Object actualArg = FunctionObject.convertArg(cx, start, value, tag);
				Object[] args;
				if (nativeSetter instanceof DelegatedMember) {
					args = new Object[]{start, actualArg};
				} else {
					args = new Object[]{actualArg};
				}
				nativeSetter.actuallyInvoke(cx, start, start, args, MethodSignature.ofArgs(args));
			} else if (setter instanceof Function f) {
				f.call(cx, f.getParentScope(), start, new Object[]{value});
			}
			return true;
		}
		return super.setValue(cx, value, owner, start);
	}

	@Override
	Object getValue(Context cx, Scriptable start) {
		if (getter != null) {
			if (getter instanceof MemberBox nativeGetter) {
				Object getterThis;
				Object[] args;
				if (nativeGetter.delegateTo == null) {
					getterThis = start;
					args = ScriptRuntime.EMPTY_OBJECTS;
				} else {
					getterThis = nativeGetter.delegateTo;
					args = new Object[]{start};
				}
				return nativeGetter.invoke(getterThis, args);
			} else if (getter instanceof Function f) {
				return f.call(cx, f.getParentScope(), start, ScriptRuntime.EMPTY_OBJECTS);
			}
		}
		return this.value;
	}
}
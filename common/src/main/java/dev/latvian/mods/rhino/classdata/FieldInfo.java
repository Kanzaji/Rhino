package dev.latvian.mods.rhino.classdata;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.ContextJS;
import dev.latvian.mods.rhino.js.prototype.CastType;
import dev.latvian.mods.rhino.js.prototype.MemberFunctions;
import dev.latvian.mods.rhino.util.RemapForJS;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class FieldInfo implements MemberFunctions {
	public static final FieldInfo[] EMPTY_ARRAY = new FieldInfo[0];

	public final transient Field javaField;
	public final String remappedName;

	FieldInfo(Field javaField, String[] remapPrefixes) {
		this.javaField = javaField;
		var ra = javaField.getAnnotation(RemapForJS.class);

		String remappedName = ra == null ? "" : ra.value();

		if (remappedName.isEmpty()) {
			for (String s : remapPrefixes) {
				if (javaField.getName().startsWith(s)) {
					remappedName = javaField.getName().substring(s.length());
					break;
				}
			}
		}

		this.remappedName = remappedName;
	}

	@Override
	public boolean isStatic() {
		return Modifier.isStatic(javaField.getModifiers());
	}

	@Override
	public Object getValue(ContextJS cx, @Nullable Object self, Object key, CastType returnType) {
		try {
			return returnType.cast(cx, javaField.get(self));
		} catch (Exception ex) {
			throw Context.throwAsScriptRuntimeEx(ex);
		}
	}

	@Override
	public boolean setValue(ContextJS cx, @Nullable Object self, Object key, Object value, CastType valueType) {
		try {
			javaField.set(self, valueType.cast(cx, value));
		} catch (Exception ex) {
			throw Context.throwAsScriptRuntimeEx(ex);
		}

		return true;
	}
}
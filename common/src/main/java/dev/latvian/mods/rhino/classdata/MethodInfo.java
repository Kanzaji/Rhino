package dev.latvian.mods.rhino.classdata;

import dev.latvian.mods.rhino.ContextJS;
import dev.latvian.mods.rhino.util.HideFromJS;
import dev.latvian.mods.rhino.util.RemapForJS;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

public class MethodInfo extends ExecutableInfo {
	public static final MethodInfo[] EMPTY_ARRAY = new MethodInfo[0];

	public final String remappedName;

	MethodInfo(Method method, MutableBoolean hidden, PublicClassData[] parentList, String[] remapPrefixes) {
		super(method);
		String _remappedName = "";

		var m = method;

		for (var c : parentList) {
			if (m != null) {
				if (m.isAnnotationPresent(HideFromJS.class)) {
					hidden.setTrue();
					break;
				}

				if (_remappedName.isBlank()) {
					var ra = m.getAnnotation(RemapForJS.class);
					_remappedName = ra == null ? "" : ra.value();
				}
			}

			try {
				m = c.type.getDeclaredMethod(method.getName(), signature.types);
			} catch (Exception ex) {
				m = null;
			}
		}

		if (hidden.isFalse() && _remappedName.isBlank()) {
			for (String s : remapPrefixes) {
				if (method.getName().startsWith(s)) {
					_remappedName = method.getName().substring(s.length());
					break;
				}
			}
		}

		remappedName = _remappedName;
	}

	@Override
	public Class<?> getType() {
		return ((Method) member).getReturnType();
	}

	public boolean isVoid() {
		var c = getType();
		return c == void.class || c == Void.class;
	}

	public boolean isBoolean() {
		var c = getType();
		return c == boolean.class || c == Boolean.class;
	}

	@Override
	public Object invoke(ContextJS cx, @Nullable Object self, Object[] args) throws Exception {
		return ((Method) member).invoke(nullIfStatic(self), args);
	}
}
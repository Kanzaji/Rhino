package dev.latvian.mods.rhino.classdata;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.util.RemapForJS;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class FieldInfo extends MemberInfo implements BaseMember {
	public static final FieldInfo[] EMPTY_ARRAY = new FieldInfo[0];

	public final String remappedName;

	FieldInfo(Field field, String[] remapPrefixes) {
		super(field);
		var ra = field.getAnnotation(RemapForJS.class);

		String _remappedName = ra == null ? "" : ra.value();

		if (_remappedName.isEmpty()) {
			for (String s : remapPrefixes) {
				if (field.getName().startsWith(s)) {
					_remappedName = field.getName().substring(s.length());
					break;
				}
			}
		}

		remappedName = _remappedName;
	}

	public boolean isFinal() {
		return Modifier.isFinal(member.getModifiers());
	}

	@Override
	public Class<?> getType() {
		return ((Field) member).getType();
	}

	@Override
	public boolean isMethod() {
		return false;
	}

	@Override
	public Object get(Context cx, Scriptable scope, @Nullable Object self) throws Exception {
		return ((Field) member).get(nullIfStatic(self));
	}

	@Override
	public void set(Context cx, Scriptable scope, @Nullable Object self, Object value) throws Exception {
		if (isFinal()) {
			throw new IllegalStateException("Cannot set final field " + member.getName());
		}

		((Field) member).set(nullIfStatic(self), value);
	}
}
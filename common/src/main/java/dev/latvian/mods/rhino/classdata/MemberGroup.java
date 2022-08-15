package dev.latvian.mods.rhino.classdata;

import dev.latvian.mods.rhino.ContextJS;
import dev.latvian.mods.rhino.ScriptRuntime;
import dev.latvian.mods.rhino.js.UndefinedJS;
import dev.latvian.mods.rhino.js.prototype.CastType;
import dev.latvian.mods.rhino.js.prototype.MemberFunctions;
import org.jetbrains.annotations.Nullable;

public class MemberGroup implements MemberFunctions {
	public final MemberFunctions field;
	public final MemberFunctions methods;
	public final MemberFunctions beanGet;
	public final MemberFunctions beanSet;

	MemberGroup(TempMemberGroup g) {
		field = g.field;

		if (g.methods == null || g.methods.isEmpty()) {
			methods = null;
		} else if (g.methods.size() == 1) {
			methods = g.methods.get(0);
		} else {
			ExecutableInfo[] info = new ExecutableInfo[g.methods.size()];

			for (int i = 0; i < info.length; i++) {
				info[i] = (ExecutableInfo) g.methods.get(i);
			}

			methods = new ExecutableGroup(info);
		}

		beanGet = g.beanGet;
		beanSet = g.beanSet;
	}

	@Override
	public Object getValue(ContextJS cx, @Nullable Object self, Object key, CastType returnType) {
		if (beanGet != null) {
			return beanGet.invoke(cx, self, key, ScriptRuntime.EMPTY_OBJECTS, returnType);
		} else if (field != null) {
			return field.getValue(cx, self, key, returnType);
		} else {
			return UndefinedJS.PROTOTYPE;
		}
	}

	@Override
	public boolean setValue(ContextJS cx, @Nullable Object self, Object key, @Nullable Object value, CastType valueType) {
		if (beanSet != null) {
			return beanSet.invoke(cx, self, key, new Object[]{valueType.cast(cx, value)}, CastType.NONE) != UndefinedJS.PROTOTYPE;
		} else if (field != null) {
			return field.setValue(cx, self, key, value, valueType);
		} else {
			return false;
		}
	}

	@Override
	public Object invoke(ContextJS cx, @Nullable Object self, Object key, Object[] args, CastType returnType) {
		return methods == null ? UndefinedJS.PROTOTYPE : methods.invoke(cx, self, key, args, returnType);
	}
}

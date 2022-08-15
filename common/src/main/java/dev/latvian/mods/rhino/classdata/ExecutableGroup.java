package dev.latvian.mods.rhino.classdata;

import dev.latvian.mods.rhino.ContextJS;
import dev.latvian.mods.rhino.js.UndefinedJS;
import dev.latvian.mods.rhino.js.prototype.CastType;
import dev.latvian.mods.rhino.js.prototype.MemberFunctions;
import org.jetbrains.annotations.Nullable;

public record ExecutableGroup(ExecutableInfo[] methods) implements MemberFunctions {
	@Override
	public Object invoke(ContextJS cx, @Nullable Object self, Object key, Object[] args, CastType returnType) {
		ExecutableInfo p = null;
		int ca = -1;
		MethodSignature argsSig = MethodSignature.ofArgs(args);

		for (var m : methods) {
			int a = m.getSignature().matches(cx, args, argsSig);

			if (a > ca) {
				p = m;
				ca = a;
			}
		}

		if (p != null) {
			return p.invoke(cx, self, key, args, returnType);
		}

		return UndefinedJS.PROTOTYPE;
	}
}

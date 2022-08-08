package dev.latvian.mods.rhino.classdata;

import dev.latvian.mods.rhino.ScriptRuntime;
import dev.latvian.mods.rhino.SharedContextData;
import dev.latvian.mods.rhino.util.Possible;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class ClassMember {
	public final ClassData classData;
	public final String name;
	public boolean isFinal; // only fields

	public Field field;
	public Map<MethodSignature, MethodInfo> methods;
	public MethodInfo beanGet;
	public MethodInfo beanSet;

	private Map<MethodSignature, Possible<MethodInfo>> methodCache;

	ClassMember(ClassData c, String n) {
		classData = c;
		name = n;
	}

	@Override
	public String toString() {
		return name;
	}

	public Possible<?> get(SharedContextData data, @Nullable Object obj) throws Exception {
		if (beanGet != null) {
			return Possible.of(beanGet.method.invoke(obj, ScriptRuntime.EMPTY_ARGS));
		} else if (field != null) {
			return Possible.of(field.get(obj));
		}

		return Possible.EMPTY;
	}

	public Possible<?> set(SharedContextData data, @Nullable Object obj, @Nullable Object value) throws Exception {
		if (beanSet != null) {
			return Possible.of(beanSet.method.invoke(obj, value));
		} else if (!isFinal) {
			field.set(obj, value);
			return Possible.NULL;
		}

		return Possible.EMPTY;
	}

	public Possible<?> invoke(SharedContextData data, @Nullable Object obj, Object[] args, MethodSignature argsSig) throws Exception {
		var m = method(data, args, argsSig);

		if (m.isSet()) {
			return Possible.of(m.get().method.invoke(obj, args));
		}

		return Possible.EMPTY;
	}

	public Possible<MethodInfo> method(SharedContextData data, Object[] args, MethodSignature argsSig) {
		if (methods == null) {
			return Possible.absent();
		}

		if (methodCache == null) {
			methodCache = new HashMap<>();
		}

		var p = methodCache.get(argsSig);

		if (p == null) {
			p = Possible.absent();
			int ca = -1;

			for (var m : methods.values()) {
				int a = m.signature.matches(data, args, argsSig);

				if (a > ca) {
					p = Possible.of(m);
					ca = a;
				}
			}

			methodCache.put(argsSig, p);
		}

		return p;
	}

	public void merge(ClassMember member) {
		isFinal = isFinal || member.isFinal;

		if (field == null) {
			field = member.field;
		}

		if (member.methods != null && !member.methods.isEmpty()) {
			if (methods == null) {
				methods = new HashMap<>();
			}

			for (var entry : member.methods.entrySet()) {
				var mi = methods.get(entry.getKey());
				var m = entry.getValue();

				if (mi == null) {
					methods.put(entry.getKey(), m);
				} else {
					mi.isHidden = mi.isHidden || m.isHidden;

					if (mi.bean == null) {
						mi.bean = m.bean;
					}
				}
			}
		}

		if (beanGet == null) {
			beanGet = member.beanGet;
		}

		if (beanSet == null) {
			beanSet = member.beanSet;
		}
	}
}

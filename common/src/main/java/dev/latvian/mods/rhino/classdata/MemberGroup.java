package dev.latvian.mods.rhino.classdata;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.ScriptRuntime;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.util.Possible;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class MemberGroup implements BaseMember {
	public final String name;
	public BaseMember field;
	public Map<MethodSignature, BaseMember> methods;
	public BaseMember noArgMethod;
	public BaseMember beanGet;
	public BaseMember beanSet;

	private Map<MethodSignature, Possible<BaseMember>> methodCache;

	MemberGroup(String n) {
		name = n;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Class<?> getType() {
		if (field != null) {
			return field.getType();
		} else if (beanGet != null) {
			return beanGet.getType();
		} else if (beanSet != null) {
			return beanSet.getType();
		} else if (noArgMethod != null) {
			return noArgMethod.getType();
		} else if (methods != null) {
			return methods.values().iterator().next().getType();
		}

		throw new MemberMethodNotSupportedException.Type(this);
	}

	void optimize(Map<String, BaseMember> map) {
		int size = methods == null ? 0 : methods.size();

		if (field != null) {
			size++;
		}

		if (beanGet != null) {
			size++;
		}

		if (beanSet != null) {
			size++;
		}

		if (noArgMethod != null) {
			size++;
		}

		if (size != 1) {
			map.put(name, this);
		} else if (field != null) {
			map.put(name, field);
		} else if (beanGet != null) {
			map.put(name, beanGet);
		} else if (beanSet != null) {
			map.put(name, beanSet);
		} else if (noArgMethod != null) {
			map.put(name, noArgMethod);
		} else {
			map.put(name, methods.values().iterator().next());
		}
	}

	@Override
	public Object get(Context cx, Scriptable scope, @Nullable Object self) throws Exception {
		if (beanGet != null) {
			return beanGet.invoke(cx, scope, self, ScriptRuntime.EMPTY_OBJECTS, MethodSignature.EMPTY);
		} else if (field != null) {
			return field.get(cx, scope, self);
		}

		throw new MemberMethodNotSupportedException.Get(this);
	}

	@Override
	public void set(Context cx, Scriptable scope, @Nullable Object self, @Nullable Object value) throws Exception {
		if (beanSet != null) {
			beanSet.invoke(cx, scope, self, new Object[]{value}, MethodSignature.ofArgs(value));
		} else if (field != null) {
			field.set(cx, scope, self, value);
		}

		throw new MemberMethodNotSupportedException.Set(this);
	}

	@Override
	public Object invoke(Context cx, Scriptable scope, @Nullable Object self, Object[] args, MethodSignature argsSig) throws Exception {
		if (!argsSig.isEmpty()) {
			var m = method(cx, scope, args, argsSig);

			if (m.isSet()) {
				return m.get().invoke(cx, scope, self, args, argsSig);
			}
		} else if (noArgMethod != null) {
			return noArgMethod.invoke(cx, scope, self, args, argsSig);
		}

		throw new MemberMethodNotSupportedException.Invoke(this);
	}

	// FIXME: override actualGet, actualSet, actualInvoke for proper type

	private Possible<BaseMember> method(Context cx, Scriptable scope, Object[] args, MethodSignature argsSig) {
		if (methods == null) {
			return Possible.absent();
		}

		if (methodCache == null) {
			methodCache = new HashMap<>();
		}

		var p = methodCache.get(argsSig);

		if (p == null) {
			var data = cx.getSharedData(scope);
			p = Possible.absent();
			int ca = -1;

			for (var m : methods.values()) {
				int a = m.getSignature().matches(data, args, argsSig);

				if (a > ca) {
					p = Possible.of(m);
					ca = a;
				}
			}

			methodCache.put(argsSig, p);
		}

		return p;
	}
}

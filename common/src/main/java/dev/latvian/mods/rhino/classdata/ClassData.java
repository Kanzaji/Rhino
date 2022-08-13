package dev.latvian.mods.rhino.classdata;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.ContextJS;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.SharedContextData;
import dev.latvian.mods.rhino.js.JavaObjectJS;
import dev.latvian.mods.rhino.js.TypeJS;
import dev.latvian.mods.rhino.js.UndefinedJS;
import dev.latvian.mods.rhino.js.prototype.MemberFunctions;
import dev.latvian.mods.rhino.js.prototype.PrototypeJS;
import dev.latvian.mods.rhino.js.prototype.WithPrototype;
import dev.latvian.mods.rhino.util.Possible;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ClassData implements WithPrototype, MemberFunctions {
	public static ClassData of(Context cx, Scriptable scope, Class<?> type) {
		return cx.getSharedData(scope).getClassDataCache().of(type);
	}

	public final ClassDataCache cache;
	public final PublicClassData publicClassData;
	private PrototypeJS prototype;
	private Map<String, BaseMember> members;
	private Map<String, BaseMember> staticMembers;
	private Map<MethodSignature, ConstructorInfo> constructors;
	private Map<MethodSignature, Possible<ConstructorInfo>> constructorCache;

	ClassData(ClassDataCache c, PublicClassData d) {
		cache = c;
		publicClassData = d;
	}

	private MemberGroup make(Map<String, MemberGroup> map, String name) {
		MemberGroup m = map.get(name);

		if (m == null) {
			m = new MemberGroup(name);
			map.put(name, m);
		}

		return m;
	}

	public Map<String, BaseMember> getMembers(boolean isStatic) {
		var m = isStatic ? staticMembers : members;

		if (m == null) {
			if (publicClassData == PublicClassData.OBJECT) {
				m = Map.of();

				if (isStatic) {
					staticMembers = m;
				} else {
					members = m;
				}

				return m;
			}

			var m1 = new HashMap<String, MemberGroup>();

			for (var field : publicClassData.getFields()) {
				String n = cache.sharedData.getRemapper().getMappedField(cache.sharedData, publicClassData, field);
				var cm = make(m1, n);
				cm.field = field;
			}

			for (var method : publicClassData.getMethods()) {
				String n = cache.sharedData.getRemapper().getMappedMethod(cache.sharedData, publicClassData, method);
				var cm = make(m1, n);

				if (method.signature.isEmpty()) {
					cm.noArgMethod = method;
				} else {
					if (cm.methods == null) {
						cm.methods = new HashMap<>();
					}

					cm.methods.put(method.signature, method);
				}

				if (method.signature.types.length == 0 && n.length() >= 4 && !method.isVoid() && Character.isUpperCase(n.charAt(3)) && n.startsWith("get")) {
					make(m1, n.substring(3, 4).toLowerCase() + n.substring(4)).beanGet = method;
				} else if (method.signature.types.length == 1 && n.length() >= 4 && Character.isUpperCase(n.charAt(3)) && n.startsWith("set")) {
					make(m1, n.substring(3, 4).toLowerCase() + n.substring(4)).beanSet = method;
				} else if (method.signature.types.length == 0 && n.length() >= 3 && method.isBoolean() && Character.isUpperCase(n.charAt(2)) && n.startsWith("is")) {
					make(m1, n.substring(2, 3).toLowerCase() + n.substring(3)).beanGet = method;
				}
			}

			m = new HashMap<>();

			for (var member : m1.values()) {
				member.optimize(m);
			}

			m = ClassDataCache.optimizeMap(m);

			if (isStatic) {
				staticMembers = m;
			} else {
				members = m;
			}
		}

		return m;
	}

	@Nullable
	public BaseMember getMember(String name, boolean isStatic) {
		var m = getMembers(isStatic).get(name);
		return m == null && !isStatic ? getMembers(true).get(name) : m;
	}

	public Map<MethodSignature, ConstructorInfo> getConstructors() {
		if (constructors == null) {
			constructors = new HashMap<>();
		}

		return constructors;
	}

	public Possible<ConstructorInfo> constructor(SharedContextData data, Object[] args, MethodSignature argsSig) {
		var ctors = getConstructors();

		if (ctors.isEmpty()) {
			return Possible.absent();
		}

		if (constructorCache == null) {
			constructorCache = new HashMap<>();
		}

		var p = constructorCache.get(argsSig);

		if (p == null) {
			p = Possible.absent();
			int ca = -1;

			for (var m : ctors.values()) {
				int a = m.signature.matches(data, args, argsSig);

				if (a > ca) {
					p = Possible.of(m);
					ca = a;
				}
			}

			constructorCache.put(argsSig, p);
		}

		return p;
	}

	@Override
	public String toString() {
		return publicClassData.toString();
	}

	public RuntimeException reportMemberNotFound(Context cx, String memberName) {
		return Context.reportRuntimeError2(cx, "msg.java.member.not.found", toString(), memberName);
	}

	@Override
	public Object getValue(ContextJS cx, @Nullable Object self, Object key) {
		var m = getMember(key.toString(), self == null);

		if (m != null) {
			return m.getJS(cx, self);
		}

		return UndefinedJS.PROTOTYPE;
	}

	@Override
	public boolean setValue(ContextJS cx, @Nullable Object self, Object key, Object value) {
		var m = getMember(key.toString(), self == null);

		if (m != null) {
			m.setJS(cx, self, value);
			return true;
		}

		return false;
	}

	@Override
	public Object invoke(ContextJS cx, @Nullable Object self, Object key, Object[] args) {
		var m = getMember(key.toString(), self == null);

		if (m != null) {
			m.invokeJS(cx, self, args);
		}

		return UndefinedJS.PROTOTYPE;
	}

	@Override
	public boolean deleteValue(ContextJS cx, @Nullable Object self, Object key) {
		return false;
	}

	@Override
	public PrototypeJS getPrototype(ContextJS cx) {
		if (prototype == null) {
			String name = publicClassData.toString();

			int idx = name.lastIndexOf('$');

			if (idx != -1) {
				name = name.substring(idx + 1);
			}

			idx = name.lastIndexOf('.');

			if (idx != -1) {
				name = name.substring(idx + 1);
			}

			prototype = JavaObjectJS.PROTOTYPE.create(TypeJS.OBJECT, name).memberFunctions(this);
		}

		return prototype;
	}
}

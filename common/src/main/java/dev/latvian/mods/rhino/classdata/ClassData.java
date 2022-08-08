package dev.latvian.mods.rhino.classdata;

import dev.latvian.mods.rhino.util.HideFromJS;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassData {
	public final ClassDataCache cache;
	public final Class<?> type;
	private List<ClassData> parents;
	private Map<String, ClassMember> members;
	private Map<MethodSignature, Constructor<?>> constructors;
	private List<String> remapPrefixes;

	ClassData(ClassDataCache c, Class<?> t) {
		cache = c;
		type = t;
	}

	public List<ClassData> getParents() {
		if (parents == null) {
			parents = new ArrayList<>();

			if (this != cache.objectClassData && !type.isPrimitive()) {
				var s = type.getSuperclass();

				if (s != null && s != Object.class) {
					parents.add(cache.of(s));
				}

				for (var i : type.getInterfaces()) {
					parents.add(cache.of(i));
				}
			}

			parents = ClassDataCache.optimizeList(parents);
		}

		return parents;
	}

	private ClassMember make(String name) {
		ClassMember m = members.get(name);

		if (m == null) {
			m = new ClassMember(this, name);
			members.put(name, m);
		}

		return m;
	}

	private Map<String, ClassMember> getMembers() {
		if (members == null) {
			if (type == Object.class) {
				members = Map.of();
				return members;
			}

			members = new HashMap<>();

			for (Field field : type.getFields()) {
				int m = field.getModifiers();

				if (!Modifier.isTransient(m) && !field.isAnnotationPresent(HideFromJS.class)) {
					String n = cache.sharedData.getRemapper().getMappedField(cache.sharedData, type, field);
					var cm = make(n);
					cm.field = field;
					cm.isFinal = Modifier.isFinal(m);
				}
			}

			for (Method method : type.getMethods()) {
				int m = method.getModifiers();

				if (!Modifier.isNative(m) && method.getDeclaringClass() != Object.class) {
					String n = cache.sharedData.getRemapper().getMappedMethod(cache.sharedData, type, method);
					var cm = make(n);

					if (cm.methods == null) {
						cm.methods = new HashMap<>();
					}

					MethodInfo mi = new MethodInfo();
					mi.method = method;
					mi.signature = MethodSignature.of(method.getParameterTypes());
					cm.methods.put(mi.signature, mi);

					mi.isHidden = method.isAnnotationPresent(HideFromJS.class);

					if (mi.isHidden) {
						continue;
					}

					if (mi.signature.types.length == 0 && n.length() >= 4 && !isVoid(method.getReturnType()) && Character.isUpperCase(n.charAt(3)) && n.startsWith("get")) {
						mi.bean = n.substring(3, 4).toLowerCase() + n.substring(4);
						make(mi.bean).beanGet = mi;
					} else if (mi.signature.types.length == 1 && n.length() >= 4 && Character.isUpperCase(n.charAt(3)) && n.startsWith("set")) {
						mi.bean = n.substring(3, 4).toLowerCase() + n.substring(4);
						make(mi.bean).beanSet = mi;
					} else if (mi.signature.types.length == 0 && n.length() >= 3 && isBoolean(method.getReturnType()) && Character.isUpperCase(n.charAt(2)) && n.startsWith("is")) {
						mi.bean = n.substring(2, 3).toLowerCase() + n.substring(3);
						make(mi.bean).beanGet = mi;
					}
				}
			}

			if (members.isEmpty()) {
				members = Map.of();
			}
		}

		return members;
	}

	private static boolean isVoid(Class<?> c) {
		return c == void.class || c == Void.class;
	}

	private static boolean isBoolean(Class<?> c) {
		return c == boolean.class || c == Boolean.class;
	}

	@Nullable
	public ClassMember getMember(String name) {
		return getMembers().get(name);
	}

	@Nullable
	public Constructor<?> getConstructor(MethodSignature sig) {
		if (constructors == null) {
			constructors = new HashMap<>();

			for (Constructor<?> c : type.getConstructors()) {
				if (!c.isAnnotationPresent(HideFromJS.class)) {
					constructors.put(MethodSignature.of(c.getParameterTypes()), c);
				}
			}
		}

		return constructors.get(sig);
	}

	public List<String> getRemapPrefixes() {
		if (remapPrefixes == null) {
			remapPrefixes = new ArrayList<>();

			for (var a : type.getAnnotationsByType(RemapPrefixForJS.class)) {
				remapPrefixes.add(a.value());
			}

			remapPrefixes = ClassDataCache.optimizeList(remapPrefixes);
		}

		return remapPrefixes;
	}

	@Override
	public String toString() {
		return type.toString();
	}
}

package dev.latvian.mods.rhino.classdata;

import dev.latvian.mods.rhino.ScriptRuntime;
import dev.latvian.mods.rhino.js.AsJS;
import dev.latvian.mods.rhino.js.JavaClassJS;
import dev.latvian.mods.rhino.js.ObjectJS;
import dev.latvian.mods.rhino.util.HideFromJS;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PublicClassData implements AsJS {
	public static final Set<Class<?>> EXCLUDED_PARENT_CLASSES = Set.of(Object.class, Comparable.class);
	public static final PublicClassData[] EMPTY_ARRAY = new PublicClassData[0];

	public static final PublicClassData OBJECT = new PublicClassData(Object.class);
	public static final PublicClassData OBJECT_ARRAY = new PublicClassData(Object[].class);
	public static final PublicClassData CLASS = new PublicClassData(Class.class);
	private static final Object LOCK = new Object();
	private static final Map<Class<?>, PublicClassData> CACHE = new HashMap<>();

	public static PublicClassData of(Class<?> type) {
		if (type == null || type == Object.class) {
			return OBJECT;
		}

		synchronized (LOCK) {
			PublicClassData d = CACHE.get(type);

			if (d == null) {
				d = new PublicClassData(type);
				CACHE.put(type, d);
			}

			return d;
		}
	}

	public final Class<?> type;
	public final boolean isHidden;
	private String name;
	private PublicClassData[] parents;
	private ConstructorInfo[] constructors;
	private FieldInfo[] fields;
	private MethodInfo[] methods;
	private MethodInfo[] declaredMethods;

	private PublicClassData(Class<?> type) {
		this.type = type;
		this.isHidden = type.isAnnotationPresent(HideFromJS.class);
	}

	@Override
	public String toString() {
		if (name == null) {
			name = type.getName();
		}

		return name;
	}

	public PublicClassData[] getParents() {
		if (parents == null) {
			var parentList = new ArrayList<PublicClassData>();
			var stack = new ArrayDeque<Class<?>>();
			var stackSet = new HashSet<Class<?>>();
			stack.add(type);

			while (!stack.isEmpty()) {
				var current = stack.pop();
				parentList.add(of(current));

				for (var iface : current.getInterfaces()) {
					if (!EXCLUDED_PARENT_CLASSES.contains(iface) && stackSet.add(iface)) {
						stack.add(iface);
					}
				}

				var parent = current.getSuperclass();

				if (parent != null && !EXCLUDED_PARENT_CLASSES.contains(parent) && stackSet.add(parent)) {
					stack.add(parent);
				}
			}

			parentList.remove(0);
			parents = parentList.toArray(PublicClassData.EMPTY_ARRAY);
		}

		return parents;
	}

	public boolean isInterface() {
		return Modifier.isInterface(type.getModifiers());
	}

	public boolean isAbstract() {
		return Modifier.isAbstract(type.getModifiers());
	}

	private void initMembers() {
		if (fields != null) {
			return;
		}

		var fieldList = new ArrayList<FieldInfo>();
		var methodList = new ArrayList<MethodInfo>();

		var remapPrefixSet = new HashSet<String>();
		var parents = getParents();

		for (var p : parents) {
			for (var a : p.type.getAnnotationsByType(RemapPrefixForJS.class)) {
				remapPrefixSet.add(a.value());
			}
		}

		var remapPrefixes = remapPrefixSet.toArray(ScriptRuntime.EMPTY_STRINGS);

		for (var field : type.getFields()) {
			int m = field.getModifiers();

			if (!Modifier.isTransient(m) && !field.isAnnotationPresent(HideFromJS.class)) {
				fieldList.add(new FieldInfo(field, remapPrefixes));
			}
		}

		MutableBoolean hidden = new MutableBoolean(false);

		for (var method : type.getMethods()) {
			if (!Modifier.isNative(method.getModifiers()) && !EXCLUDED_PARENT_CLASSES.contains(method.getDeclaringClass())) {
				hidden.setFalse();
				var mi = new MethodInfo(method, hidden, parents, remapPrefixes);

				if (hidden.isFalse()) {
					methodList.add(mi);
				}
			}
		}

		fields = fieldList.isEmpty() ? FieldInfo.EMPTY_ARRAY : fieldList.toArray(FieldInfo.EMPTY_ARRAY);
		methods = methodList.isEmpty() ? MethodInfo.EMPTY_ARRAY : methodList.toArray(MethodInfo.EMPTY_ARRAY);
	}

	public ConstructorInfo[] getConstructors() {
		if (constructors == null) {
			var constructorList = new ArrayList<ConstructorInfo>();

			for (Constructor<?> c : type.getConstructors()) {
				if (!c.isAnnotationPresent(HideFromJS.class)) {
					constructorList.add(new ConstructorInfo(c));
				}
			}

			constructors = constructorList.isEmpty() ? ConstructorInfo.EMPTY_ARRAY : constructorList.toArray(ConstructorInfo.EMPTY_ARRAY);
		}

		return constructors;
	}

	public FieldInfo[] getFields() {
		if (fields == null) {
			initMembers();
		}

		return fields;
	}

	public MethodInfo[] getMethods() {
		if (methods == null) {
			initMembers();
		}

		return methods;
	}

	public MethodInfo[] getDeclaredMethods() {
		if (declaredMethods == null) {
			List<MethodInfo> declaredMethodList = new ArrayList<>();

			for (var m : getMethods()) {
				if (m.getDeclaringClass() == type) {
					declaredMethodList.add(m);
				}
			}

			declaredMethods = declaredMethodList.isEmpty() ? MethodInfo.EMPTY_ARRAY : declaredMethodList.toArray(MethodInfo.EMPTY_ARRAY);
		}

		return declaredMethods;
	}

	@Override
	public ObjectJS asJS() {
		return new JavaClassJS(type);
	}
}

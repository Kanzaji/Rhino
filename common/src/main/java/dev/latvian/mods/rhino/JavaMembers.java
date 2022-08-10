/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

/**
 * @author Mike Shaver
 * @author Norris Boyd
 * @see NativeJavaObject
 * @see NativeJavaClass
 */
class JavaMembers {
	/*
	private final Class<?> cl;
	private final Map<String, Object> members;
	private Map<String, FieldAndMethods> fieldAndMethods;
	private final Map<String, Object> staticMembers;
	private Map<String, FieldAndMethods> staticFieldAndMethods;
	NativeJavaMethod ctors; // we use NativeJavaMethod for ctor overload resolution

	JavaMembers(Scriptable scope, Class<?> cl) {
		this(scope, cl, false);
	}

	JavaMembers(Scriptable scope, Class<?> cl, boolean includeProtected) {
		try {
			Context cx = ContextFactory.getGlobal().enterContext();
			ClassShutter shutter = cx.getClassShutter();
			if (shutter != null && !shutter.visibleToScripts(cl.getName(), ClassShutter.TYPE_MEMBER)) {
				throw Context.reportRuntimeError1(Context.getCurrentContext(), "msg.access.prohibited", cl.getName());
			}
			this.members = new HashMap<>();
			this.staticMembers = new HashMap<>();
			this.cl = cl;
			boolean includePrivate = cx.hasFeature(Context.FEATURE_ENHANCED_JAVA_ACCESS);
			reflect(cx, scope, includeProtected, includePrivate);
		} finally {
			Context.exit();
		}
	}

	public boolean has(String name, boolean isStatic) {
		Map<String, Object> ht = isStatic ? staticMembers : members;
		Object obj = ht.get(name);
		if (obj != null) {
			return true;
		}
		return findExplicitFunction(name, isStatic) != null;
	}

	public Object get(Context cx, Scriptable scope, String name, Object javaObject, boolean isStatic) {
		Map<String, Object> ht = isStatic ? staticMembers : members;
		Object member = ht.get(name);
		if (!isStatic && member == null) {
			// Try to get static member from instance (LC3)
			member = staticMembers.get(name);
		}
		if (member == null) {
			member = this.getExplicitFunction(cx, scope, name, javaObject, isStatic);
			if (member == null) {
				return Scriptable.NOT_FOUND;
			}
		}
		if (member instanceof Scriptable) {
			return member;
		}
		Object rval;
		Class<?> type;
		try {
			if (member instanceof BeanProperty bp) {
				if (bp.getter == null) {
					return Scriptable.NOT_FOUND;
				}
				rval = bp.getter.invoke(javaObject, ScriptRuntime.EMPTY_ARGS);
				type = bp.getter.method().getReturnType();
			} else {
				Field field = (Field) member;
				rval = field.get(isStatic ? null : javaObject);
				type = field.getType();
			}
		} catch (Exception ex) {
			throw Context.throwAsScriptRuntimeEx(ex);
		}
		// Need to wrap the object before we return it.
		scope = ScriptableObject.getTopLevelScope(scope);
		return cx.getWrapFactory().wrap(cx, scope, rval, type);
	}

	public void put(Scriptable scope, String name, Object javaObject, Object value, boolean isStatic) {
		Map<String, Object> ht = isStatic ? staticMembers : members;
		Object member = ht.get(name);
		if (!isStatic && member == null) {
			// Try to get static member from instance (LC3)
			member = staticMembers.get(name);
		}
		if (member == null) {
			throw reportMemberNotFound(name);
		}
		if (member instanceof FieldAndMethods) {
			FieldAndMethods fam = (FieldAndMethods) ht.get(name);
			member = fam.field;
		}

		Context cx = Context.getContext();

		// Is this a bean property "set"?
		if (member instanceof BeanProperty bp) {
			if (bp.setter == null) {
				throw reportMemberNotFound(name);
			}
			// If there's only one setter or if the value is null, use the
			// main setter. Otherwise, let the NativeJavaMethod decide which
			// setter to use:
			if (bp.setters == null || value == null) {
				Class<?> setType = bp.setter.argTypes[0];
				Object[] args = {Context.jsToJava(cx, value, setType)};
				try {
					bp.setter.invoke(javaObject, args);
				} catch (Exception ex) {
					throw Context.throwAsScriptRuntimeEx(ex);
				}
			} else {
				Object[] args = {value};
				bp.setters.call(cx, ScriptableObject.getTopLevelScope(scope), scope, args);
			}
		} else {
			if (!(member instanceof Field field)) {
				String str = (member == null) ? "msg.java.internal.private" : "msg.java.method.assign";
				throw Context.reportRuntimeError1(cx, str, name);
			}
			int fieldModifiers = field.getModifiers();

			if (Modifier.isFinal(fieldModifiers)) {
				// treat Java final the same as JavaScript [[READONLY]]
				throw Context.throwAsScriptRuntimeEx(new IllegalAccessException("Can't modify final field " + field.getName()));
			}

			Object javaValue = Context.jsToJava(cx, value, field.getType());
			try {
				field.set(javaObject, javaValue);
			} catch (IllegalAccessException accessEx) {
				throw Context.throwAsScriptRuntimeEx(accessEx);
			} catch (IllegalArgumentException argEx) {
				throw Context.reportRuntimeError3(cx, "msg.java.internal.field.type", value.getClass().getName(), field, javaObject.getClass().getName());
			}
		}
	}

	public Object[] getIds(boolean isStatic) {
		Map<String, Object> map = isStatic ? staticMembers : members;
		return map.keySet().toArray(ScriptRuntime.EMPTY_ARGS);
	}

	public static String javaSignature(Class<?> type) {
		if (!type.isArray()) {
			return type.getName();
		}
		int arrayDimension = 0;
		do {
			++arrayDimension;
			type = type.getComponentType();
		} while (type.isArray());
		String name = type.getName();
		String suffix = "[]";
		if (arrayDimension == 1) {
			return name.concat(suffix);
		}
		int length = name.length() + arrayDimension * suffix.length();
		StringBuilder sb = new StringBuilder(length);
		sb.append(name);
		while (arrayDimension != 0) {
			--arrayDimension;
			sb.append(suffix);
		}
		return sb.toString();
	}

	public static String liveConnectSignature(Class<?>[] argTypes) {
		int N = argTypes.length;
		if (N == 0) {
			return "()";
		}
		StringBuilder sb = new StringBuilder();
		sb.append('(');
		for (int i = 0; i != N; ++i) {
			if (i != 0) {
				sb.append(',');
			}
			sb.append(javaSignature(argTypes[i]));
		}
		sb.append(')');
		return sb.toString();
	}

	private MemberBox findExplicitFunction(String name, boolean isStatic) {
		int sigStart = name.indexOf('(');
		if (sigStart < 0) {
			return null;
		}

		Map<String, Object> ht = isStatic ? staticMembers : members;
		MemberBox[] methodsOrCtors = null;
		boolean isCtor = (isStatic && sigStart == 0);

		if (isCtor) {
			// Explicit request for an overloaded constructor
			methodsOrCtors = ctors.methods;
		} else {
			// Explicit request for an overloaded method
			String trueName = name.substring(0, sigStart);
			Object obj = ht.get(trueName);
			if (!isStatic && obj == null) {
				// Try to get static member from instance (LC3)
				obj = staticMembers.get(trueName);
			}
			if (obj instanceof NativeJavaMethod njm) {
				methodsOrCtors = njm.methods;
			}
		}

		if (methodsOrCtors != null) {
			for (MemberBox methodsOrCtor : methodsOrCtors) {
				Class<?>[] type = methodsOrCtor.argTypes;
				String sig = liveConnectSignature(type);
				if (sigStart + sig.length() == name.length() && name.regionMatches(sigStart, sig, 0, sig.length())) {
					return methodsOrCtor;
				}
			}
		}

		return null;
	}

	private Object getExplicitFunction(Context cx, Scriptable scope, String name, Object javaObject, boolean isStatic) {
		Map<String, Object> ht = isStatic ? staticMembers : members;
		Object member = null;
		MemberBox methodOrCtor = findExplicitFunction(name, isStatic);

		if (methodOrCtor != null) {
			Scriptable prototype = ScriptableObject.getFunctionPrototype(cx, scope);

			if (methodOrCtor.isCtor()) {
				NativeJavaConstructor fun = new NativeJavaConstructor(methodOrCtor);
				fun.setPrototype(cx, prototype);
				member = fun;
				ht.put(name, fun);
			} else {
				String trueName = methodOrCtor.getName();
				member = ht.get(trueName);

				if (member instanceof NativeJavaMethod && ((NativeJavaMethod) member).methods.length > 1) {
					NativeJavaMethod fun = new NativeJavaMethod(methodOrCtor, name);
					fun.setPrototype(cx, prototype);
					ht.put(name, fun);
					member = fun;
				}
			}
		}

		return member;
	}

	public static Map<MethodSignature, Method> discoverAccessibleMethods(Class<?> clazz, boolean includeProtected, boolean includePrivate) {
		Map<MethodSignature, Method> map = new HashMap<>();
		discoverAccessibleMethods(clazz, map, includeProtected, includePrivate);
		return map;
	}

	private static void discoverAccessibleMethods(Class<?> clazz, Map<MethodSignature, Method> map, boolean includeProtected, boolean includePrivate) {
		if ((isPublic(clazz.getModifiers()) || includePrivate) && !clazz.isAnnotationPresent(HideFromJS.class)) {
			try {
				if (includeProtected || includePrivate) {
					while (clazz != null) {
						try {
							Method[] methods = clazz.getDeclaredMethods();
							for (Method method : methods) {
								if (method.isAnnotationPresent(HideFromJS.class)) {
									continue;
								}

								int mods = method.getModifiers();

								if (isPublic(mods) || isProtected(mods) || includePrivate) {
									if (includePrivate && !method.isAccessible()) {
										method.setAccessible(true);
									}

									addMethod(map, method);
								}
							}
							Class<?>[] interfaces = clazz.getInterfaces();
							for (Class<?> intface : interfaces) {
								discoverAccessibleMethods(intface, map, includeProtected, includePrivate);
							}
							clazz = clazz.getSuperclass();
						} catch (SecurityException e) {
							// Some security settings (i.e., applets) disallow
							// access to Class.getDeclaredMethods. Fall back to
							// Class.getMethods.
							Method[] methods = clazz.getMethods();
							for (Method method : methods) {
								addMethod(map, method);
							}
							break; // getMethods gets superclass methods, no
							// need to loop any more
						}
					}
				} else {
					Method[] methods = clazz.getMethods();
					for (Method method : methods) {
						addMethod(map, method);
					}
				}
				return;
			} catch (SecurityException e) {
				Context.reportWarning(Context.getCurrentContext(), "Could not discover accessible methods of class " + clazz.getName() + " due to lack of privileges, " + "attemping superclasses/interfaces.");
				// Fall through and attempt to discover superclass/interface
				// methods
			}
		}

		Class<?>[] interfaces = clazz.getInterfaces();
		for (Class<?> intface : interfaces) {
			discoverAccessibleMethods(intface, map, includeProtected, includePrivate);
		}
		Class<?> superclass = clazz.getSuperclass();
		if (superclass != null) {
			discoverAccessibleMethods(superclass, map, includeProtected, includePrivate);
		}
	}

	private static void addMethod(Map<MethodSignature, Method> map, Method method) throws SecurityException {
		if (!method.isAnnotationPresent(HideFromJS.class)) {
			MethodSignature sig = new MethodSignature(method.getName(), method.getParameterCount() == 0 ? MethodSignature.NO_ARGS : method.getParameterTypes());
			// Array may contain methods with same signature but different return value!
			if (!map.containsKey(sig)) {
				map.put(sig, method);
			}
		}
	}

	public record MethodSignature(String name, Class<?>[] args) {
		private static final Class<?>[] NO_ARGS = new Class<?>[0];

		@Override
		public boolean equals(Object o) {
			if (o instanceof MethodSignature ms) {
				return ms.name.equals(name) && Arrays.equals(args, ms.args);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return name.hashCode() ^ args.length;
		}
	}

	private void reflect(Context cx, Scriptable scope, boolean includeProtected, boolean includePrivate) {
		if (cl.isAnnotationPresent(HideFromJS.class)) {
			ctors = new NativeJavaMethod(new MemberBox[0], cl.getSimpleName());
			return;
		}

		// We reflect methods first, because we want overloaded field/method
		// names to be allocated to the NativeJavaMethod before the field
		// gets in the way.

		for (Method method : discoverAccessibleMethods(cl, includeProtected, includePrivate).values()) {
			int mods = method.getModifiers();
			boolean isStatic = Modifier.isStatic(mods);
			Map<String, Object> ht = isStatic ? staticMembers : members;
			String name = cx.getSharedData().getRemapper().getMappedMethod(cx.getSharedData(), cl, method);

			Object value = ht.get(name);
			if (value == null) {
				ht.put(name, method);
			} else {
				ObjArray overloadedMethods;
				if (value instanceof ObjArray) {
					overloadedMethods = (ObjArray) value;
				} else {
					if (!(value instanceof Method)) {
						throw Kit.codeBug();
					}
					// value should be instance of Method as at this stage
					// staticMembers and members can only contain methods
					overloadedMethods = new ObjArray();
					overloadedMethods.add(value);
					ht.put(name, overloadedMethods);
				}
				overloadedMethods.add(method);
			}
		}

		// replace Method instances by wrapped NativeJavaMethod objects
		// first in staticMembers and then in members
		for (int tableCursor = 0; tableCursor != 2; ++tableCursor) {
			boolean isStatic = (tableCursor == 0);
			Map<String, Object> ht = isStatic ? staticMembers : members;
			for (Map.Entry<String, Object> entry : ht.entrySet()) {
				MemberBox[] methodBoxes;
				Object value = entry.getValue();
				if (value instanceof Method) {
					methodBoxes = new MemberBox[1];
					methodBoxes[0] = new MemberBox((Method) value);
				} else {
					ObjArray overloadedMethods = (ObjArray) value;
					int N = overloadedMethods.size();
					if (N < 2) {
						throw Kit.codeBug();
					}
					methodBoxes = new MemberBox[N];
					for (int i = 0; i != N; ++i) {
						Method method = (Method) overloadedMethods.get(i);
						methodBoxes[i] = new MemberBox(method);
					}
				}
				NativeJavaMethod fun = new NativeJavaMethod(methodBoxes);
				if (scope != null) {
					ScriptRuntime.setFunctionProtoAndParent(cx, fun, scope);
				}
				ht.put(entry.getKey(), fun);
			}
		}

		// Reflect fields.
		for (Field field : getAccessibleFields(includeProtected, includePrivate)) {
			String name = cx.getSharedData().getRemapper().getMappedField(cx.getSharedData(), cl, field);

			int mods = field.getModifiers();
			try {
				boolean isStatic = Modifier.isStatic(mods);
				Map<String, Object> ht = isStatic ? staticMembers : members;
				Object member = ht.get(name);
				if (member == null) {
					ht.put(name, field);
				} else if (member instanceof NativeJavaMethod method) {
					FieldAndMethods fam = new FieldAndMethods(cx, scope, method.methods, field);
					Map<String, FieldAndMethods> fmht = isStatic ? staticFieldAndMethods : fieldAndMethods;
					if (fmht == null) {
						fmht = new HashMap<>();
						if (isStatic) {
							staticFieldAndMethods = fmht;
						} else {
							fieldAndMethods = fmht;
						}
					}
					fmht.put(name, fam);
					ht.put(name, fam);
				} else if (member instanceof Field oldField) {
					// If this newly reflected field shadows an inherited field,
					// then replace it. Otherwise, since access to the field
					// would be ambiguous from Java, no field should be
					// reflected.
					// For now, the first field found wins, unless another field
					// explicitly shadows it.
					if (oldField.getDeclaringClass().isAssignableFrom(field.getDeclaringClass())) {
						ht.put(name, field);
					}
				} else {
					// "unknown member type"
					throw Kit.codeBug();
				}
			} catch (SecurityException e) {
				// skip this field
				Context.reportWarning(cx, "Could not access field " + name + " of class " + cl.getName() + " due to lack of privileges.");
			}
		}

		// Create bean properties from corresponding get/set methods first for
		// static members and then for instance members
		for (int tableCursor = 0; tableCursor != 2; ++tableCursor) {
			boolean isStatic = (tableCursor == 0);
			Map<String, Object> ht = isStatic ? staticMembers : members;

			Map<String, BeanProperty> toAdd = new HashMap<>();

			// Now, For each member, make "bean" properties.
			for (String name : ht.keySet()) {
				// Is this a getter?
				boolean memberIsGetMethod = name.startsWith("get");
				boolean memberIsSetMethod = name.startsWith("set");
				boolean memberIsIsMethod = name.startsWith("is");
				if (memberIsGetMethod || memberIsIsMethod || memberIsSetMethod) {
					// Double check name component.
					String nameComponent = name.substring(memberIsIsMethod ? 2 : 3);
					if (nameComponent.length() == 0) {
						continue;
					}

					// Make the bean property name.
					String beanPropertyName = nameComponent;
					char ch0 = nameComponent.charAt(0);
					if (Character.isUpperCase(ch0)) {
						if (nameComponent.length() == 1) {
							beanPropertyName = nameComponent.toLowerCase();
						} else {
							char ch1 = nameComponent.charAt(1);
							if (!Character.isUpperCase(ch1)) {
								beanPropertyName = Character.toLowerCase(ch0) + nameComponent.substring(1);
							}
						}
					}

					// If we already have a member by this name, don't do this
					// property.
					if (toAdd.containsKey(beanPropertyName)) {
						continue;
					}
					Object v = ht.get(beanPropertyName);
					if (v != null) {
						// A private field shouldn't mask a public getter/setter
						if (!includePrivate || !(v instanceof Member) || !Modifier.isPrivate(((Member) v).getModifiers())) {
							continue;
						}
					}

					// Find the getter method, or if there is none, the is-
					// method.
					MemberBox getter;
					getter = findGetter(isStatic, ht, "get", nameComponent);
					// If there was no valid getter, check for an is- method.
					if (getter == null) {
						getter = findGetter(isStatic, ht, "is", nameComponent);
					}

					// setter
					MemberBox setter = null;
					NativeJavaMethod setters = null;
					String setterName = "set".concat(nameComponent);

					if (ht.containsKey(setterName)) {
						// Is this value a method?
						Object member = ht.get(setterName);
						if (member instanceof NativeJavaMethod njmSet) {
							if (getter != null) {
								// We have a getter. Now, do we have a matching
								// setter?
								Class<?> type = getter.method().getReturnType();
								setter = extractSetMethod(type, njmSet.methods, isStatic);
							} else {
								// No getter, find any set method
								setter = extractSetMethod(njmSet.methods, isStatic);
							}
							if (njmSet.methods.length > 1) {
								setters = njmSet;
							}
						}
					}
					// Make the property.
					BeanProperty bp = new BeanProperty(getter, setter, setters);
					toAdd.put(beanPropertyName, bp);
				}
			}

			// Add the new bean properties.
			ht.putAll(toAdd);
		}

		// Reflect constructors
		List<Constructor<?>> constructors = getAccessibleConstructors(includePrivate);
		MemberBox[] ctorMembers = new MemberBox[constructors.size()];
		for (int i = 0; i != constructors.size(); ++i) {
			ctorMembers[i] = new MemberBox(constructors.get(i));
		}
		ctors = new NativeJavaMethod(ctorMembers, cl.getSimpleName());
	}


	RuntimeException reportMemberNotFound(String memberName) {
		return Context.reportRuntimeError2(Context.getCurrentContext(), "msg.java.member.not.found", cl.getName(), memberName);
	}
	*/
}


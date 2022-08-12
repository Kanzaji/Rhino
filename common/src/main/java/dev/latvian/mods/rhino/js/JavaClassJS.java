package dev.latvian.mods.rhino.js;

import dev.latvian.mods.rhino.ContextJS;
import dev.latvian.mods.rhino.js.prototype.PrototypeJS;

public class JavaClassJS {
	public static PrototypeJS PROTOTYPE = JavaObjectJS.PROTOTYPE.create("JavaClass")
			.staticPropertyValue("OBJECT", Object.class)
			.staticPropertyValue("CLASS", Class.class)
			.staticPropertyValue("STRING", String.class)
			.staticPropertyValue("CHAR", Character.class)
			.staticPropertyValue("BYTE", Byte.class)
			.staticPropertyValue("SHORT", Short.class)
			.staticPropertyValue("INT", Integer.class)
			.staticPropertyValue("LONG", Long.class)
			.staticPropertyValue("FLOAT", Float.class)
			.staticPropertyValue("DOUBLE", Double.class)
			.staticPropertyValue("BOOLEAN", Boolean.class)
			.staticPropertyValue("VOID", Void.class)
			.staticPropertyValue("PRIMITIVE_CHAR", Character.TYPE)
			.staticPropertyValue("PRIMITIVE_BYTE", Byte.TYPE)
			.staticPropertyValue("PRIMITIVE_SHORT", Short.TYPE)
			.staticPropertyValue("PRIMITIVE_INT", Integer.TYPE)
			.staticPropertyValue("PRIMITIVE_LONG", Long.TYPE)
			.staticPropertyValue("PRIMITIVE_FLOAT", Float.TYPE)
			.staticPropertyValue("PRIMITIVE_DOUBLE", Double.TYPE)
			.staticPropertyValue("PRIMITIVE_BOOLEAN", Boolean.TYPE)
			.staticPropertyValue("PRIMITIVE_VOID", Void.TYPE)
			.property("name", JavaClassJS::name)
			.property("simpleName", JavaClassJS::simpleName)
			.property("superclass", JavaClassJS::superclass)
			.property("package", JavaClassJS::getPackage)
			.property("interface", JavaClassJS::isInterface)
			.property("array", JavaClassJS::isArray)
			.property("primitive", JavaClassJS::isPrimitive)
			.property("annotation", JavaClassJS::isAnnotation)
			.property("synthetic", JavaClassJS::isSynthetic)
			.property("componentType", JavaClassJS::getComponentType)
			.property("interfaces", JavaClassJS::getInterfaces)
			.function("isAssignableFrom", JavaClassJS::isAssignableFrom);

	private static Object name(ContextJS cx, Object self) {
		return ((Class<?>) self).getName();
	}

	private static Object simpleName(ContextJS cx, Object self) {
		return ((Class<?>) self).getSimpleName();
	}

	private static Object superclass(ContextJS cx, Object self) {
		return ((Class<?>) self).getSuperclass();
	}

	private static Object getPackage(ContextJS cx, Object self) {
		return ((Class<?>) self).getPackageName();
	}

	private static Object isInterface(ContextJS cx, Object self) {
		return ((Class<?>) self).isInterface();
	}

	private static Object isArray(ContextJS cx, Object self) {
		return ((Class<?>) self).isArray();
	}

	private static Object isPrimitive(ContextJS cx, Object self) {
		return ((Class<?>) self).isPrimitive();
	}

	private static Object isAnnotation(ContextJS cx, Object self) {
		return ((Class<?>) self).isAnnotation();
	}

	private static Object isSynthetic(ContextJS cx, Object self) {
		return ((Class<?>) self).isSynthetic();
	}

	private static Object getComponentType(ContextJS cx, Object self) {
		return ((Class<?>) self).getComponentType();
	}

	private static Object getInterfaces(ContextJS cx, Object self) {
		return ((Class<?>) self).getInterfaces();
	}

	private static Object isAssignableFrom(ContextJS cx, Object self, Object[] args) {
		if (args[0] instanceof Class<?>) {
			return ((Class<?>) self).isAssignableFrom((Class<?>) args[0]);
		} else {
			try {
				return ((Class<?>) self).isAssignableFrom(Class.forName(String.valueOf(args[0])));
			} catch (ClassNotFoundException e) {
				throw new RuntimeException("Class " + args[0] + " not found");
			}
		}
	}
}

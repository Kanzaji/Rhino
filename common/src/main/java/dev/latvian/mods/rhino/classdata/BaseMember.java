package dev.latvian.mods.rhino.classdata;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.ContextJS;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;

public interface BaseMember {
	default MethodSignature getSignature() {
		throw new MemberMethodNotSupportedException.Signature(this);
	}

	default boolean isMethod() {
		return true;
	}

	default Object get(ContextJS cx, @Nullable Object self) throws Exception {
		throw new MemberMethodNotSupportedException.Get(this);
	}

	default boolean set(ContextJS cx, @Nullable Object self, Object value) throws Exception {
		throw new MemberMethodNotSupportedException.Set(this);
	}

	default Object invoke(ContextJS cx, @Nullable Object self, Object[] args) throws Exception {
		throw new MemberMethodNotSupportedException.Invoke(this);
	}

	default Object getJS(ContextJS cx, @Nullable Object self) {
		try {
			var value = get(cx, self);
			return Context.javaToJS(cx.context, value, cx.getScope());
		} catch (InvocationTargetException ite) {
			// Must allow ContinuationPending exceptions to propagate unhindered
			Throwable e = ite;
			do {
				e = ((InvocationTargetException) e).getTargetException();
			} while ((e instanceof InvocationTargetException));
			throw Context.throwAsScriptRuntimeEx(e);
		} catch (Exception ex) {
			throw Context.throwAsScriptRuntimeEx(ex);
		}
	}

	default void setJS(ContextJS cx, @Nullable Object self, Object value) {
		try {
			set(cx, self, value);
		} catch (InvocationTargetException ite) {
			// Must allow ContinuationPending exceptions to propagate unhindered
			Throwable e = ite;
			do {
				e = ((InvocationTargetException) e).getTargetException();
			} while ((e instanceof InvocationTargetException));
			throw Context.throwAsScriptRuntimeEx(e);
		} catch (Exception ex) {
			throw Context.throwAsScriptRuntimeEx(ex);
		}
	}

	default Object invokeJS(ContextJS cx, @Nullable Object self, Object[] args) {
		try {
			return invoke(cx, self, args);
		} catch (InvocationTargetException ite) {
			// Must allow ContinuationPending exceptions to propagate unhindered
			Throwable e = ite;
			do {
				e = ((InvocationTargetException) e).getTargetException();
			} while ((e instanceof InvocationTargetException));
			throw Context.throwAsScriptRuntimeEx(e);
		} catch (Exception ex) {
			throw Context.throwAsScriptRuntimeEx(ex);
		}
	}

	default boolean isStatic() {
		return true;
	}

	default String getName() {
		return "<unknown>";
	}

	default Class<?> getType() {
		throw new MemberMethodNotSupportedException.Type(this);
	}

	default boolean isVarArgs() {
		throw new MemberMethodNotSupportedException.VarArgs(this);
	}

	default Class<?> getDeclaringClass() {
		throw new MemberMethodNotSupportedException.DeclaringClass(this);
	}
}

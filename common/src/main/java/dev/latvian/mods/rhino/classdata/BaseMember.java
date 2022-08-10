package dev.latvian.mods.rhino.classdata;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.ScriptableObject;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;

public interface BaseMember {
	default MethodSignature getSignature() {
		throw new IllegalStateException("getSignature() is not supported for this member!");
	}

	default boolean isMethod() {
		return true;
	}

	default Object get(Context cx, Scriptable scope, @Nullable Object self) throws Exception {
		throw new MemberMethodNotSupportedException.Get(this);
	}

	default void set(Context cx, Scriptable scope, @Nullable Object self, Object value) throws Exception {
		throw new MemberMethodNotSupportedException.Set(this);
	}

	default Object invoke(Context cx, Scriptable scope, @Nullable Object self, Object[] args, MethodSignature argsSig) throws Exception {
		throw new MemberMethodNotSupportedException.Invoke(this);
	}

	default Object actuallyGet(Context cx, Scriptable scope, @Nullable Object self, Class<?> hint) {
		try {
			var value = get(cx, scope, self);
			scope = ScriptableObject.getTopLevelScope(scope);
			return cx.getWrapFactory().wrap(cx, scope, value, hint);
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

	default void actuallySet(Context cx, Scriptable scope, @Nullable Object self, Object value) {
		try {
			set(cx, scope, self, value);
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

	default Object actuallyInvoke(Context cx, Scriptable scope, @Nullable Object self, Object[] args, MethodSignature argsSig) {
		try {
			return invoke(cx, scope, self, args, argsSig);
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

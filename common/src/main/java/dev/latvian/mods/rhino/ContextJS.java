package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.js.UndefinedJS;

public class ContextJS {
	public final Context context;
	private Scriptable scope;
	private SharedContextData sharedContextData;

	public ContextJS(Context context) {
		this.context = context;
	}

	public ContextJS(Context context, Scriptable scope) {
		this.context = context;
		this.scope = scope;
	}

	public Scriptable getScope() {
		return scope;
	}

	public void setFrame(Interpreter.CallFrame frame) {
		this.scope = frame.scope;
		this.sharedContextData = null;
	}

	public SharedContextData getSharedContextData() {
		if (sharedContextData == null) {
			sharedContextData = context.getSharedData(getScope());
		}

		return sharedContextData;
	}

	public String asString(Object value) {
		if (value == null) {
			return "null";
		} else if (value == UndefinedJS.PROTOTYPE) {
			return "undefined";
		} else if (value instanceof CharSequence) {
			return value.toString();
		} else {
			return ScriptRuntime.toString(context, value);
		}
	}

	public double asNumber(Object value) {
		if (value == null || value == UndefinedJS.PROTOTYPE) {
			return Double.NaN;
		} else if (value instanceof Number) {
			return ((Number) value).doubleValue();
		} else {
			return ScriptRuntime.toNumber(context, value);
		}
	}

	public double asNumber(Object[] args, int index) {
		return index >= args.length ? Double.NaN : asNumber(args[index]);
	}

	public double asInteger(Object value) {
		if (value == null || value == UndefinedJS.PROTOTYPE) {
			return Double.NaN;
		} else if (value instanceof Number) {
			return ((Number) value).longValue();
		} else {
			return ScriptRuntime.toInteger(context, value);
		}
	}

	public boolean asBoolean(Object value) {
		if (value instanceof Boolean b) {
			return b;
		} else {
			return ScriptRuntime.toBoolean(context, value);
		}
	}
}

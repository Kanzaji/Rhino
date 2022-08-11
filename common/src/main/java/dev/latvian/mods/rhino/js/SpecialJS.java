package dev.latvian.mods.rhino.js;

public class SpecialJS extends ObjectJS {
	public static final SpecialJS NULL = new SpecialJS(true);
	public static final SpecialJS NOT_FOUND = new SpecialJS(true);

	private final boolean isNull;

	private SpecialJS(boolean n) {
		isNull = n;
	}

	@Override
	public boolean isNull() {
		return isNull;
	}

	@Override
	public Object unwrap() {
		return null;
	}
}

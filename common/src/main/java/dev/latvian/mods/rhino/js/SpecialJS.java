package dev.latvian.mods.rhino.js;

public class SpecialJS extends ObjectJS {
	public static final SpecialJS NULL = new SpecialJS("null", true);
	public static final SpecialJS UNDEFINED = new SpecialJS("undefined", true);

	private final String name;
	private final boolean isNull;

	private SpecialJS(String name, boolean isNull) {
		this.name = name;
		this.isNull = isNull;
	}

	@Override
	public TypeJS getType() {
		return TypeJS.UNDEFINED;
	}

	@Override
	public Object unwrap() {
		return null;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public boolean isNull() {
		return isNull;
	}

	@Override
	public String asString() {
		return name;
	}

	@Override
	public double asNumber() {
		return 0D;
	}

	@Override
	public boolean asBoolean() {
		return false;
	}

	@Override
	public Object cast(TypeJS type) {
		return null;
	}
}

package dev.latvian.mods.rhino.classdata;

public class MemberMethodNotSupportedException extends IllegalStateException {
	public final BaseMember member;

	private MemberMethodNotSupportedException(BaseMember member, String func) {
		super(func + " is not supported for member '" + member + "'!");
		this.member = member;
	}

	public static class Get extends MemberMethodNotSupportedException {
		public Get(BaseMember member) {
			super(member, "get()");
		}
	}

	public static class Set extends MemberMethodNotSupportedException {
		public Set(BaseMember member) {
			super(member, "set(value)");
		}
	}

	public static class Invoke extends MemberMethodNotSupportedException {
		public Invoke(BaseMember member) {
			super(member, "invoke(args[])");
		}
	}

	public static class Type extends MemberMethodNotSupportedException {
		public Type(BaseMember member) {
			super(member, "getType()");
		}
	}

	public static class VarArgs extends MemberMethodNotSupportedException {
		public VarArgs(BaseMember member) {
			super(member, "isVarArgs()");
		}
	}

	public static class DeclaringClass extends MemberMethodNotSupportedException {
		public DeclaringClass(BaseMember member) {
			super(member, "getDeclaringClass()");
		}
	}
}

package dev.latvian.mods.rhino.classdata;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Member;
import java.lang.reflect.Modifier;

public class MemberInfo implements BaseMember {
	public final Member member;

	MemberInfo(Member member) {
		this.member = member;
	}

	@Override
	public String toString() {
		return member.toString();
	}

	@Override
	public boolean isStatic() {
		return Modifier.isStatic(member.getModifiers());
	}

	public boolean isNative() {
		return Modifier.isNative(member.getModifiers());
	}

	@Override
	public Class<?> getDeclaringClass() {
		return member.getDeclaringClass();
	}

	@Override
	public String getName() {
		return member.getName();
	}

	@Nullable
	protected Object nullIfStatic(@Nullable Object self) {
		return self != null && isStatic() ? null : self;
	}
}
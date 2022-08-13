package dev.latvian.mods.rhino.classdata;

import dev.latvian.mods.rhino.ContextJS;
import dev.latvian.mods.rhino.NativeJavaMethod;
import dev.latvian.mods.rhino.util.Remapper;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Executable;

public class ExecutableInfo extends MemberInfo {
	public final MethodSignature signature;
	private String paramString;

	ExecutableInfo(Executable executable) {
		super(executable);
		signature = MethodSignature.of(executable.getParameterTypes());
	}

	@Override
	public String toString() {
		if (paramString == null) {
			StringBuilder sb = new StringBuilder(member.getName());
			sb.append('(');

			if (!signature.isEmpty()) {
				for (Class<?> param : signature.types) {
					sb.append(Remapper.getTypeName(param.getTypeName()));
				}
			}

			sb.append(')');
			paramString = sb.toString();
		}

		return paramString;
	}

	@Override
	public MethodSignature getSignature() {
		return signature;
	}

	@Override
	public boolean isVarArgs() {
		return ((Executable) member).isVarArgs();
	}

	@Override
	public Object get(ContextJS cx, @Nullable Object self) throws Exception {
		return new NativeJavaMethod(this, self, member.getName());
	}
}
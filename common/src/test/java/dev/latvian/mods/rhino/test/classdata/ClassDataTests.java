package dev.latvian.mods.rhino.test.classdata;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.ScriptRuntime;
import dev.latvian.mods.rhino.classdata.MethodSignature;
import dev.latvian.mods.rhino.mod.util.NBTUtils;
import dev.latvian.mods.rhino.util.HideFromJS;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@SuppressWarnings("unused")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ClassDataTests {
	@Test
	@DisplayName("Class Data")
	public void classData() {
		Context cx = Context.enterWithNewFactory();
		var cache = cx.getSharedData().getClassDataCache();
		var data = cache.of(Player.class);
		var member = data.getMember("x");
		System.out.println(member);
		Context.exit();
	}

	@Test
	@DisplayName("Class Data ambiguous arguments")
	public void ambiguousCassData() {
		Context cx = Context.enterWithNewFactory();
		var typeWrappers = cx.getSharedData().getTypeWrappers();
		typeWrappers.register(CompoundTag.class, NBTUtils::isTagCompound, NBTUtils::toTagCompound);
		var cache = cx.getSharedData().getClassDataCache();
		var data = cache.of(CompoundTag.class);
		var member = data.getMember("merge");
		System.out.println(member);
		Context.exit();

		if (member == null) {
			Assertions.fail("Member is null");
			return;
		}

		Object[] args = ScriptRuntime.unwrapArgs(new LinkedHashMap<>());
		var argsSig = MethodSignature.ofArgs(args);

		var m = member.method(cx.getSharedData(), args, argsSig);
		Assertions.assertTrue(m.isSet());
	}

	@Test
	@DisplayName("Public member test")
	public void publicMemberTest() {
		List<String> members = new ArrayList<>();
		var type = TestClassC.class;

		for (var constructor : type.getConstructors()) {
			if (!constructor.isAnnotationPresent(HideFromJS.class)) {
				members.add(constructor.toString());
			}
		}

		for (var field : type.getFields()) {
			int m = field.getModifiers();

			if (!Modifier.isTransient(m) && !field.isAnnotationPresent(HideFromJS.class)) {
				members.add(field.toString());
			}
		}

		for (var method : type.getMethods()) {
			int m = method.getModifiers();

			if (!Modifier.isNative(m) && method.getDeclaringClass() != Object.class && !method.isAnnotationPresent(HideFromJS.class)) {
				members.add(method.toString());
			}
		}

		System.out.println(members);
	}
}

package dev.latvian.mods.rhino.test.classdata;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.classdata.MethodSignature;
import dev.latvian.mods.rhino.classdata.PublicClassData;
import dev.latvian.mods.rhino.mod.util.NBTUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.ArrayList;
import java.util.Arrays;
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
		var member = data.getMember("x", false);
		System.out.println(member);
		Context.exit();
	}

	@Test
	@DisplayName("Class Data ambiguous arguments")
	public void ambiguousCassData() {
		Context cx = Context.enterWithNewFactory();
		var scope = cx.initStandardObjects();
		var typeWrappers = cx.getSharedData().getTypeWrappers();
		typeWrappers.register(CompoundTag.class, NBTUtils::isTagCompound, NBTUtils::toTagCompound);
		var cache = cx.getSharedData().getClassDataCache();
		var data = cache.of(CompoundTag.class);
		var member = data.getMember("merge", false);
		System.out.println(member);
		Context.exit();

		if (member == null) {
			Assertions.fail("Member is null");
			return;
		}

		Object[] args = MethodSignature.unwrapArgs(cx, new Object[]{new LinkedHashMap<>()}, new Class<?>[]{CompoundTag.class});
		var argsSig = MethodSignature.ofArgs(args);

		// var m = member.method(cx.getSharedData(), args, argsSig);
		// Assertions.assertTrue(m.isSet());
		// member.actuallyInvoke(cx, scope, null)
	}

	@Test
	@DisplayName("PublicClassData test")
	public void publicClassDataTest() {
		List<Object> members = new ArrayList<>();
		var data = PublicClassData.of(TestClassC.class);
		members.addAll(Arrays.asList(data.getConstructors()));
		members.addAll(Arrays.asList(data.getFields()));
		members.addAll(Arrays.asList(data.getMethods()));
		System.out.println(members);
	}
}

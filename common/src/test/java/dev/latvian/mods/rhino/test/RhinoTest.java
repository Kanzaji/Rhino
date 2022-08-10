package dev.latvian.mods.rhino.test;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.NativeJavaClass;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.ScriptableObject;
import dev.latvian.mods.rhino.mod.util.CollectionTagWrapper;
import dev.latvian.mods.rhino.mod.util.CompoundTagWrapper;
import dev.latvian.mods.rhino.mod.util.NBTUtils;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Assertions;

import java.util.HashMap;
import java.util.Map;

public class RhinoTest {
	public final String testName;
	public final Map<String, Object> include;
	private boolean shareScope;
	public Scriptable sharedScope;

	public RhinoTest(String n) {
		testName = n;
		include = new HashMap<>();
		add("console", TestConsole.class);
		add("NBT", NBTUtils.class);
	}

	public RhinoTest add(String name, Object value) {
		include.put(name, value);
		return this;
	}

	public RhinoTest shareScope() {
		shareScope = true;
		return this;
	}

	public void test(String name, String script, String console) {
		var cx = Context.enterWithNewFactory();

		try {
			if (shareScope && sharedScope == null) {
				sharedScope = cx.initStandardObjects();
			}

			var scope = sharedScope != null ? sharedScope : cx.initStandardObjects();
			var sharedData = cx.getSharedData(scope);
			// context.setClassShutter((fullClassName, type) -> type != ClassShutter.TYPE_CLASS_IN_PACKAGE || isClassAllowed(fullClassName));

			var typeWrappers = sharedData.getTypeWrappers();
			typeWrappers.register(CompoundTag.class, NBTUtils::isTagCompound, NBTUtils::toTagCompound);
			typeWrappers.register(CollectionTag.class, NBTUtils::isTagCollection, NBTUtils::toTagCollection);
			typeWrappers.register(ListTag.class, NBTUtils::isTagCollection, NBTUtils::toTagList);
			typeWrappers.register(Tag.class, NBTUtils::toTag);

			sharedData.addCustomJavaToJsWrapper(CompoundTag.class, CompoundTagWrapper::new);
			sharedData.addCustomJavaToJsWrapper(CollectionTag.class, CollectionTagWrapper::new);

			for (var entry : include.entrySet()) {
				if (entry.getValue() instanceof Class<?> c) {
					ScriptableObject.putProperty(cx, scope, entry.getKey(), new NativeJavaClass(cx, scope, c));
				} else {
					ScriptableObject.putProperty(cx, scope, entry.getKey(), Context.javaToJS(entry.getValue(), scope));
				}
			}

			cx.evaluateString(scope, script, testName + "/" + name, 1, null);
		} catch (Exception ex) {
			TestConsole.info("Error: " + ex.getMessage());
			// ex.printStackTrace();
		} finally {
			Context.exit();
		}

		Assertions.assertEquals(console.trim(), TestConsole.getConsoleOutput().trim());
	}
}

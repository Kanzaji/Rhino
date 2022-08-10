package dev.latvian.mods.rhino.test.classdata;

import dev.latvian.mods.rhino.util.RemapPrefixForJS;

@RemapPrefixForJS("modid$")
public interface TestInterfaceA {
	void say(String text);
}

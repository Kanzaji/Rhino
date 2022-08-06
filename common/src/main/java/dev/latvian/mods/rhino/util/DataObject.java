package dev.latvian.mods.rhino.util;

import dev.latvian.mods.rhino.Context;

import java.util.List;
import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public interface DataObject {
	<T> T createDataObject(Context cx, Supplier<T> instanceFactory);

	<T> List<T> createDataObjectList(Context cx, Supplier<T> instanceFactory);

	boolean isDataObjectList();
}

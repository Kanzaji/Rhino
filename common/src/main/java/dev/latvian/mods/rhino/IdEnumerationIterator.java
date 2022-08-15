package dev.latvian.mods.rhino;

import java.util.function.Consumer;

public interface IdEnumerationIterator {
	boolean enumerationIteratorHasNext(ContextJS cx, Consumer<Object> callback);

	boolean enumerationIteratorNext(ContextJS cx, Consumer<Object> callback) throws JavaScriptException;
}

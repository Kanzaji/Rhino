package dev.latvian.mods.rhino.util;

import dev.latvian.mods.rhino.Wrapper;

public interface Deletable {
	void onDeletedByJS();

	static void deleteObject(Object o) {
		if (o instanceof Deletable d) {
			d.onDeletedByJS();
			return;
		}

		Object o1 = o;

		while (o1 instanceof Wrapper w) {
			o1 = w.unwrap();

			if (o1 instanceof Deletable d) {
				d.onDeletedByJS();
				return;
			}
		}
	}
}

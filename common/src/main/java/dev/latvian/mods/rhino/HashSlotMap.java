/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * This class implements the SlotMap interface using a java.util.HashMap. This class has more
 * overhead than EmbeddedSlotMap, especially because it puts each "Slot" inside an intermediate
 * object. However it is much more resistant to large number of hash collisions than
 * EmbeddedSlotMap and therefore we use this implementation when an object gains a large
 * number of properties.
 */

public class HashSlotMap implements SlotMap {

	private final LinkedHashMap<Object, Slot> map = new LinkedHashMap<>();

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public Slot query(Context cx, Object key, int index) {
		Object name = key == null ? String.valueOf(index) : key;
		return map.get(name);
	}

	@Override
	public Slot get(Context cx, Object key, int index, SlotAccess accessType) {
		Object name = key == null ? String.valueOf(index) : key;
		Slot slot = map.get(name);
		switch (accessType) {
			case QUERY:
				return slot;
			case MODIFY:
			case MODIFY_CONST:
				if (slot != null) {
					return slot;
				}
				break;
			case MODIFY_GETTER_SETTER:
				if (slot instanceof GetterSlot) {
					return slot;
				}
				break;
			case CONVERT_ACCESSOR_TO_DATA:
				if (!(slot instanceof GetterSlot)) {
					return slot;
				}
				break;
		}

		return createSlot(cx, key, index, name, accessType);
	}

	private Slot createSlot(Context cx, Object key, int index, Object name, SlotAccess accessType) {
		Slot slot = map.get(name);
		if (slot != null) {
			Slot newSlot;

			if (accessType == SlotAccess.MODIFY_GETTER_SETTER && !(slot instanceof GetterSlot)) {
				newSlot = new GetterSlot(name, slot.indexOrHash, slot.getAttributes());
			} else if (accessType == SlotAccess.CONVERT_ACCESSOR_TO_DATA && (slot instanceof GetterSlot)) {
				newSlot = new Slot(name, slot.indexOrHash, slot.getAttributes());
			} else if (accessType == SlotAccess.MODIFY_CONST) {
				return null;
			} else {
				return slot;
			}
			newSlot.value = slot.value;
			map.put(name, newSlot);
			return newSlot;
		}

		Slot newSlot = (accessType == SlotAccess.MODIFY_GETTER_SETTER ? new GetterSlot(key, index, 0) : new Slot(key, index, 0));
		if (accessType == SlotAccess.MODIFY_CONST) {
			newSlot.setAttributes(ScriptableObject.CONST);
		}
		addSlot(cx, newSlot);
		return newSlot;
	}

	@Override
	public void addSlot(Context cx, Slot newSlot) {
		Object name = newSlot.name == null ? String.valueOf(newSlot.indexOrHash) : newSlot.name;
		map.put(name, newSlot);
	}

	@Override
	public void remove(Context cx, Object key, int index) {
		Object name = key == null ? String.valueOf(index) : key;
		Slot slot = map.get(name);
		if (slot != null) {
			// non-configurable
			if ((slot.getAttributes() & ScriptableObject.PERMANENT) != 0) {
				if (cx.isStrictMode()) {
					throw ScriptRuntime.typeError1("msg.delete.property.with.configurable.false", key);
				}
				return;
			}
			map.remove(name);
		}
	}

	@Override
	public Iterator<Slot> iterator() {
		return map.values().iterator();
	}
}

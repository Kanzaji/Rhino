/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import java.util.Iterator;

/**
 * This class holds the various SlotMaps of various types, and knows how to atomically
 * switch between them when we need to so that we use the right data structure at the right time.
 */
class SlotMapContainer implements SlotMap {

	protected SlotMap map;

	SlotMapContainer(int initialSize) {
		map = new HashSlotMap();
	}

	@Override
	public int size() {
		return map.size();
	}

	public int dirtySize() {
		return map.size();
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public Slot get(Context cx, Object key, int index, SlotAccess accessType) {
		return map.get(cx, key, index, accessType);
	}

	@Override
	public Slot query(Context cx, Object key, int index) {
		return map.query(cx, key, index);
	}

	@Override
	public void addSlot(Context cx, Slot newSlot) {
		map.addSlot(cx, newSlot);
	}

	@Override
	public void remove(Context cx, Object key, int index) {
		map.remove(cx, key, index);
	}

	@Override
	public Iterator<Slot> iterator() {
		return map.iterator();
	}

	public long readLock() {
		// No locking in the default implementation
		return 0L;
	}

	public void unlockRead(long stamp) {
		// No locking in the default implementationock.unlockRead(stamp);
	}

	/**
	 * Before inserting a new item in the map, check and see if we need to expand from the embedded
	 * map to a HashMap that is more robust against large numbers of hash collisions.
	 */
	protected void checkMapSize(Context cx) {
	}
}

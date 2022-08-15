package dev.latvian.mods.rhino.classdata;

import dev.latvian.mods.rhino.js.prototype.MemberFunctions;

import java.util.List;
import java.util.Map;

class TempMemberGroup {
	public final String name;
	public MemberFunctions field;
	public List<MemberFunctions> methods;
	public MemberFunctions beanGet;
	public MemberFunctions beanSet;

	TempMemberGroup(String n) {
		name = n;
	}

	void optimize(Map<String, MemberFunctions> map) {
		int size = methods == null ? 0 : methods.size();

		if (field != null) {
			size++;
		}

		if (beanGet != null) {
			size++;
		}

		if (beanSet != null) {
			size++;
		}

		if (size != 1) {
			map.put(name, new MemberGroup(this));
		} else if (field != null) {
			map.put(name, field);
		} else if (beanGet != null) {
			map.put(name, beanGet);
		} else if (beanSet != null) {
			map.put(name, beanSet);
		} else {
			map.put(name, methods.get(0));
		}
	}
}

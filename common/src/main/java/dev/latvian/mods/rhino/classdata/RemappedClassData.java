package dev.latvian.mods.rhino.classdata;

import dev.latvian.mods.rhino.js.prototype.MemberFunctions;
import dev.latvian.mods.rhino.util.Remapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RemappedClassData {
	public final PublicClassData publicClassData;
	public final Remapper remapper;
	public final String name;
	public final Map<String, MemberFunctions> members;
	public final Map<String, MemberFunctions> staticMembers;

	public RemappedClassData(PublicClassData publicClassData, Remapper remapper) {
		this.publicClassData = publicClassData;
		this.remapper = remapper;
		this.name = this.remapper.getMappedClass(publicClassData);
		this.members = new HashMap<>();
		this.staticMembers = new HashMap<>();
		load();
	}

	private TempMemberGroup make(Map<String, TempMemberGroup> map, String name) {
		TempMemberGroup m = map.get(name);

		if (m == null) {
			m = new TempMemberGroup(name);
			map.put(name, m);
		}

		return m;
	}

	private void load() {
		var lm = new HashMap<String, TempMemberGroup>();
		var sm = new HashMap<String, TempMemberGroup>();

		for (var field : publicClassData.getFields()) {
			String n = remapper.getMappedField(publicClassData, field);
			var cm = make(field.isStatic() ? sm : lm, n);
			cm.field = field;
		}

		for (var method : publicClassData.getMethods()) {
			String n = remapper.getMappedMethod(publicClassData, method);
			var m = method.isStatic() ? sm : lm;
			var cm = make(m, n);

			if (cm.methods == null) {
				cm.methods = new ArrayList<>(1);
			}

			cm.methods.add(method);

			if (method.signature.types.length == 0 && n.length() >= 4 && !method.isVoid() && Character.isUpperCase(n.charAt(3)) && n.startsWith("get")) {
				make(m, n.substring(3, 4).toLowerCase() + n.substring(4)).beanGet = method;
			} else if (method.signature.types.length == 1 && n.length() >= 4 && Character.isUpperCase(n.charAt(3)) && n.startsWith("set")) {
				make(m, n.substring(3, 4).toLowerCase() + n.substring(4)).beanSet = method;
			} else if (method.signature.types.length == 0 && n.length() >= 3 && method.isBoolean() && Character.isUpperCase(n.charAt(2)) && n.startsWith("is")) {
				make(m, n.substring(2, 3).toLowerCase() + n.substring(3)).beanGet = method;
			}
		}

		for (var member : sm.values()) {
			member.optimize(staticMembers);
		}
	}

	@Override
	public String toString() {
		return name;
	}
}

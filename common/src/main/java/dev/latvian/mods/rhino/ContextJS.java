package dev.latvian.mods.rhino;

public class ContextJS {
	public final Context context;
	public final Scriptable rootScope;
	private SharedContextData sharedContextData;

	public ContextJS(Context context) {
		this.context = context;
		this.rootScope = new NativeObject();
	}

	public SharedContextData getSharedContextData() {
		if (sharedContextData == null) {
			sharedContextData = context.getSharedData(rootScope);
		}

		return sharedContextData;
	}
}

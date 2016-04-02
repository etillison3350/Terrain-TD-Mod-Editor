package modeditor.mod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;

import modeditor.JSON;
import modeditor.Main;
import modeditor.undo.UndoableList;
import modeditor.undo.UndoableMap;

public class Mod {

	private Mod() {}

	public static UndoableMap<String, Object> info = new UndoableMap<>();
	public static UndoableList<UndoableMap<String, Object>> values = new UndoableList<>();

	private static Map<String, Class<?>> fieldTypes;

	private static Map<String, Field> fieldDef;
	private static Map<String, List<String>> arrayDef;

	public static void generateValues() {
		if (Main.path == null) return;

		fieldTypes = new HashMap<>();

		fieldTypes.put("B", Boolean.class);
		fieldTypes.put("B2", Boolean.class);
		fieldTypes.put("D", Number.class);
		fieldTypes.put("EFID", String.class);
		fieldTypes.put("EA", List.class);
		fieldTypes.put("EUA", List.class);
		fieldTypes.put("EID", String.class);
		fieldTypes.put("I", Number.class);
		fieldTypes.put("LA", List.class);
		fieldTypes.put("P", Object.class);
		fieldTypes.put("P2", Object.class);
		fieldTypes.put("PID", String.class);
		fieldTypes.put("PRA", List.class);
		fieldTypes.put("PUA", List.class);
		fieldTypes.put("S", String.class);
		fieldTypes.put("SA", List.class);
		fieldTypes.put("TUA", List.class);
		fieldTypes.put("UA", List.class);

		fieldDef = new LinkedHashMap<>();
		try {
			List<String> lines = Files.readAllLines(Paths.get("resources/fielddef.cfg"));
			for (String line : lines) {
				Field f = new Field(line);
				fieldDef.put(f.name, f);
			}
		} catch (Exception e) {}

		arrayDef = new HashMap<>();
		try {
			List<String> lines = Files.readAllLines(Paths.get("resources/arraydef.cfg"));
			for (String line : lines) {
				String[] parts = line.split("\t");
				arrayDef.put(parts[0], Arrays.asList(parts[1].split("&")));
			}
		} catch (Exception e) {}

		values = new UndoableList<>();
		values.addUndoableEditListener(listener);

		try (Stream<Path> files = Files.walk(Main.path)) {
			files.forEach(path -> {
				try {
					if (Files.isDirectory(path) || !path.getFileName().toString().endsWith(".json")) return;

					List<?> json = JSON.parseJSON(new String(Files.readAllBytes(path)));

					while (json.get(0) instanceof List) {
						json = (List<?>) json.get(0);
					}

					for (Object o : json) {
						if (!(o instanceof Map<?, ?>)) continue;

						Map<?, ?> map = (Map<?, ?>) o;

						if (path.getFileName().toString().equals("info.json")) {
							info.putAll(getFullKeys("mod", map));
							info.addUndoableEditListener(listener);
						} else {
							Map<String, Object> keys = new HashMap<>();
							fieldDef.keySet().stream().filter(s -> s.startsWith(map.get("type") + ">")).forEach(s -> {
								Object[] args = fieldDef.get(s).args;
								if (args.length <= 0) return;
								keys.put(s, args[0]);
							});
							keys.putAll(getFullKeys(map.get("type").toString(), map));
							keys.put("type", map.get("type").toString());

							UndoableMap<String, Object> um = undoableFrom(keys);
							um.addUndoableEditListener(listener);
							values.addFinal(um);
						}
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		} catch (IOException e) {}

	}

	private static <K> UndoableMap<K, Object> undoableFrom(Map<K, ?> map) {
		UndoableMap<K, Object> ret = new UndoableMap<>();
		ret.addUndoableEditListener(listener);
		for (K key : map.keySet()) {
			Object value = map.get(key);

			if (value instanceof Map) {
				value = undoableFrom((Map<?, ?>) value);
			} else if (value instanceof List) {
				value = undoableFrom((List<?>) value);
			}
			ret.putFinal(key, value);
		}
		return ret;
	}

	private static UndoableList<Object> undoableFrom(List<?> list) {
		UndoableList<Object> ret = new UndoableList<>();
		ret.addUndoableEditListener(listener);
		for (Object value : list) {
			if (value instanceof Map) {
				value = undoableFrom((Map<?, ?>) value);
			} else if (value instanceof List) {
				value = undoableFrom((List<?>) value);
			}
			ret.addFinal(value);
		}
		return ret;
	}

	private static Map<String, Object> getFullKeys(String prefix, Map<?, ?> map) {
		Map<String, Object> ret = new HashMap<>();

		for (Object k : map.keySet()) {
			if (!(k instanceof String)) continue;
			Object o = map.get(k);
			if (o == null) continue;
			List<String> keys = new ArrayList<>(1);
			keys.add(prefix + ">" + k);
			Field f = fieldDef.get(prefix + ">" + k);
			if (f == null) {
				keys = arrayDef.get(prefix + ">" + k);
				if (keys == null) continue;
			}

			for (String key : keys) {
				if (!fieldTypes.get(f.type).isAssignableFrom(o.getClass())) continue;

				if (!f.type.startsWith("P") && !f.type.endsWith("A")) ret.put(key, o);
				if (o instanceof Map<?, ?>) {
					ret.putAll(getFullKeys(key, (Map<?, ?>) o));
				} else if (o instanceof List<?>) {
					List<?> list = (List<?>) o;

					if ("tower>terrain".equals(key)) {
						for (Object obj : list) {
							if (!(obj instanceof String)) continue;
							if (arrayDef.containsKey(key + ">" + obj)) {
								for (String pre : arrayDef.get(key + ">" + obj)) {
									ret.put(pre, true);
								}
							} else {
								ret.put(key + ">" + obj, true);
							}
						}
					} else if (f.type.equals("UA")) {
						List<Map<String, Object>> l = new ArrayList<>();

						for (Object obj : list) {
							if (obj instanceof Map<?, ?>) {
								l.add(getFullKeys("unit", (Map<?, ?>) obj));
							} else if (obj instanceof List<?>) {
								List<?> li = (List<?>) obj;

								Map<String, Object> m = new HashMap<>();

								for (int i = 0; i < li.size(); i++) {
									List<String> s = arrayDef.containsKey("unit>" + i + "-" + li.size()) ? arrayDef.get("unit>" + i + ">" + list.size()) : arrayDef.get("unit>" + i);
									for (String str : s) {
										m.put(str, li.get(i));
									}
								}

								l.add(m);
							}
						}

						ret.put(key, l);
					} else if (f.type.endsWith("SA")) {
						List<Object> strings = new ArrayList<>();
						for (Object obj : list) {
							if (!(obj instanceof String)) continue;
							strings.add(obj);
						}
					} else if (f.type.endsWith("A")) {
						List<Map<String, Object>> l = new ArrayList<>();
						String pre;
						switch (f.type) {
							case "EA":
								pre = "effect";
								break;
							case "LA":
								pre = "ilevel";
								break;
							case "PRA":
								pre = "projectile";
								break;
							case "TUA":
								pre = "towerupgrade";
								break;
							default:
								continue;
						}

						for (Object obj : list) {
							if (!(obj instanceof Map<?, ?>)) continue;

							l.add(getFullKeys(pre, (Map<?, ?>) obj));
						}

						ret.put(key, l);
					} else if (f.type.equals("P")) {
						for (int i = 0; i < list.size(); i++) {
							List<String> s = arrayDef.containsKey(key + ">" + i + "-" + list.size()) ? arrayDef.get(key + ">" + i + ">" + list.size()) : arrayDef.get(key + ">" + i);
							for (String str : s) {
								ret.put(str, list.get(i));
							}
						}
					}
				}
			}
		}
		return ret;
	}

	public static Map<String, Field> getFieldDefinitions() {
		return new LinkedHashMap<String, Field>(fieldDef);
	}

	private static final UndoableEditListener listener = new UndoableEditListener() {

		@Override
		public void undoableEditHappened(UndoableEditEvent e) {
			Main.manager.addEdit(e.getEdit());
		}
	};

}

package modeditor;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JSON {

	public static final NumberFormat numbers = new DecimalFormat("0");

	private JSON() {}

	public static List<Object> parseJSON(String json) {
		HashMap<String, Object> braceObjs = new HashMap<>();

		String s = json;

		int lastAddress = (int) (System.currentTimeMillis() % 32768);

		Pattern quotes = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"");
		Matcher m = quotes.matcher(s);
		while (m.find()) {
			String v = m.group(1);
			String k = "@" + lastAddress++;

			braceObjs.put(k, v.replaceAll("\\\\(.)", "$1"));
			s = s.replace(m.group(), k);

			m = quotes.matcher(s);
		}

		s = s.replaceAll("\\s+", "");

		Pattern braces = Pattern.compile("\\[[^\\[\\]\\{\\}]*\\]|\\{[^\\[\\]\\{\\}]*?\\}");

		m = braces.matcher(s);
		while (m.find()) {
			String g = m.group();
			if (g.charAt(0) == '[') {
				String[] terms = g.split("[\\[\\]\\,]");
				ArrayList<Object> termList = new ArrayList<>(terms.length);
				for (String term : terms) {
					if (term.isEmpty()) continue;

					if (term.startsWith("\"")) {
						termList.add(term.substring(1, term.length() - 1));
					} else if (term.startsWith("@")) {
						termList.add(braceObjs.get(term));
					} else if (term.equals("true")) {
						termList.add(true);
					} else if (term.equals("false")) {
						termList.add(false);
					} else {
						try {
							termList.add(Double.parseDouble(term));
						} catch (NumberFormatException e) {
							termList.add(null);
						}
					}
				}

				String k = "@" + lastAddress++;

				s = s.replace(g, k);
				if (s.equals(k)) return termList;
				braceObjs.put(k, termList);
			} else {
				String[] terms = g.split("[\\{\\}\\,]");
				Map<String, Object> termList = new HashMap<>();
				for (String term : terms) {
					if (term.isEmpty()) continue;

					String[] kvs = term.split(":");
					String k = (kvs[0].startsWith("@") ? (String) braceObjs.get(kvs[0]) : kvs[0].replace("\"", "")).toLowerCase();
					if (kvs[1].startsWith("\"")) {
						termList.put(k, kvs[1].substring(1, kvs[1].length() - 1));
					} else if (kvs[1].startsWith("@")) {
						termList.put(k, braceObjs.get(kvs[1]));
					} else if (kvs[1].equals("true")) {
						termList.put(k, true);
					} else if (kvs[1].equals("false")) {
						termList.put(k, false);
					} else {
						try {
							termList.put(k, Double.parseDouble(kvs[1]));
						} catch (NumberFormatException e) {
							termList.put(k, null);
						}
					}
				}

				String k = "@" + lastAddress++;

				s = s.replace(g, k);
				if (s.equals(k)) {
					ArrayList<Object> ret = new ArrayList<>(1);
					ret.add(termList);
					return ret;
				}
				braceObjs.put(k, termList);
			}

			m = braces.matcher(s);
		}

		return null;
	}

	/**
	 * <ul>
	 * <li><b><i>writeJSON</i></b><br>
	 * <br>
	 * {@code String writeJSON()}<br>
	 * <br>
	 * Writes the given object as a JSON object<br>
	 * @param obj The object to write. Can be any type of object (including <code>null</code>) <b>except primitive array types</b> (e.g. <code>int[]</code>, <code>float[]</code>, etc).
	 * @return The object represented in JSON as a string
	 *         </ul>
	 */
	public static String writeJSON(Object obj) {
		numbers.setMaximumFractionDigits(16);

		if (obj == null) {
			return "null";
		}

		if (obj instanceof Number) {
			return numbers.format(((Number) obj).doubleValue());
		}

		if (obj instanceof Boolean) {
			return Boolean.toString((boolean) obj);
		}

		if (obj instanceof String) {
			return "\"" + ((String) obj).replaceAll("[\"\\\\]", "\\$0") + "\"";
		}

		if (obj instanceof Iterable) {
			String ret = "[";
			for (Object o : (Iterable<?>) obj)
				ret += writeJSON(o) + ",";
			return ret.replaceAll(",$", "") + "]";
		}

		if (obj.getClass().isArray()) {
			String ret = "[";
			for (Object o : (Object[]) obj)
				ret += writeJSON(o) + ",";
			return ret.replaceAll(",$", "") + "]";
		}

		if (obj instanceof Map) {
			String ret = "{";
			for (Object o : ((Map<?, ?>) obj).keySet())
				ret += String.format("\"%s\":%s,", o, writeJSON(((Map<?, ?>) obj).get(o)));
			return ret.replaceAll(",$", "") + "}";
		}

		String ret = "{";
		for (Field field : obj.getClass().getFields()) {
			try {
				if (Modifier.isStatic(field.getModifiers())) continue;

				Object o = field.get(obj);
				if (o == obj) continue;
				ret += String.format("\"%s\":%s,", field.getName(), writeJSON(o));
			} catch (IllegalArgumentException | IllegalAccessException e) {}
		}
		return ret.replaceAll(",$", "") + "}";
	}

}

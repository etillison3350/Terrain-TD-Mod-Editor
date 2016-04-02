package modeditor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;

import modeditor.undo.UndoableMap;

public class Language {

	private static Locale currentLocale = Locale.US;

	private static final Pattern PATTERN = Pattern.compile("^([A-Za-z0-9\\-\\*\\>]+)\\s*\\=\\s*(.+)$", Pattern.MULTILINE);

	private Language() {}

	private static Map<Locale, Map<String, String>> terms = new HashMap<>();
	private static UndoableMap<Locale, UndoableMap<String, String>> modTerms;
	private static Map<String, Locale> localeNames;

	public static Map<String, Locale> getLocaleNames() {
		if (localeNames != null) return localeNames;

		Map<String, Locale> ret = new TreeMap<>();

		List<String> lines;
		try {
			lines = Files.readAllLines(Paths.get("resources/locale/names"));
		} catch (IOException e) {
			return ret;
		}

		for (String line : lines) {
			Matcher matcher = PATTERN.matcher(line);
			if (!matcher.find()) continue;
			ret.put(matcher.group(2), Locale.forLanguageTag(matcher.group(1)));
		}

		localeNames = ret;

		return ret;
	}

	public static String get(String key) {
		if (!terms.containsKey(currentLocale)) generateValues();
		String ret = terms.get(currentLocale).get(key.toLowerCase());
		if (ret == null) ret = "missing-key-" + key.toLowerCase();
		return ret;
	}

	private static void generateValues() {
		HashMap<String, String> vals = new HashMap<>();
		
		try {
			Files.walk(Paths.get("resources/locale")).forEach(path -> {
				try {
					if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) return;
					
					String[] names = path.getFileName().toString().split("\\.");
					if (!names[names.length - 1].equals("lang") || !Locale.forLanguageTag(names[0]).equals(currentLocale)) return;
					
					List<String> lines = Files.readAllLines(path);
					
					for (String line : lines) {
						Matcher matcher = PATTERN.matcher(line);
						if (!matcher.find()) continue;
						
						vals.put(matcher.group(1).toLowerCase(), matcher.group(2));
					}
				} catch (IOException e) {}
			});
		} catch (IOException e) {}
		
		terms.put(currentLocale, vals);
	}

	public static String getFromMod(String key) {
		return getFromMod(currentLocale, key);
	}
	
	public static String getFromMod(Locale locale, String key) {
		if (modTerms == null) generateModValues();
		
		String ret = "";
		try {
			ret = modTerms.get(locale).get(key);
		} catch (Exception e) {}

		return ret == null ? "" : ret;
	}

	private static void generateModValues() {
		if (Main.path == null) return;

		modTerms = new UndoableMap<>();
		modTerms.addUndoableEditListener(listener);

		try {
			Files.walk(Main.path).forEach(path -> {
				if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) return;
				String[] names = path.getFileName().toString().split("\\.");
				if (!names[names.length - 1].equals("lang")) return;

				Locale loc = Locale.forLanguageTag(names[0]);
				UndoableMap<String, String> terms;
				if (modTerms.containsKey(loc)) {
					terms = modTerms.get(loc);
				} else {
					terms = new UndoableMap<>();
					terms.addUndoableEditListener(listener);
					modTerms.putFinal(loc, terms);
				}

				List<String> lines;
				try {
					lines = Files.readAllLines(path);
				} catch (Exception e) {
					return;
				}

				for (String line : lines) {
					Matcher m = PATTERN.matcher(line);
					if (!m.find()) continue;
					terms.putFinal(m.group(1), m.group(2));
				}
			});
		} catch (IOException e) {}
	}

	public static Locale getCurrentLocale() {
		return currentLocale;
	}

	public static void setCurrentLocale(Locale currentLocale) {
		Language.currentLocale = currentLocale;
	}

	private static final UndoableEditListener listener = new UndoableEditListener() {

		@Override
		public void undoableEditHappened(UndoableEditEvent e) {
			Main.manager.addEdit(e.getEdit());
		}
	};

}

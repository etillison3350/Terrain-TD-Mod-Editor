package modeditor.mod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Field {

		public final String name;
		public final String type;
		public final Object[] args;

		public Field(String str) {
			try {
				String[] strings = str.split("\\s");
				this.name = strings[0];
				this.type = strings[1];
				if (strings.length < 3) {
					args = type.endsWith("A") ? new Object[] {new ArrayList<>()} : new Object[0];
				} else if (strings[2].startsWith("[")) {
					args = strings[2].replaceAll("[\\[\\]]", "").split(",");
				} else if (strings[2].equalsIgnoreCase("t")) {
					args = new Object[] {true};
				} else if (strings[2].equalsIgnoreCase("f")) {
					args = new Object[] {false};
				} else {
					List<Double> ns = new ArrayList<>();
					Arrays.stream(strings).forEach(s -> {
						try {
							ns.add(Double.parseDouble(s));
						} catch (NumberFormatException e) {}
					});
					args = ns.toArray();
				}
			} catch (Exception e) {
				throw new IllegalArgumentException(e.getMessage());
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return name == null ? 31 : 31 + name.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			Field other = (Field) obj;
			if (name == null) {
				if (other.name != null) return false;
			} else if (!name.equals(other.name)) {
				return false;
			}
			return true;
		}
	}
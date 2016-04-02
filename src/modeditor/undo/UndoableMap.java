package modeditor.undo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

public class UndoableMap<K, V> extends HashMap<K, V> {

		private static final long serialVersionUID = -9067818878758681588L;

		private final List<UndoableEditListener> undoableEditListeners = new ArrayList<>();

		public UndoableMap() {
			super();
		}

		public UndoableMap(int initialCapacity, float loadFactor) {
			super(initialCapacity, loadFactor);
		}

		public UndoableMap(int initialCapacity) {
			super(initialCapacity);
		}

		public UndoableMap(Map<? extends K, ? extends V> m) {
			super(m);
		}

		@Override
		public V put(K key, V value) {
			final V ret = super.put(key, value);
			final MapEdit<K, V> edit = new MapEdit<>(this, key, ret, value);
			for (UndoableEditListener l : undoableEditListeners) {
				l.undoableEditHappened(new UndoableEditEvent(this, edit));
			}
			return ret;
		}

		public V putFinal(K key, V value) {
			if (value == null) {
				return super.remove(key);
			} else {
				return super.put(key, value);
			}
		}

		public void addUndoableEditListener(UndoableEditListener listener) {
			undoableEditListeners.add(listener);
		}

		public void removeUndoableEditListener(UndoableEditListener listener) {
			undoableEditListeners.remove(listener);
		}

		private static final class MapEdit<K, V> extends AbstractUndoableEdit {

			private static final long serialVersionUID = 1293449970683782384L;

			private final K key;
			private final V origVal;
			private final V newVal;
			private final UndoableMap<K, V> map;

			public MapEdit(UndoableMap<K, V> map, K key, V origVal, V newVal) {
				this.map = map;
				this.key = key;
				this.origVal = origVal;
				this.newVal = newVal;
			}

			@Override
			public void undo() throws CannotUndoException {
				map.putFinal(key, origVal);
			}

			@Override
			public void redo() throws CannotRedoException {
				map.putFinal(key, newVal);
			}

		}

	}
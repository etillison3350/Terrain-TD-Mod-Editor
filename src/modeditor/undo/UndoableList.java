package modeditor.undo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

public class UndoableList<E> extends ArrayList<E> {

		private static final long serialVersionUID = 8083483542757743304L;

		private final List<UndoableEditListener> undoableEditListeners = new ArrayList<>();

		public UndoableList() {
			super();
		}

		public UndoableList(Collection<? extends E> c) {
			super(c);
		}

		public UndoableList(int initialCapacity) {
			super(initialCapacity);
		}

		@Override
		public boolean add(E e) {
			if (super.add(e)) {
				final ListEdit<E> edit = new ListEdit<>(this, e, this.size() - 1, true);
				for (UndoableEditListener l : undoableEditListeners) {
					l.undoableEditHappened(new UndoableEditEvent(this, edit));
				}
				return true;
			}
			return false;
		}

		@Override
		public boolean remove(Object o) {
			final int i = this.indexOf(o);
			final E e = this.get(i);
			if (super.remove(o)) {
				final ListEdit<E> edit = new ListEdit<>(this, e, i, false);
				for (UndoableEditListener l : undoableEditListeners) {
					l.undoableEditHappened(new UndoableEditEvent(this, edit));
				}
				return true;
			}
			return false;
		}

		private boolean undo(Object o) {
			return super.remove(o);
		}
		
		public boolean addFinal(E e) {
			return super.add(e);
		}

		public void addUndoableEditListener(UndoableEditListener listener) {
			undoableEditListeners.add(listener);
		}

		public void removeUndoableEditListener(UndoableEditListener listener) {
			undoableEditListeners.remove(listener);
		}

		private static class ListEdit<E> extends AbstractUndoableEdit {

			private static final long serialVersionUID = 7765991485654517086L;

			private final E object;
			private final int index;
			private final UndoableList<E> list;
			private final boolean add;

			public ListEdit(UndoableList<E> list, E object, int index, boolean add) {
				this.list = list;
				this.object = object;
				this.index = index;
				this.add = add;
			}

			@Override
			public void undo() throws CannotUndoException {
				if (add) {
					list.undo(object);
				} else {
					list.add(index, object);
				}
			}

			@Override
			public void redo() throws CannotRedoException {
				if (add) {
					list.add(index, object);
				} else {
					list.undo(object);
				}
			}

		}

	}

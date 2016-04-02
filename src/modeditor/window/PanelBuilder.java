package modeditor.window;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.Box;
import javax.swing.JPanel;

public class PanelBuilder {

	private final JPanel panel;
	private final GridBagConstraints c;

	private boolean created = false;

	public PanelBuilder() {
		panel = new JPanel(new GridBagLayout());

		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(0, 5, 0, 5);
		c.weightx = 1;
		c.weighty = 0;
	}

	public void add(Component comp) {
		panel.add(comp, c);
		c.gridy++;
	}

	public void addSpace() {
		addSpace(5);
	}

	public void addSpace(int height) {
		if (panel.getComponentCount() > 0) panel.add(Box.createVerticalStrut(height), c);
		c.gridy++;
	}

	public JPanel build() {
		if (created) throw new IllegalStateException("Panel can only be built once");
		created = true;

		c.weighty = 1;
		panel.add(Box.createVerticalGlue(), c);
		return panel;
	}

	public void addAll(Component[] components) {
		for (Component c : components)
			add(c);
	}

}

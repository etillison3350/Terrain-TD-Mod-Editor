package modeditor.window;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import modeditor.Language;
import modeditor.mod.Field;
import modeditor.mod.Mod;

public class Window extends JFrame {

	private static final long serialVersionUID = 4553840840009909214L;

	public final JMenuBar menuBar;
	public final JMenu file;
	public final JMenuItem newMod;
	public final JMenuItem open;
	public final JMenuItem save;
	public final JMenuItem saveAs;
	public final JMenuItem settings;
	public final JMenuItem exit;
	public final JMenu edit;
	public final JMenuItem undo;
	public final JMenuItem redo;
	public final JMenu object;
	public final JMenuItem newObject;
	public final JMenuItem delete;
	public final JMenuItem duplicate;

	public final JSplitPane splitPane;
	public final JTree tree;
	public final JPanel panel;

	public final Map<String, Icon> icons = new HashMap<>();// enemy, instant, level, obstacle, tower, world;

	public Window() {
		super(Language.get("title"));

		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		this.setSize(100 * (int) Math.floor((dim.width - 50) / 100), 100 * (int) Math.floor((dim.height - 50) / 100));
		this.setLocationRelativeTo(null);
		this.setExtendedState(this.getExtendedState() | JFrame.MAXIMIZED_BOTH);

		menuBar = new JMenuBar();

		file = new JMenu(Language.get("file"));
		newMod = new JMenuItem(Language.get("new"));
		newMod.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));
		file.add(newMod);
		open = new JMenuItem(Language.get("open"));
		open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
		file.add(open);
		save = new JMenuItem(Language.get("save"));
		save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
		file.add(save);
		saveAs = new JMenuItem(Language.get("save-as"));
		saveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));
		file.add(saveAs);
		file.addSeparator();
		settings = new JMenuItem(Language.get("settings"));
		file.add(settings);
		file.addSeparator();
		exit = new JMenuItem(Language.get("exit"));
		exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK));
		file.add(exit);

		menuBar.add(file);

		edit = new JMenu(Language.get("edit"));
		undo = new JMenuItem(Language.get("undo"));
		undo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK));
		edit.add(undo);
		redo = new JMenuItem(Language.get("redo"));
		redo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));
		edit.add(redo);

		menuBar.add(edit);

		object = new JMenu(Language.get("object"));
		newObject = new JMenuItem(Language.get("new"));
		newObject.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK));
		object.add(newObject);
		delete = new JMenuItem(Language.get("delete"));
		delete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
		object.add(delete);
		duplicate = new JMenuItem(Language.get("duplicate"));
		object.add(duplicate);

		menuBar.add(object);

		this.setJMenuBar(menuBar);

		tree = new JTree();
		tree.setRootVisible(true);
		tree.setCellRenderer(new DefaultTreeCellRenderer() {

			private static final long serialVersionUID = -5051686189121402601L;

			@Override
			public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
				Component c = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

				if (!(value instanceof DefaultMutableTreeNode) || !(c instanceof JLabel)) return c;
				Object v = ((DefaultMutableTreeNode) value).getUserObject();
				if (!(v instanceof Map)) return c;

				JLabel label = (JLabel) c;
				Map<?, ?> map = ((Map<?, ?>) v);
				label.setText(Language.getFromMod(map.get(map.get("type") + ">id") + ""));
				if (!map.get("type").equals("mod")) label.setIcon(icons.get(map.get("type")));

				return c;
			}

		});
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addTreeSelectionListener(new TreeSelectionListener() {

			@Override
			public void valueChanged(TreeSelectionEvent e) {
				panel.removeAll();

				Object v = ((DefaultMutableTreeNode) tree.getLastSelectedPathComponent()).getUserObject();
				if (!(v instanceof Map)) {
					panel.revalidate();
					panel.repaint();
					return;
				}

				Map<?, ?> map = (Map<?, ?>) v;

				final String type = map.get("type") + ">";

				PanelBuilder p = new PanelBuilder();

				for (String s : Mod.getFieldDefinitions().keySet()) {
					if (s.endsWith("id")) {
						// TODO
					} else if (s.startsWith(type)) {
						p.addSpace();
						try {
							p.addAll(getComponentsForValue(s, Mod.getFieldDefinitions().get(s), map.get(s)));
						} catch (Exception exception) {}
					}
				}

				JScrollPane scroll = new JScrollPane(p.build(), JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
				scroll.getVerticalScrollBar().setUnitIncrement(10);
				panel.add(scroll);

				panel.revalidate();
				panel.repaint();
			}
		});

		JButton add = new JButton("+");
		add.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				newObject.doClick();
			}
		});

		JPanel treePanel = new JPanel(new BorderLayout());
		treePanel.add(add, BorderLayout.PAGE_START);
		treePanel.add(tree);

		panel = new JPanel(new BorderLayout());

		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treePanel, panel);
		splitPane.setDividerLocation(256);
		this.add(splitPane);

		Image enemy = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), instant = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), level = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), obstacle = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), tower = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), world = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), towerupgrade = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

		try {
			enemy = ImageIO.read(Paths.get("resources/images/enemy.png").toFile()).getScaledInstance(14, 14, 0);
		} catch (IOException e) {}
		icons.put("enemy", new ImageIcon(enemy));

		try {
			instant = ImageIO.read(Paths.get("resources/images/instant.png").toFile()).getScaledInstance(14, 14, 0);
		} catch (IOException e) {}
		icons.put("instant", new ImageIcon(instant));

		try {
			level = ImageIO.read(Paths.get("resources/images/level.png").toFile()).getScaledInstance(14, 14, 0);
		} catch (IOException e) {}
		icons.put("level", new ImageIcon(level));

		try {
			obstacle = ImageIO.read(Paths.get("resources/images/obstacle.png").toFile()).getScaledInstance(14, 14, 0);
		} catch (IOException e) {}
		icons.put("obstacle", new ImageIcon(obstacle));

		try {
			tower = ImageIO.read(Paths.get("resources/images/tower.png").toFile()).getScaledInstance(14, 14, 0);
		} catch (IOException e) {}
		icons.put("tower", new ImageIcon(tower));

		try {
			world = ImageIO.read(Paths.get("resources/images/world.png").toFile()).getScaledInstance(14, 14, 0);
		} catch (IOException e) {}
		icons.put("world", new ImageIcon(world));

		try {
			world = ImageIO.read(Paths.get("resources/images/world.png").toFile()).getScaledInstance(14, 14, 0);
		} catch (IOException e) {}
		icons.put("world", new ImageIcon(world));

		try {
			towerupgrade = ImageIO.read(Paths.get("resources/images/towerupgrade.png").toFile()).getScaledInstance(14, 14, 0);
		} catch (IOException e) {}
		icons.put("towerupgrade", new ImageIcon(towerupgrade));

		regenerateNodes();
	}

	private static Component[] getComponentsForValue(String key, Field field, Object value) {
		List<Component> ret = new ArrayList<>();

		if (!field.type.equals("B")) {
			JLabel lbl = new JLabel(Language.get(key));
			lbl.setFont(lbl.getFont().deriveFont(Mod.getFieldDefinitions().get(key).type.equals("P") ? Font.BOLD : Font.PLAIN));
			ret.add(lbl);
		}

		switch (field.type) {
			case "S":
				if (field.args.length > 0) {
					ret.add(new JComboBox<Object>(field.args));
				} else {
					ret.add(new JTextField(value.toString()));
				}
				break;
			case "I":
				ret.add(new JSpinner(new SpinnerNumberModel(((Number) value).intValue(), field.args.length <= 1 ? Integer.MIN_VALUE : ((Number) field.args[1]).intValue(), field.args.length <= 2 ? Integer.MAX_VALUE : ((Number) field.args[2]).intValue(), 1)));
				break;
			case "D":
				ret.add(new JSpinner(new SpinnerNumberModel(((Number) value).doubleValue(), field.args.length <= 1 ? Double.NEGATIVE_INFINITY : ((Number) field.args[1]).doubleValue(), field.args.length <= 2 ? Double.POSITIVE_INFINITY : ((Number) field.args[2]).doubleValue(), 1.0)));
				break;
			case "B":
				JCheckBox box = new JCheckBox(Language.get(key), (Boolean) value);
				box.setHorizontalTextPosition(SwingConstants.LEADING);
//				box.setHorizontalAlignment(SwingConstants.TRAILING);
				ret.add(box);
				break;
			default:
				throw new IllegalArgumentException("No component could be created for the field type '" + field.type + "'");
		}

		return ret.toArray(new Component[ret.size()]);
	}

	private void regenerateNodes() {
		Mod.generateValues();

		HashMap<Object, Object> info = new HashMap<>(Mod.info);
		info.put("type", "mod");
		DefaultMutableTreeNode mod = new DefaultMutableTreeNode(info);

		Map<String, DefaultMutableTreeNode> types = new HashMap<>();
		types.put("tower", new DefaultMutableTreeNode(Language.get("towers")));
		types.put("enemy", new DefaultMutableTreeNode(Language.get("enemies")));
		types.put("instant", new DefaultMutableTreeNode(Language.get("instants")));
		types.put("obstacle", new DefaultMutableTreeNode(Language.get("obstacles")));
		types.put("level", new DefaultMutableTreeNode(Language.get("levels")));
		types.put("world", new DefaultMutableTreeNode(Language.get("worlds")));

		for (String key : types.keySet())
			mod.add(types.get(key));

		for (Map<String, Object> map : Mod.values) {
			DefaultMutableTreeNode node = new DefaultMutableTreeNode(map);
			types.get(map.get("type")).add(node);

			if (map.get("type").equals("tower")) {
				List<?> upgrades = (List<?>) map.get("tower>upgrades");

				for (Object o : upgrades) {
					if (!(o instanceof Map)) continue;

					Map<Object, Object> m = new HashMap<>();
					m.put("type", "towerupgrade");
					m.putAll((Map<?, ?>) o);

					DefaultMutableTreeNode unode = new DefaultMutableTreeNode(m);
					node.add(unode);
				}
			}
		}

		((DefaultTreeModel) tree.getModel()).setRoot(mod);
	}

}

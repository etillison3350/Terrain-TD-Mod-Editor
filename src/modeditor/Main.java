package modeditor;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.undo.UndoManager;

import modeditor.window.Window;

public class Main {

	public static Window window;
	public static UndoManager manager = new UndoManager();

	public static Path path = Paths.get("C:/Users/etillison/git/Terrain-TD/terraintd/mods/base");
	
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {}

		window = new Window();
		
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setVisible(true);
	}

}

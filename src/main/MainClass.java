package main;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import GameEditor.GameEditor;

public class MainClass {

	public static void main(String[] args) {
		if (args.length > 0 && "--editor".equalsIgnoreCase(args[0])) {
			GameEditor.main(args);
			return;
		}
		SwingUtilities.invokeLater(() -> {
			try {
				new Game();
			} catch (RuntimeException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(null, "The game could not start:\n" + e.getMessage(),
						"Pananakop startup error", JOptionPane.ERROR_MESSAGE);
			}
		});
	}
}

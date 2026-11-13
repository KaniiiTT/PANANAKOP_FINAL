package main;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class MainClass {

	public static void main(String[] args) {
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
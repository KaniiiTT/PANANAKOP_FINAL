package main;

import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowAdapter;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

import javax.swing.JFrame;

public class GameWindow {
	private JFrame jframe;

	public GameWindow(GamePanel gamePanel) {
		this(gamePanel, false, null);
	}

	public GameWindow(GamePanel gamePanel, boolean testMode, Runnable onTestClosed) {

		jframe = new JFrame(testMode ? "Pananakop - Map Test" : "Pananakop");

		jframe.setDefaultCloseOperation(testMode ? JFrame.DISPOSE_ON_CLOSE : JFrame.EXIT_ON_CLOSE);
		jframe.add(gamePanel);
		
		jframe.setResizable(false);
		jframe.pack();

		// Keep the title bar on-screen even when the fixed game area is larger
		// than the monitor (for example, on a 1366x768 laptop display).
		Rectangle usableScreen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		int x = usableScreen.x + Math.max(0, (usableScreen.width - jframe.getWidth()) / 2);
		int y = usableScreen.y + Math.max(0, (usableScreen.height - jframe.getHeight()) / 2);
		jframe.setLocation(x, y);
		jframe.setVisible(true);
		jframe.toFront();
		jframe.requestFocus();
		javax.swing.SwingUtilities.invokeLater(gamePanel::requestFocusInWindow);
		jframe.addWindowFocusListener(new WindowFocusListener() {

			@Override
			public void windowLostFocus(WindowEvent e) {
				gamePanel.getGame().windowFocusLost();
			}

			@Override
			public void windowGainedFocus(WindowEvent e) {
				// TODO Auto-generated method stub

			}
		});
		if (testMode)
			jframe.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosed(WindowEvent event) {
					gamePanel.getGame().stopGameLoop();
					if (onTestClosed != null)
						onTestClosed.run();
				}
			});

	}

	public void close() {
		jframe.dispose();
	}

}

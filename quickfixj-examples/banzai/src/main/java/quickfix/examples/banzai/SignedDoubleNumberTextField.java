package quickfix.examples.banzai;

import javax.swing.*;
import java.awt.event.KeyEvent;

public class SignedDoubleNumberTextField extends JTextField {
    public void processKeyEvent(KeyEvent e) {
        char keyChar = e.getKeyChar();
        int keyCode = e.getKeyCode();
        if (((keyChar >= '0') && (keyChar <= '9')) ||
                (keyChar == 8) || (keyChar == 10) || (e.isControlDown() && (keyCode == 65 || keyCode == 67 || keyCode == 88 || keyCode == 86)) || (keyCode == 37) || (keyCode == 39) || (keyChar == 127) || (keyChar == 45)) {
            super.processKeyEvent(e);
        } else if (keyChar == '.') {
            String text = getText();
            if (!text.contains(".")) {
                super.processKeyEvent(e);
            }
        }
    }
}

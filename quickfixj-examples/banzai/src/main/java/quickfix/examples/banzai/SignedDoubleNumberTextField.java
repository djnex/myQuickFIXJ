package quickfix.examples.banzai;

import javax.swing.*;
import java.awt.event.KeyEvent;

public class SignedDoubleNumberTextField extends JTextField {
    public void processKeyEvent(KeyEvent e) {
        char keyChar = e.getKeyChar();
        if (((keyChar >= '0') && (keyChar <= '9')) ||
                (keyChar == 8) || (keyChar == 127) || (keyChar == 45)) {
            super.processKeyEvent(e);
        } else if (keyChar == '.') {
            String text = getText();
            if (!text.contains(".")) {
                super.processKeyEvent(e);
            }
        }
    }
}

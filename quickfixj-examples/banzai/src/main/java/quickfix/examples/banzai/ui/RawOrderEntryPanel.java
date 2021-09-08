package quickfix.examples.banzai.ui;

import quickfix.examples.banzai.BanzaiApplication;

import javax.swing.*;
import java.awt.*;

public class RawOrderEntryPanel extends JPanel {
    private final GridBagConstraints constraints = new GridBagConstraints();
    private final BanzaiApplication application;
    private final JTextField symbolTextField = new JTextField();
    private final JButton submitButton = new JButton("SubmitRaw");
    
    public RawOrderEntryPanel(BanzaiApplication application) {
        this.application = application;
        setName("RawOrderEntryPanel");
        submitButton.setText("SubmitRaw");
        createComponents();
    }

    private void createComponents() {
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1;
        int x = 0;
        int y = 0;

        add(new JLabel("Raw FIX mesage"), x, y);
        add(symbolTextField, x, ++y);
        add(symbolTextField, ++x, y);
    }
}

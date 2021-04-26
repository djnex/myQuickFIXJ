package quickfix.examples.banzai.ui;

import quickfix.examples.banzai.BanzaiApplication;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DJCustomPanel extends JPanel {
    private final JButton mdRequestButton = new JButton("MD");
    private final BanzaiApplication application;
    public DJCustomPanel(final BanzaiApplication application) {
        this.application = application;
        mdRequestButton.addActionListener(new MDRequestListener());
        setLayout(new GridBagLayout());
        createComponents();
    }

    private class MDRequestListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            application.sendMDRequest();
        }
    }

    private final GridBagConstraints constraints = new GridBagConstraints();
    private void createComponents() {
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1;

        int x = 0;
        int y = 0;

        constraints.insets = new Insets(0, 0, 5, 5);
        add(mdRequestButton, x, y);
    }

    private JComponent add(JComponent component, int x, int y) {
        constraints.gridx = x;
        constraints.gridy = y;
        add(component, constraints);
        return component;
    }
}

/*******************************************************************************
 * Copyright (c) quickfixengine.org  All rights reserved.
 *
 * This file is part of the QuickFIX FIX Engine
 *
 * This file may be distributed under the terms of the quickfixengine.org
 * license as defined by quickfixengine.org and appearing in the file
 * LICENSE included in the packaging of this file.
 *
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING
 * THE WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE.
 *
 * See http://www.quickfixengine.org/LICENSE for licensing information.
 *
 * Contact ask@quickfixengine.org if any conditions of this licensing
 * are not clear to you.
 ******************************************************************************/

package quickfix.examples.banzai.ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.SessionID;
import quickfix.examples.banzai.BanzaiApplication;
import quickfix.examples.banzai.DoubleNumberTextField;
import quickfix.examples.banzai.IntegerNumberTextField;
import quickfix.examples.banzai.LogonEvent;
import quickfix.examples.banzai.Order;
import quickfix.examples.banzai.OrderSide;
import quickfix.examples.banzai.OrderTIF;
import quickfix.examples.banzai.OrderTableModel;
import quickfix.examples.banzai.OrderType;
import quickfix.examples.banzai.SignedDoubleNumberTextField;

@SuppressWarnings("unchecked")
public class OrderEntryPanel extends JPanel implements Observer {
    private final static Logger LOGGER = LoggerFactory.getLogger(OrderEntryPanel.class);
    private boolean symbolEntered = false;
    private boolean quantityEntered = false;
    private boolean limitEntered = false;
    private boolean stopEntered = false;
    private boolean sessionEntered = false;

    private final JTextField symbolTextField = new JTextField();
    private final SignedDoubleNumberTextField quantityTextField = new SignedDoubleNumberTextField();

    private final JComboBox sideComboBox = new JComboBox(OrderSide.toArray());
    private final JComboBox typeComboBox = new JComboBox(OrderType.toArray());
    private final JComboBox tifComboBox = new JComboBox(OrderTIF.toArray());

    private final SignedDoubleNumberTextField limitPriceTextField = new SignedDoubleNumberTextField();
    private final SignedDoubleNumberTextField stopPriceTextField = new SignedDoubleNumberTextField();

    private final JComboBox sessionComboBox = new JComboBox();

    private final JLabel limitPriceLabel = new JLabel("Limit");
    private final JLabel stopPriceLabel = new JLabel("Stop");

    private final JLabel messageLabel = new JLabel(" ");
    private final JButton submitButton = new JButton("Submit");
    private final JButton submitButton2 = new JButton("Submit2");
    private final JButton nosCancelButton = new JButton("NosCancel");
    private final JButton nosAmendButton = new JButton("NosAmend");
    private final JButton stressButton = new JButton("Stress");
    private final JButton stopStressButton = new JButton("StopStress");

    private volatile boolean isStressed = false;
    
    private OrderTableModel orderTableModel = null;
    private transient BanzaiApplication application = null;

    private final GridBagConstraints constraints = new GridBagConstraints();

    public OrderEntryPanel(final OrderTableModel orderTableModel,
                final BanzaiApplication application) {
        setName("OrderEntryPanel");
        this.orderTableModel = orderTableModel;
        this.application = application;

        application.addLogonObserver(this);

        SubmitActivator activator = new SubmitActivator();
        symbolTextField.addKeyListener(activator);
        quantityTextField.addKeyListener(activator);
        limitPriceTextField.addKeyListener(activator);
        stopPriceTextField.addKeyListener(activator);
        sessionComboBox.addItemListener(activator);

        setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        setLayout(new GridBagLayout());
        createComponents();
        symbolTextField.setText("BTC/USDC");
        quantityTextField.setText("1");
        limitPriceTextField.setText("100");
        sideComboBox.setSelectedItem(OrderSide.BUY);
        typeComboBox.setSelectedItem(OrderType.LIMIT);
        tifComboBox.setSelectedItem(OrderTIF.DAY);
    }

    public void addActionListener(ActionListener listener) {
        submitButton.addActionListener(listener);
    }

    public void setMessage(String message) {
        messageLabel.setText(message);
        if (message == null || message.equals(""))
            messageLabel.setText(" ");
    }

    public void clearMessage() {
        setMessage(null);
    }

    private void createComponents() {
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1;

        int x = 0;
        int y = 0;

        add(new JLabel("Symbol"), x, y);
        add(new JLabel("Quantity"), ++x, y);
        add(new JLabel("Side"), ++x, y);
        add(new JLabel("Type"), ++x, y);
        constraints.ipadx = 30;
        add(limitPriceLabel, ++x, y);
        add(stopPriceLabel, ++x, y);
        constraints.ipadx = 0;
        add(new JLabel("TIF"), ++x, y);
        constraints.ipadx = 30;

        symbolTextField.setName("SymbolTextField");
        add(symbolTextField, x = 0, ++y);
        constraints.ipadx = 0;
        quantityTextField.setName("QuantityTextField");
        add(quantityTextField, ++x, y);
        sideComboBox.setName("SideComboBox");
        add(sideComboBox, ++x, y);
        typeComboBox.setName("TypeComboBox");
        add(typeComboBox, ++x, y);
        limitPriceTextField.setName("LimitPriceTextField");
        add(limitPriceTextField, ++x, y);
        stopPriceTextField.setName("StopPriceTextField");
        add(stopPriceTextField, ++x, y);
        tifComboBox.setName("TifComboBox");
        add(tifComboBox, ++x, y);

        constraints.insets = new Insets(3, 0, 0, 0);
//        constraints.gridwidth = GridBagConstraints.RELATIVE;
        sessionComboBox.setName("SessionComboBox");
        x = 0;
        add(sessionComboBox, x, ++y);
//        x = 0;
        add(nosCancelButton, ++x, y);
        add(nosAmendButton, ++x, y);
        add(stressButton, ++x, y);
        add(stopStressButton, ++x, y);
        add(submitButton, ++x, y);
        add(submitButton2, ++x, y);
//        constraints.gridwidth = GridBagConstraints.REMAINDER;
//        submitButton.setName("SubmitButton");
        constraints.gridwidth = 0;
        add(messageLabel, 0, ++y);

        typeComboBox.addItemListener(new PriceListener());
        typeComboBox.setSelectedItem(OrderType.STOP);
        typeComboBox.setSelectedItem(OrderType.MARKET);

        Font font = new Font(messageLabel.getFont().getFontName(), Font.BOLD, 12);
        messageLabel.setFont(font);
        messageLabel.setForeground(Color.red);
        messageLabel.setHorizontalAlignment(JLabel.CENTER);
        submitButton.setEnabled(true);
        submitButton.addActionListener(new NewOrderSingleListener());
        submitButton2.setEnabled(true);
        submitButton2.addActionListener(new NewOrderSingleListener2());
        nosCancelButton.setEnabled(true);
        nosAmendButton.setEnabled(true);
        stressButton.setEnabled(true);
        stopStressButton.setEnabled(true);
        nosCancelButton.addActionListener(new NosCancelListener());
        nosAmendButton.addActionListener(new NosAmendListener());
        stressButton.addActionListener(new StressListener());
        stopStressButton.addActionListener(new StopStressListener());
        activateSubmit();
    }

    private JComponent add(JComponent component, int x, int y) {
        constraints.gridx = x;
        constraints.gridy = y;
        add(component, constraints);
        return component;
    }

    private void activateSubmit() {
//        OrderType type = (OrderType) typeComboBox.getSelectedItem();
//        boolean activate = symbolEntered && quantityEntered && sessionEntered;
//
//        if (type == OrderType.MARKET)
//            submitButton.setEnabled(activate);
//        else if (type == OrderType.LIMIT)
//            submitButton.setEnabled(activate && limitEntered);
//        else if (type == OrderType.STOP)
//            submitButton.setEnabled(activate && stopEntered);
//        else if (type == OrderType.STOP_LIMIT)
//            submitButton.setEnabled(activate && limitEntered && stopEntered);
    }

    private class PriceListener implements ItemListener {
        public void itemStateChanged(ItemEvent e) {
            OrderType item = (OrderType) typeComboBox.getSelectedItem();
            if (item == OrderType.MARKET) {
                enableLimitPrice(false);
                enableStopPrice(false);
            } else if (item == OrderType.STOP) {
                enableLimitPrice(false);
                enableStopPrice(true);
            } else if (item == OrderType.LIMIT) {
                enableLimitPrice(true);
                enableStopPrice(false);
            } else {
                enableLimitPrice(true);
                enableStopPrice(true);
            }
            activateSubmit();
        }

        private void enableLimitPrice(boolean enabled) {
            Color labelColor = enabled ? Color.black : Color.gray;
            Color bgColor = enabled ? Color.white : Color.gray;
            limitPriceTextField.setEnabled(enabled);
            limitPriceTextField.setBackground(bgColor);
            limitPriceLabel.setForeground(labelColor);
        }

        private void enableStopPrice(boolean enabled) {
            Color labelColor = enabled ? Color.black : Color.gray;
            Color bgColor = enabled ? Color.white : Color.gray;
            stopPriceTextField.setEnabled(enabled);
            stopPriceTextField.setBackground(bgColor);
            stopPriceLabel.setForeground(labelColor);
        }
    }

    public void update(Observable o, Object arg) {
        LogonEvent logonEvent = (LogonEvent) arg;
        if (logonEvent.isLoggedOn())
            sessionComboBox.addItem(logonEvent.getSessionID());
        else
            sessionComboBox.removeItem(logonEvent.getSessionID());
    }

    private class NewOrderSingleListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            nosSubmit();
        }
    }

    private class NewOrderSingleListener2 implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            nosSubmit2();
        }
    }

    private class NosCancelListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            Order order = nosSubmit();
            application.cancel(order);
        }
    }

    private class StressListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            new Thread(()-> {
                isStressed = true;
                LOGGER.info("SystemOnStress");
                while (isStressed) {
                    Order order = nosSubmit();
                    application.cancel(order);
                }
                LOGGER.info("SystemOnStress: DONE");
            }, "systemOnStress").start();
        }
    }

    private class StopStressListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            new Thread(()-> {
                isStressed = false;
            }, "StopStress").start();
        }
    }
    
    private class NosAmendListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            Order order = nosSubmit();
            Order newOrder = (Order) order.clone();
            newOrder.setQuantity(8.00);
            application.replace(order, newOrder);
        }
    }

    private Order nosSubmit2() {
        Order order = new Order();
        order.setSide((OrderSide) sideComboBox.getSelectedItem());
        order.setType((OrderType) typeComboBox.getSelectedItem());
        order.setTIF((OrderTIF) tifComboBox.getSelectedItem());

        order.setSymbol(symbolTextField.getText());
        order.setQuantity(Double.parseDouble(quantityTextField.getText()));
        order.setOpen(order.getQuantity());
        order.setText("extraField");

        OrderType type = order.getType();
        if (type == OrderType.LIMIT || type == OrderType.STOP_LIMIT)
            order.setLimit(limitPriceTextField.getText());
        if (type == OrderType.STOP || type == OrderType.STOP_LIMIT)
            order.setStop(stopPriceTextField.getText());
        order.setSessionID((SessionID) sessionComboBox.getSelectedItem());

        orderTableModel.addOrder(order);
        application.send(order);
        return order;
    }

    private Order nosSubmit() {
        Order order = new Order();
        order.setSide((OrderSide) sideComboBox.getSelectedItem());
        order.setType((OrderType) typeComboBox.getSelectedItem());
        order.setTIF((OrderTIF) tifComboBox.getSelectedItem());

        order.setSymbol(symbolTextField.getText());
        order.setQuantity(Double.parseDouble(quantityTextField.getText()));
        order.setOpen(order.getQuantity());

        OrderType type = order.getType();
        if (type == OrderType.LIMIT || type == OrderType.STOP_LIMIT)
            order.setLimit(limitPriceTextField.getText());
        if (type == OrderType.STOP || type == OrderType.STOP_LIMIT)
            order.setStop(stopPriceTextField.getText());
        order.setSessionID((SessionID) sessionComboBox.getSelectedItem());

        orderTableModel.addOrder(order);
        application.send(order);
        return order;
    }

    private SessionID getSessionID() {
        return (SessionID) sessionComboBox.getSelectedItem();
    }

    private class SubmitActivator implements KeyListener, ItemListener {
        public void keyReleased(KeyEvent e) {
            Object obj = e.getSource();
            if (obj == symbolTextField) {
                symbolEntered = testField(obj);
            } else if (obj == quantityTextField) {
                quantityEntered = testField(obj);
            } else if (obj == limitPriceTextField) {
                limitEntered = testField(obj);
            } else if (obj == stopPriceTextField) {
                stopEntered = testField(obj);
            }
            if(e.getKeyCode() == 10) nosSubmit();
            activateSubmit();
        }

        public void itemStateChanged(ItemEvent e) {
            sessionEntered = sessionComboBox.getSelectedItem() != null;
            activateSubmit();
            application.setSessionID((SessionID) sessionComboBox.getSelectedItem());
        }

        private boolean testField(Object o) {
            String value = ((JTextField) o).getText();
            value = value.trim();
            return value.length() > 0;
        }

        public void keyTyped(KeyEvent e) {}

        public void keyPressed(KeyEvent e) {}
    }
}

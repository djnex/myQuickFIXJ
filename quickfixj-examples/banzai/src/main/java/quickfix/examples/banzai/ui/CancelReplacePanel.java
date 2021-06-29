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

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import quickfix.examples.banzai.*;
import quickfix.field.MassCancelRequestType;
import quickfix.field.Side;

public class CancelReplacePanel extends JPanel {
    private final JLabel quantityLabel = new JLabel("Quantity");
    private final JLabel limitPriceLabel = new JLabel("Limit");
    private final JLabel stopLabel = new JLabel("Stop");
    private final SignedDoubleNumberTextField quantityTextField = new SignedDoubleNumberTextField();
    private final SignedDoubleNumberTextField limitPriceTextField = new SignedDoubleNumberTextField();
    private final SignedDoubleNumberTextField stopPriceTextField = new SignedDoubleNumberTextField();
    private final JButton cancelButton = new JButton("Cancel");
    private final JButton cancelByOrderIdButton = new JButton("CancelByOrderId");
    private final JButton massCancelButton = new JButton("MC");
    private final JButton massCancelButtonSym = new JButton("MC_Symbol");
    private final JButton massCancelButtonUSym = new JButton("MC_Underlying");
    private final JButton replaceButton = new JButton("Replace");
    private final JComboBox typeComboBox = new JComboBox(OrderType.toArray());
    private final JComboBox sideComboBox = new JComboBox(OrderSide.toArray());
    private Order order = null;

    private final GridBagConstraints constraints = new GridBagConstraints();

    private final BanzaiApplication application;

    public CancelReplacePanel(final BanzaiApplication application) {
        this.application = application;
        cancelButton.addActionListener(new CancelListener());
        cancelByOrderIdButton.addActionListener(new CancelByOrderIdListener());
        massCancelButton.addActionListener(new MassCancelListener());
        massCancelButtonSym.addActionListener(new MassCancelListenerSymbol());
        massCancelButtonUSym.addActionListener(new MassCancelListenerUnderlyingSymbol());
        replaceButton.addActionListener(new ReplaceListener());
        typeComboBox.setSelectedItem(OrderType.LIMIT);
        sideComboBox.setSelectedItem(Side.BUY);
        setLayout(new GridBagLayout());
        createComponents();
    }

    public void addActionListener(ActionListener listener) {
        cancelButton.addActionListener(listener);
        cancelByOrderIdButton.addActionListener(listener);
        massCancelButton.addActionListener(listener);
        massCancelButtonSym.addActionListener(listener);
        massCancelButtonUSym.addActionListener(listener);
        replaceButton.addActionListener(listener);
    }

    private void createComponents() {
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1;

        int x = 0;
        int y = 0;

        constraints.insets = new Insets(0, 0, 5, 5);
        add(cancelButton, x, y);
        add(cancelByOrderIdButton, ++x, y);
        add(massCancelButton, ++x, y);
        add(massCancelButtonSym, ++x, y);
        add(massCancelButtonUSym, ++x, y);
        x = 0;
        add(replaceButton, x, ++y);
        typeComboBox.setName("TypeComboBox");
        add(typeComboBox, ++x, y);
        sideComboBox.setName("SideComboBox");
        add(sideComboBox, ++x, y);
        constraints.weightx = 0;
        add(quantityLabel, ++x, y);
        constraints.weightx = 5;
        add(quantityTextField, ++x, y);
        constraints.weightx = 0;
        add(limitPriceLabel, ++x, y);
        constraints.weightx = 5;
        add(limitPriceTextField, ++x, y);
        add(stopLabel, ++x, y);
        add(stopPriceTextField, ++x, y);
    }

    public void setEnabled(boolean enabled) {
        cancelButton.setEnabled(enabled);
        cancelByOrderIdButton.setEnabled(enabled);
        massCancelButton.setEnabled(true);
        massCancelButtonSym.setEnabled(true);
        massCancelButtonUSym.setEnabled(true);
        replaceButton.setEnabled(true);
        quantityTextField.setEnabled(enabled);
        limitPriceTextField.setEnabled(enabled);
        stopPriceTextField.setEnabled(enabled);

        Color labelColor = enabled ? Color.black : Color.gray;
        Color bgColor = enabled ? Color.white : Color.gray;
        quantityTextField.setBackground(bgColor);
        limitPriceTextField.setBackground(bgColor);
        stopPriceTextField.setBackground(bgColor);
        quantityLabel.setForeground(labelColor);
        limitPriceLabel.setForeground(labelColor);
        stopLabel.setForeground(labelColor);
    }

    public void update() {
        setOrder(this.order);
    }

    public void setOrder(Order order) {
        if (order == null)
            return;
        this.order = order;
        quantityTextField.setText
        (Double.toString(order.getQuantity()));

        Double limit = order.getLimit();
        if (limit != null)
            limitPriceTextField.setText(order.getLimit().toString());

        Double stop = order.getStop();
        if (stop != null)
            stopPriceTextField.setText(stop.toString());
        
        OrderType orderType = order.getType();
        if (orderType != null) {
            typeComboBox.setSelectedItem(orderType);
        }
        OrderSide side = order.getSide();
        if (side != null) {
            sideComboBox.setSelectedItem(side);
        }
        setEnabled(order.getOpen() > 0);
    }

    private JComponent add(JComponent component, int x, int y) {
        constraints.gridx = x;
        constraints.gridy = y;
        add(component, constraints);
        return component;
    }

    private class CancelListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            application.cancel(order);
        }
    }

    private class CancelByOrderIdListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            application.cancel44_byOrderId(order);
        }
    }

    private class MassCancelListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            application.massCancel(order, new MassCancelRequestType(MassCancelRequestType.CANCEL_ALL_ORDERS));
        }
    }

    private class MassCancelListenerSymbol implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            application.massCancel(order, new MassCancelRequestType(MassCancelRequestType.CANCEL_ORDERS_FOR_A_SECURITY));
        }
    }
    
    private class MassCancelListenerUnderlyingSymbol implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            application.massCancel(order, new MassCancelRequestType(MassCancelRequestType.CANCEL_ORDERS_FOR_AN_UNDERLYING_SECURITY));
        }
    }

    private class ReplaceListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            Order newOrder = (Order) order.clone();
            newOrder.setQuantity(Double.parseDouble(quantityTextField.getText()));
            OrderType orderType = (OrderType) typeComboBox.getSelectedItem();
            newOrder.setType(orderType);
            if (orderType == OrderType.LIMIT || orderType == OrderType.STOP_LIMIT) {
                newOrder.setLimit(Double.parseDouble(limitPriceTextField.getText()));    
            } else {
                newOrder.setLimit("");
            }
            if (orderType == OrderType.STOP_LIMIT) {
                newOrder.setStop(stopPriceTextField.getText());
            }
            newOrder.setSide((OrderSide) sideComboBox.getSelectedItem());
            newOrder.setRejected(false);
            newOrder.setCanceled(false);
//            newOrder.setOpen(0);
//            newOrder.setExecuted(0);
            newOrder.setSymbol(order.getSymbol());
            application.replace(order, newOrder);
        }
    }
}

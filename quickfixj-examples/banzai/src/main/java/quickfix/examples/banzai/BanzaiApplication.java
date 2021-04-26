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

package quickfix.examples.banzai;

import diginexMessages.DigiOrderMassCancelRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.Application;
import quickfix.DefaultMessageFactory;
import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.FixVersions;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.RejectLogon;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.UnsupportedMessageType;
import quickfix.field.*;
import quickfix.fix44.MarketDataRequest;
import quickfix.fix44.OrderMassCancelReport;

import javax.swing.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Observable;
import java.util.Observer;

public class BanzaiApplication implements Application {
    private final static Logger LOGGER = LoggerFactory.getLogger(BanzaiApplication.class);
    private final DefaultMessageFactory messageFactory = new DefaultMessageFactory();
    private OrderTableModel orderTableModel = null;
    private ExecutionTableModel executionTableModel = null;
    private final ObservableOrder observableOrder = new ObservableOrder();
    private final ObservableLogon observableLogon = new ObservableLogon();
    private boolean isAvailable = true;
    private boolean isMissingField;
    private SessionID sessionID;

    static private final TwoWayMap sideMap = new TwoWayMap();
    static private final TwoWayMap typeMap = new TwoWayMap();
    static private final TwoWayMap tifMap = new TwoWayMap();
    static private final HashMap<SessionID, HashSet<String>> execIDs = new HashMap<>();

    public BanzaiApplication(OrderTableModel orderTableModel,
            ExecutionTableModel executionTableModel) {
        this.orderTableModel = orderTableModel;
        this.executionTableModel = executionTableModel;
    }

    public void onCreate(SessionID sessionID) {
    }

    public void onLogon(SessionID sessionID) {
        observableLogon.logon(sessionID);
    }

    public void onLogout(SessionID sessionID) {
        observableLogon.logoff(sessionID);
    }

    public void toAdmin(quickfix.Message message, SessionID sessionID) {
        sending(message);
    }

    public void toApp(quickfix.Message message, SessionID sessionID) throws DoNotSend {
        sending(message);
    }

    private void sending(Message message) {
        SwingUtilities.invokeLater(() -> LOGGER.info("=====> sending message, type: {}", message.getClass().getSimpleName()));
    }

    private void received(Message message, SessionID sessionID) {
        SwingUtilities.invokeLater(new MessageProcessor(message, sessionID));
    }

    public void fromAdmin(quickfix.Message message, SessionID sessionID) throws FieldNotFound,
            IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        received(message, sessionID);
    }

    public void fromApp(quickfix.Message message, SessionID sessionID) throws FieldNotFound,
            IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        received(message, sessionID);
    }

    public class MessageProcessor implements Runnable {
        private final quickfix.Message message;
        private final SessionID sessionID;

        public MessageProcessor(quickfix.Message message, SessionID sessionID) {
            this.message = message;
            this.sessionID = sessionID;
        }

        public void run() {
            try {
                LOGGER.info("=====> received message, type: {}", message.getClass().getSimpleName());
                MsgType msgType = new MsgType();
                if (isAvailable) {
                    if (isMissingField) {
                        // For OpenFIX certification testing
//                        sendBusinessReject(message, BusinessRejectReason.CONDITIONALLY_REQUIRED_FIELD_MISSING, "Conditionally required field missing");
                        LOGGER.error("Conditionally required field missing");
                    }
                    else if (message.getHeader().isSetField(DeliverToCompID.FIELD)) {
                        // This is here to support OpenFIX certification
//                        sendSessionReject(message, SessionRejectReason.COMPID_PROBLEM);
                        LOGGER.error("sendSessionReject COMPID_PROBLEM");
                    } else if (message.getHeader().getField(msgType).valueEquals("8")) {
                        executionReport(message, sessionID);
                    } else if (message.getHeader().getField(msgType).valueEquals("9")) {
                        cancelReject(message, sessionID);
                    }
                } else {
//                    sendBusinessReject(message, BusinessRejectReason.APPLICATION_NOT_AVAILABLE,
//                            "Application not available");
                    LOGGER.error("Application not available");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setSessionID(SessionID sessionID) {
        this.sessionID = sessionID;
    }

    public SessionID getSessionID() {
        return sessionID;
    }

    private void sendSessionReject(Message message, int rejectReason) throws FieldNotFound,
            SessionNotFound {
        Message reply = createMessage(message, MsgType.REJECT);
        reverseRoute(message, reply);
        String refSeqNum = message.getHeader().getString(MsgSeqNum.FIELD);
        reply.setString(RefSeqNum.FIELD, refSeqNum);
        reply.setString(RefMsgType.FIELD, message.getHeader().getString(MsgType.FIELD));
        reply.setInt(SessionRejectReason.FIELD, rejectReason);
        Session.sendToTarget(reply);
    }

    private void sendBusinessReject(Message message, int rejectReason, String rejectText)
            throws FieldNotFound, SessionNotFound {
        Message reply = createMessage(message, MsgType.BUSINESS_MESSAGE_REJECT);
        reverseRoute(message, reply);
        String refSeqNum = message.getHeader().getString(MsgSeqNum.FIELD);
        reply.setString(RefSeqNum.FIELD, refSeqNum);
        reply.setString(RefMsgType.FIELD, message.getHeader().getString(MsgType.FIELD));
        reply.setInt(BusinessRejectReason.FIELD, rejectReason);
        reply.setString(Text.FIELD, rejectText);
        Session.sendToTarget(reply);
    }

    private Message createMessage(Message message, String msgType) throws FieldNotFound {
        return messageFactory.create(message.getHeader().getString(BeginString.FIELD), msgType);
    }

    private void reverseRoute(Message message, Message reply) throws FieldNotFound {
        reply.getHeader().setString(SenderCompID.FIELD,
                message.getHeader().getString(TargetCompID.FIELD));
        reply.getHeader().setString(TargetCompID.FIELD,
                message.getHeader().getString(SenderCompID.FIELD));
    }

    private void executionReport(Message message, SessionID sessionID) throws FieldNotFound {

        OrderID orderID = (OrderID) message.getField(new OrderID());
        if (orderID == null/* || alreadyProcessed(orderID.getValue(), sessionID)*/) {
            LOGGER.error("{} don't exists", orderID);
            return;            
        }

        Order order = orderTableModel.getOrder(message.getField(new ClOrdID()).getValue());
        if (order == null) {
            return;
        }
        order.setOrderId(orderID.getValue());

//        if (message.getField(new Price()) != null) {
//            double updatedValue = message.getField(new Price()).getValue();
//            if (Double.compare(updatedValue, order.getLimit()) != 0) {
//                order.setLimit(updatedValue);
//            }    
//        }
//
//        if (message.getField(new AvgPx()) != null) {
//            double updatedValue = message.getField(new AvgPx()).getValue();
//            if (Double.compare(updatedValue, order.getAvgPx()) != 0) {
//                order.setAvgPx(updatedValue);
//            }
//        }
//        
//        double orderQty = message.getField(new OrderQty()).getValue();
//        if (order.getQuantity() != (int) orderQty) {
//            order.setQuantity((int) orderQty);
//        }
//        double cumQty = message.getField(new CumQty()).getValue();
//        double openQty = orderQty - cumQty;
//        if (Double.compare(order.getOpen(), openQty) != 0) {
//            order.setOpen((int) openQty);
//        }

        BigDecimal fillSize;

        if (message.isSetField(LastShares.FIELD)) {
            LastShares lastShares = new LastShares();
            message.getField(lastShares);
            fillSize = new BigDecimal("" + lastShares.getValue());
        } else {
            // > FIX 4.1
            LeavesQty leavesQty = new LeavesQty();
            message.getField(leavesQty);
            fillSize = BigDecimal.valueOf(order.getQuantity()).subtract(new BigDecimal("" + leavesQty.getValue()));
        }

        if (fillSize.compareTo(BigDecimal.ZERO) > 0) {
            order.setOpen(BigDecimal.valueOf(order.getOpen()).subtract(fillSize).doubleValue());
            order.setExecuted(Double.parseDouble(message.getString(CumQty.FIELD)));
            order.setAvgPx(Double.parseDouble(message.getString(AvgPx.FIELD)));
        }

        OrdStatus ordStatus = (OrdStatus) message.getField(new OrdStatus());
        ExecType execType = (ExecType) message.getField(new ExecType());

        if (ordStatus.valueEquals(OrdStatus.REJECTED)) {
            order.setRejected(true);
            order.setOpen(0.00);
        } else if (ordStatus.valueEquals(OrdStatus.CANCELED)
                || ordStatus.valueEquals(OrdStatus.DONE_FOR_DAY)) {
            order.setCanceled(true);
            order.setOpen(0.00);
        } else if (ordStatus.valueEquals(OrdStatus.NEW)) {
            if (order.isNew()) {
                order.setNew(false);
            }
        } else if (ordStatus.valueEquals(OrdStatus.FILLED)) {
            order.setOpen(0.00);
            order.setCanceled(true);
        } else if (ordStatus.valueEquals(OrdStatus.PARTIALLY_FILLED) && execType.valueEquals(ExecType.REPLACED)) {
            OrderQty orderQty = (OrderQty) message.getField(new OrderQty());
            CumQty cumQty = (CumQty) message.getField(new CumQty());
            LeavesQty leavesQty = (LeavesQty) message.getField(new LeavesQty());
            if (leavesQty.getValue() > 0) {
                order.setOpen(leavesQty.getValue());
            } else {
                order.setOpen(0.00);
                order.setCanceled(true);
            }
            order.setQuantity(orderQty.getValue());
            order.setExecuted(cumQty.getValue());
        }
        if (execType.valueEquals(ExecType.REPLACED)) {
            Price price = (Price) message.getField(new Price());
            if (!price.valueEquals(order.getLimit())) {
                order.setLimit(price.getValue());
            }
            OrderQty orderQty = (OrderQty) message.getField(new OrderQty());
            if (!orderQty.valueEquals(order.getQuantity())) {
                order.setQuantity(orderQty.getValue());
            }
            LeavesQty leavesQty = (LeavesQty) message.getField(new LeavesQty());
            if (!leavesQty.valueEquals(order.getOpen())) {
                order.setOpen(leavesQty.getValue());
            }
        }

        try {
            order.setMessage(message.getField(new Text()).getValue());
        } catch (FieldNotFound e) {
        }

        orderTableModel.updateOrder(order, message.getField(new ClOrdID()).getValue());
        observableOrder.update(order);

        if (fillSize.compareTo(BigDecimal.ZERO) > 0) {
            Execution execution = new Execution();
            execution.setExchangeID(sessionID + message.getField(new ExecID()).getValue());

            execution.setSymbol(message.getField(new Symbol()).getValue());
            execution.setQuantity(fillSize.intValue());
            if (message.isSetField(LastPx.FIELD)) {
                execution.setPrice(Double.parseDouble(message.getString(LastPx.FIELD)));
            }
            Side side = (Side) message.getField(new Side());
            execution.setSide(FIXSideToSide(side));
            executionTableModel.addExecution(execution);
        }
    }

    private void cancelReject(Message message, SessionID sessionID) throws FieldNotFound {

        String id = message.getField(new ClOrdID()).getValue();
        Order order = orderTableModel.getOrder(id);
        if (order == null)
            return;
        if (order.getOriginalID() != null)
            order = orderTableModel.getOrder(order.getOriginalID());

        try {
            order.setMessage(message.getField(new Text()).getValue());
        } catch (FieldNotFound e) {
        }
        orderTableModel.updateOrder(order, message.getField(new OrigClOrdID()).getValue());
    }

    private boolean alreadyProcessed(String execID, SessionID sessionID) {
        HashSet<String> set = execIDs.get(sessionID);
        if (set == null) {
            set = new HashSet<>();
            set.add(execID);
            execIDs.put(sessionID, set);
            return false;
        } else {
            if (set.contains(execID))
                return true;
            set.add(execID);
            return false;
        }
    }

    private void send(quickfix.Message message, SessionID sessionID) {
        try {
            Session.sendToTarget(message, sessionID);
        } catch (SessionNotFound e) {
            System.out.println(e);
        }
    }

    public void send(Order order) {
        String beginString = order.getSessionID().getBeginString();
        switch (beginString) {
            case FixVersions.BEGINSTRING_FIX40:
                send40(order);
                break;
            case FixVersions.BEGINSTRING_FIX41:
                send41(order);
                break;
            case FixVersions.BEGINSTRING_FIX42:
                send42(order);
                break;
            case FixVersions.BEGINSTRING_FIX43:
                send43(order);
                break;
            case FixVersions.BEGINSTRING_FIX44:
                send44(order);
                break;
            case FixVersions.BEGINSTRING_FIXT11:
                send50(order);
                break;
        }
    }

    public void send40(Order order) {
        quickfix.fix40.NewOrderSingle newOrderSingle = new quickfix.fix40.NewOrderSingle(
                new ClOrdID(order.getID()), new HandlInst('1'), new Symbol(order.getSymbol()),
                sideToFIXSide(order.getSide()), new OrderQty(order.getQuantity()),
                typeToFIXType(order.getType()));

        send(populateOrder(order, newOrderSingle), order.getSessionID());
    }

    public void send41(Order order) {
        quickfix.fix41.NewOrderSingle newOrderSingle = new quickfix.fix41.NewOrderSingle(
                new ClOrdID(order.getID()), new HandlInst('1'), new Symbol(order.getSymbol()),
                sideToFIXSide(order.getSide()), typeToFIXType(order.getType()));
        newOrderSingle.set(new OrderQty(order.getQuantity()));

        send(populateOrder(order, newOrderSingle), order.getSessionID());
    }

    public void send42(Order order) {
        quickfix.fix42.NewOrderSingle newOrderSingle = new quickfix.fix42.NewOrderSingle(
                new ClOrdID(order.getID()), new HandlInst('1'), new Symbol(order.getSymbol()),
                sideToFIXSide(order.getSide()), new TransactTime(), typeToFIXType(order.getType()));
        newOrderSingle.set(new OrderQty(order.getQuantity()));

        send(populateOrder(order, newOrderSingle), order.getSessionID());
    }

    public void send43(Order order) {
        quickfix.fix43.NewOrderSingle newOrderSingle = new quickfix.fix43.NewOrderSingle(
                new ClOrdID(order.getID()), new HandlInst('1'), sideToFIXSide(order.getSide()),
                new TransactTime(), typeToFIXType(order.getType()));
        newOrderSingle.set(new OrderQty(order.getQuantity()));
        newOrderSingle.set(new Symbol(order.getSymbol()));
        send(populateOrder(order, newOrderSingle), order.getSessionID());
    }

    public void send44(Order order) {
        quickfix.fix44.NewOrderSingle newOrderSingle = new quickfix.fix44.NewOrderSingle(
                new ClOrdID(order.getID()), sideToFIXSide(order.getSide()),
                new TransactTime(), typeToFIXType(order.getType()));
        newOrderSingle.set(new OrderQty(order.getQuantity()));
        newOrderSingle.set(new Symbol(order.getSymbol()));
        newOrderSingle.set(new HandlInst('1'));
        newOrderSingle.set(new Account(Banzai.getAccount()));
        send(populateOrder(order, newOrderSingle), order.getSessionID());
    }
    
    public void sendMDRequest() {
        LOGGER.info("sending MDRequest");
        quickfix.fix44.MarketDataRequest message = new quickfix.fix44.MarketDataRequest(
                new MDReqID(String.valueOf(System.currentTimeMillis())), 
                new SubscriptionRequestType(SubscriptionRequestType.SNAPSHOT_UPDATES),
                new MarketDepth(0));
        send(message, getSessionID());
        LOGGER.info("sending MDRequest:DONE");
    }

    public void send50(Order order) {
        quickfix.fix50.NewOrderSingle newOrderSingle = new quickfix.fix50.NewOrderSingle(
                new ClOrdID(order.getID()), sideToFIXSide(order.getSide()),
                new TransactTime(), typeToFIXType(order.getType()));
        newOrderSingle.set(new OrderQty(order.getQuantity()));
        newOrderSingle.set(new Symbol(order.getSymbol()));
        newOrderSingle.set(new HandlInst('1'));
        send(populateOrder(order, newOrderSingle), order.getSessionID());
    }

    public quickfix.Message populateOrder(Order order, quickfix.Message newOrderSingle) {

        OrderType type = order.getType();

        if (type == OrderType.LIMIT)
            newOrderSingle.setField(new Price(order.getLimit()));
        else if (type == OrderType.STOP) {
            newOrderSingle.setField(new StopPx(order.getStop()));
            newOrderSingle.setField(new TargetStrategy(150));
        } else if (type == OrderType.STOP_LIMIT) {
            newOrderSingle.setField(new Price(order.getLimit()));
            newOrderSingle.setField(new StopPx(order.getStop()));
        }

        if (order.getSide() == OrderSide.SHORT_SELL
                || order.getSide() == OrderSide.SHORT_SELL_EXEMPT) {
            newOrderSingle.setField(new LocateReqd(false));
        }

        newOrderSingle.setField(tifToFIXTif(order.getTIF()));
        return newOrderSingle;
    }

    public void cancel(Order order) {
        String beginString = order.getSessionID().getBeginString();
        switch (beginString) {
            case "FIX.4.0":
                cancel40(order);
                break;
            case "FIX.4.1":
                cancel41(order);
                break;
            case "FIX.4.2":
                cancel42(order);
                break;
            case "FIX.4.4":
                cancel44(order);
                break;
            default:
                LOGGER.info("cancel not supported for {}", beginString);
                break;
        }
    }

    public void cancel40(Order order) {
        String id = order.generateID();
        quickfix.fix40.OrderCancelRequest message = new quickfix.fix40.OrderCancelRequest(
                new OrigClOrdID(order.getID()), new ClOrdID(id), new CxlType(CxlType.FULL_REMAINING_QUANTITY), new Symbol(order
                        .getSymbol()), sideToFIXSide(order.getSide()), new OrderQty(order
                        .getQuantity()));

        orderTableModel.addID(order, id);
        send(message, order.getSessionID());
    }

    public void cancel41(Order order) {
        String id = order.generateID();
        quickfix.fix41.OrderCancelRequest message = new quickfix.fix41.OrderCancelRequest(
                new OrigClOrdID(order.getID()), new ClOrdID(id), new Symbol(order.getSymbol()),
                sideToFIXSide(order.getSide()));
        message.setField(new OrderQty(order.getQuantity()));

        orderTableModel.addID(order, id);
        send(message, order.getSessionID());
    }

    public void cancel42(Order order) {
        String id = order.generateID();
        quickfix.fix42.OrderCancelRequest message = new quickfix.fix42.OrderCancelRequest(
                new OrigClOrdID(order.getID()), new ClOrdID(id), new Symbol(order.getSymbol()),
                sideToFIXSide(order.getSide()), new TransactTime());
        message.setField(new OrderQty(order.getQuantity()));

        orderTableModel.addID(order, id);
        send(message, order.getSessionID());
    }

    public void cancel44(Order order) {
        String id = order.generateID();
        quickfix.fix44.OrderCancelRequest message = new quickfix.fix44.OrderCancelRequest(
                new OrigClOrdID(order.getID()), new ClOrdID(id), sideToFIXSide(order.getSide()), new TransactTime());
        message.setField(new OrderQty(order.getQuantity()));
        message.setField(new Symbol(order.getSymbol()));
        message.set(new Account(Banzai.getAccount()));
        
        orderTableModel.addID(order, id);
        send(message, order.getSessionID());
    }

    public void massCancel(Order order, MassCancelRequestType massCancelRequestType) {
        String beginString = order.getSessionID().getBeginString();
        switch (beginString) {
            case "FIX.4.4":
                massCancel44(order, massCancelRequestType);
                break;
            default:
                LOGGER.info("massCancel not supported for {}", beginString);
                break;
        }
    }

    public void massCancel44(Order order, MassCancelRequestType massCancelRequestType) {
        String id = Order.generateID();
        DigiOrderMassCancelRequest message = new DigiOrderMassCancelRequest(new ClOrdID(id), massCancelRequestType, new TransactTime());
        message.set(new Account(Banzai.getAccount()));
        if (massCancelRequestType.getValue() == MassCancelRequestType.CANCEL_ORDERS_FOR_A_SECURITY) message.set(new Symbol(order.getSymbol()));
        else  if (massCancelRequestType.getValue() == MassCancelRequestType.CANCEL_ORDERS_FOR_AN_UNDERLYING_SECURITY) message.set(new UnderlyingSymbol(order.getSymbol()));
        orderTableModel.addID(order, id);
        send(message, order.getSessionID());
    }

    public void replace(Order order, Order newOrder) {
        String beginString = order.getSessionID().getBeginString();
        switch (beginString) {
            case "FIX.4.0":
                replace40(order, newOrder);
                break;
            case "FIX.4.1":
                replace41(order, newOrder);
                break;
            case "FIX.4.2":
                replace42(order, newOrder);
                break;
            case "FIX.4.4":
                replace44(order, newOrder);
                break;
        }
    }

    public void replace40(Order order, Order newOrder) {
        quickfix.fix40.OrderCancelReplaceRequest message = new quickfix.fix40.OrderCancelReplaceRequest(
                new OrigClOrdID(order.getID()), new ClOrdID(newOrder.getID()), new HandlInst('1'),
                new Symbol(order.getSymbol()), sideToFIXSide(order.getSide()), new OrderQty(
                        newOrder.getQuantity()), typeToFIXType(order.getType()));

        orderTableModel.addID(order, newOrder.getID());
        send(populateCancelReplace(order, newOrder, message), order.getSessionID());
    }

    public void replace41(Order order, Order newOrder) {
        quickfix.fix41.OrderCancelReplaceRequest message = new quickfix.fix41.OrderCancelReplaceRequest(
                new OrigClOrdID(order.getID()), new ClOrdID(newOrder.getID()), new HandlInst('1'),
                new Symbol(order.getSymbol()), sideToFIXSide(order.getSide()), typeToFIXType(order
                        .getType()));

        orderTableModel.addID(order, newOrder.getID());
        send(populateCancelReplace(order, newOrder, message), order.getSessionID());
    }

    public void replace42(Order order, Order newOrder) {
        quickfix.fix42.OrderCancelReplaceRequest message = new quickfix.fix42.OrderCancelReplaceRequest(
                new OrigClOrdID(order.getID()), new ClOrdID(newOrder.getID()), new HandlInst('1'),
                new Symbol(order.getSymbol()), sideToFIXSide(order.getSide()), new TransactTime(),
                typeToFIXType(order.getType()));

        orderTableModel.addID(order, newOrder.getID());
        send(populateCancelReplace(order, newOrder, message), order.getSessionID());
    }

    public void replace44(Order order, Order newOrder) {
        quickfix.fix44.OrderCancelReplaceRequest message = new quickfix.fix44.OrderCancelReplaceRequest(
                new OrigClOrdID(order.getID()), new ClOrdID(newOrder.getID()), sideToFIXSide(newOrder.getSide()), 
                new TransactTime(), typeToFIXType(newOrder.getType()));
        orderTableModel.addID(order, newOrder.getID());
        send(populateCancelReplace(order, newOrder, message), order.getSessionID());
    }

    Message populateCancelReplaceOld(Order order, Order newOrder, quickfix.Message message) {

        if (order.getQuantity() != newOrder.getQuantity())
            message.setField(new OrderQty(newOrder.getQuantity()));
        if (!order.getLimit().equals(newOrder.getLimit()))
            message.setField(new Price(newOrder.getLimit()));
        return message;
    }

    Message populateCancelReplace(Order order, Order newOrder, quickfix.Message message) {
        message.setField(new OrderQty(newOrder.getQuantity()));            
        OrderType orderType = newOrder.getType();
        if (orderType == OrderType.LIMIT || orderType == OrderType.STOP_LIMIT) {
            if (newOrder.getLimit() != null)
                message.setField(new Price(newOrder.getLimit()));
        }
        if (orderType == OrderType.STOP_LIMIT) {
            if (newOrder.getStop() != null)
                message.setField(new StopPx(newOrder.getStop()));
        }
        message.setField(new Account(Banzai.getAccount()));
        if (order.getOrderId() != null) message.setField(new OrderID(order.getOrderId()));
        message.setField(new Symbol(order.getSymbol()));
        message.setField(tifToFIXTif(order.getTIF()));
        //nothing just testing compile
        return message;
    }

    public Side sideToFIXSide(OrderSide side) {
        return (Side) sideMap.getFirst(side);
    }

    public OrderSide FIXSideToSide(Side side) {
        return (OrderSide) sideMap.getSecond(side);
    }

    public OrdType typeToFIXType(OrderType type) {
        return (OrdType) typeMap.getFirst(type);
    }

    public OrderType FIXTypeToType(OrdType type) {
        return (OrderType) typeMap.getSecond(type);
    }

    public TimeInForce tifToFIXTif(OrderTIF tif) {
        return (TimeInForce) tifMap.getFirst(tif);
    }

    public OrderTIF FIXTifToTif(TimeInForce tif) {
        return (OrderTIF) typeMap.getSecond(tif);
    }

    public void addLogonObserver(Observer observer) {
        observableLogon.addObserver(observer);
    }

    public void deleteLogonObserver(Observer observer) {
        observableLogon.deleteObserver(observer);
    }

    public void addOrderObserver(Observer observer) {
        observableOrder.addObserver(observer);
    }

    public void deleteOrderObserver(Observer observer) {
        observableOrder.deleteObserver(observer);
    }

    private static class ObservableOrder extends Observable {
        public void update(Order order) {
            setChanged();
            notifyObservers(order);
            clearChanged();
        }
    }

    private static class ObservableLogon extends Observable {
        public void logon(SessionID sessionID) {
            setChanged();
            notifyObservers(new LogonEvent(sessionID, true));
            clearChanged();
        }

        public void logoff(SessionID sessionID) {
            setChanged();
            notifyObservers(new LogonEvent(sessionID, false));
            clearChanged();
        }
    }

    static {
        sideMap.put(OrderSide.BUY, new Side(Side.BUY));
        sideMap.put(OrderSide.SELL, new Side(Side.SELL));
        sideMap.put(OrderSide.SHORT_SELL, new Side(Side.SELL_SHORT));
        sideMap.put(OrderSide.SHORT_SELL_EXEMPT, new Side(Side.SELL_SHORT_EXEMPT));
        sideMap.put(OrderSide.CROSS, new Side(Side.CROSS));
        sideMap.put(OrderSide.CROSS_SHORT, new Side(Side.CROSS_SHORT));

        typeMap.put(OrderType.MARKET, new OrdType(OrdType.MARKET));
        typeMap.put(OrderType.LIMIT, new OrdType(OrdType.LIMIT));
        typeMap.put(OrderType.STOP, new OrdType(OrdType.STOP_STOP_LOSS));
        typeMap.put(OrderType.STOP_LIMIT, new OrdType(OrdType.STOP_LIMIT));

        tifMap.put(OrderTIF.DAY, new TimeInForce(TimeInForce.DAY));
        tifMap.put(OrderTIF.IOC, new TimeInForce(TimeInForce.IMMEDIATE_OR_CANCEL));
        tifMap.put(OrderTIF.OPG, new TimeInForce(TimeInForce.AT_THE_OPENING));
        tifMap.put(OrderTIF.GTC, new TimeInForce(TimeInForce.GOOD_TILL_CANCEL));
        tifMap.put(OrderTIF.GTX, new TimeInForce(TimeInForce.GOOD_TILL_CROSSING));
    }

    public boolean isMissingField() {
        return isMissingField;
    }

    public void setMissingField(boolean isMissingField) {
        this.isMissingField = isMissingField;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean isAvailable) {
        this.isAvailable = isAvailable;
    }
}

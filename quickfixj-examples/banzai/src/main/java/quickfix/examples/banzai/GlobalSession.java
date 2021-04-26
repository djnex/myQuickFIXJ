package quickfix.examples.banzai;

public class GlobalSession {
    private Order lastOrder;

    public void setLastOrder(Order lastOrder) {
        this.lastOrder = lastOrder;
    }

    public Order getLastOrder() {
        return lastOrder;
    }
}

package diginexMessages;

import quickfix.field.*;
import quickfix.fix44.OrderMassCancelRequest;

public class DigiOrderMassCancelRequest extends OrderMassCancelRequest {

    public DigiOrderMassCancelRequest(ClOrdID clOrdID, MassCancelRequestType massCancelRequestType, TransactTime transactTime) {
        super(clOrdID, massCancelRequestType, transactTime);
    }

    public void set(Account value) {
        setField(value);
    }
}

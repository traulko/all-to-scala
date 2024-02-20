package org.traulka.test2.model;

import org.traulka.test2.model.type.DestResult;

public interface ReadSendClient {
    DestEvent readData();
    DestResult sendData(DestAddress dest, DestPayload destPayload);
}
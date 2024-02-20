package org.traulka.test2.model;

import java.util.List;

public record DestEvent(List<DestAddress> recipients, DestPayload destPayload) {
}
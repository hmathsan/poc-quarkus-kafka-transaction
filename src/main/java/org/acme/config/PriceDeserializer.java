package org.acme.config;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import org.acme.rest.UpdateRequestBody;

public class PriceDeserializer extends ObjectMapperDeserializer<UpdateRequestBody> {
    public PriceDeserializer() {
        super(UpdateRequestBody.class);
    }
}

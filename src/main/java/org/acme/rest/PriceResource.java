package org.acme.rest;

import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import io.smallrye.reactive.messaging.kafka.KafkaRecordBatch;
import io.smallrye.reactive.messaging.kafka.transactions.KafkaTransactions;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.acme.entity.Price;
import org.acme.entity.repositories.PriceRepository;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.util.List;
import java.util.concurrent.CompletionStage;

@Path("/prices")
public class PriceResource {

    @Channel("update-price-out")
    KafkaTransactions<UpdateRequestBody> producer;

    @Channel("update-price-batch-out")
    KafkaTransactions<UpdateRequestBody> producerBatch;

    @Inject
    PriceRepository repository;

    @POST
    public Uni<Void> updatePrice(UpdateRequestBody updateRequestBody) {
        return producer.withTransaction(emitter -> {
            emitter.send(updateRequestBody);
            return Uni.createFrom().voidItem();
        });
    }

    @POST
    @Path("/batch")
    public Uni<Void> updateBatch(List<UpdateRequestBody> updateRequestBodies) {
        return producerBatch.withTransaction(emitter -> {
            for (int i = 0; i < updateRequestBodies.size(); i++) {
                emitter.send(KafkaRecord.of(String.valueOf(i), updateRequestBodies.get(i)));
            }
            return Uni.createFrom().voidItem();
        });
    }

    @GET
    public List<Price> getAllPrices() {
        return repository.listAll(Sort.by("id"));
    }

    @Transactional
    @Incoming("update-price-in")
    public CompletionStage<Void> updatePriceIncoming(Message<UpdateRequestBody> updateRequestBody) {
        UpdateRequestBody payload = updateRequestBody.getPayload();
        Price price = repository.findByIdOptional(payload.id)
                .orElse(new Price());

        price.setPrice(payload.updatedPrice);
        repository.persist(new Price(null, payload.updatedPrice));
        return updateRequestBody.ack();
    }

    @Transactional
    @Incoming("update-price-batch-in")
    public CompletionStage<Void> updatePriceBatchIncoming(KafkaRecordBatch<String, UpdateRequestBody> records) {
        for (KafkaRecord<String, UpdateRequestBody> record : records) {
            UpdateRequestBody updateRequest = record.getPayload();
            Price price = repository.findByIdOptional(updateRequest.id)
                    .orElse(new Price());

            price.setPrice(updateRequest.updatedPrice);
            repository.persist(price);
        }

        return records.ack();
    }
}

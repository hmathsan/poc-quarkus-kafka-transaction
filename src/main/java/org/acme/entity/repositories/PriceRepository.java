package org.acme.entity.repositories;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.entity.Price;

@ApplicationScoped
public class PriceRepository implements PanacheRepository<Price> {

}

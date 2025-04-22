package org.awesomegic.repositoy;

import java.util.List;
import java.util.Optional;

public sealed interface Repository<T,ID> permits InMemoryRepository {
    T save(T entity);
    Optional<T> findById(ID id);
    List<T> findAll();
    boolean deleteById(ID id);
}

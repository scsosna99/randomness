package dev.scottsosna.sandbox.randomness.supplier.type;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class RandomSupplierFactory {
    /**
     * Map of suppliers registered with the factory
     */
    private final Map<String, AbstractRandomSupplier> suppliers = new HashMap<>();

    /**
     * Returns the requested supplier
     * @param key supplier requested
     * @return supplier implementation, when key is valid
     */
    public AbstractRandomSupplier get(String key) {
        return suppliers.get(key);
    }

    /**
     * As a post-construct hook, each supplier implementation registers itself with factory
     * to allow its use by the application.
     * @param supplier supplier implementation
     */
    void register(AbstractRandomSupplier supplier) {
        suppliers.put(supplier.supplierType(), supplier);
        System.out.printf("%s Registered %s\n", Instant.now(), supplier.supplierType());
    }
}

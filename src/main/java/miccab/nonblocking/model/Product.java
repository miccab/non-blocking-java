package miccab.nonblocking.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by michal on 02.08.15.
 */
public class Product {
    private int id;
    private String name;

    @JsonProperty
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @JsonProperty
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static Product createProduct(String name, int id) {
        final Product product = new Product();
        product.setId(id);
        product.setName(name);
        return product;
    }
}

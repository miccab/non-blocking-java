package miccab.nonblocking.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by michal on 16.08.15.
 */
public class ProductGroup {
    private int id;
    private String name;

    @JsonProperty
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public static ProductGroup createGroup(String name, int id) {
        final ProductGroup product = new ProductGroup();
        product.setId(id);
        product.setName(name);
        return product;
    }

}

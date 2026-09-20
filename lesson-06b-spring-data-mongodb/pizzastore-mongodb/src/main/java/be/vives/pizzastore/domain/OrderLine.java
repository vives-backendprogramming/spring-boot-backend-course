package be.vives.pizzastore.domain;

import java.math.BigDecimal;

// Embedded value object: no @Document, no own id. In the JPA version OrderLine was its own
// table with @ManyToOne references to Order and Pizza; here it lives inside the owning
// Order document (the natural aggregate boundary) and only references Pizza by id, since
// the actual Pizza object is not reachable through a Mongo document the way a JPA
// association would be.
public class OrderLine {

    private String pizzaId;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal subtotal;

    // Constructors
    public OrderLine() {
    }

    public OrderLine(String pizzaId, BigDecimal unitPrice, Integer quantity) {
        this.pizzaId = pizzaId;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    // Getters and Setters
    public String getPizzaId() {
        return pizzaId;
    }

    public void setPizzaId(String pizzaId) {
        this.pizzaId = pizzaId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
        calculateSubtotal();
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        calculateSubtotal();
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    private void calculateSubtotal() {
        if (unitPrice != null && quantity != null) {
            this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    @Override
    public String toString() {
        return "OrderLine{" +
                "pizzaId='" + pizzaId + '\'' +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", subtotal=" + subtotal +
                '}';
    }
}

package be.vives.pizzastore.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "pizzas")
public class Pizza {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(length = 1000)
    private String description;

    // @OneToOne: Pizza has one NutritionalInfo
    @OneToOne(mappedBy = "pizza", cascade = CascadeType.ALL, orphanRemoval = true)
    private NutritionalInfo nutritionalInfo;

    // @ManyToMany: Pizza can be favorited by many Customers
    @ManyToMany(mappedBy = "favoritePizzas")
    private Set<Customer> favoritedByCustomers = new HashSet<>();

    // Constructors
    public Pizza() {
    }

    public Pizza(String name, BigDecimal price, String description) {
        this.name = name;
        this.price = price;
        this.description = description;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public NutritionalInfo getNutritionalInfo() {
        return nutritionalInfo;
    }

    public void setNutritionalInfo(NutritionalInfo nutritionalInfo) {
        this.nutritionalInfo = nutritionalInfo;
    }

    public Set<Customer> getFavoritedByCustomers() {
        return favoritedByCustomers;
    }

    public void setFavoritedByCustomers(Set<Customer> favoritedByCustomers) {
        this.favoritedByCustomers = favoritedByCustomers;
    }

    @Override
    public String toString() {
        return "Pizza{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", price=" + price +
                '}';
    }
}

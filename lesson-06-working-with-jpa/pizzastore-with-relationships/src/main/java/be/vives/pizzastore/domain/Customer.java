package be.vives.pizzastore.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(length = 200)
    private String address;

    // @OneToMany: Customer has many Orders
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Order> orders = new ArrayList<>();

    // @ManyToMany: Customer can favorite many Pizzas
    @ManyToMany
    @JoinTable(
            name = "customer_favorite_pizzas",
            joinColumns = @JoinColumn(name = "customer_id"),
            inverseJoinColumns = @JoinColumn(name = "pizza_id")
    )
    private Set<Pizza> favoritePizzas = new HashSet<>();

    // Constructors
    public Customer() {
    }

    public Customer(String name, String email) {
        this.name = name;
        this.email = email;
    }

    // Helper methods for bidirectional relationships
    public void addOrder(Order order) {
        orders.add(order);
        order.setCustomer(this);
    }

    public void removeOrder(Order order) {
        orders.remove(order);
        order.setCustomer(null);
    }

    public void addFavoritePizza(Pizza pizza) {
        favoritePizzas.add(pizza);
        pizza.getFavoritedByCustomers().add(this);
    }

    public void removeFavoritePizza(Pizza pizza) {
        favoritePizzas.remove(pizza);
        pizza.getFavoritedByCustomers().remove(this);
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public List<Order> getOrders() {
        return orders;
    }

    public void setOrders(List<Order> orders) {
        this.orders = orders;
    }

    public Set<Pizza> getFavoritePizzas() {
        return favoritePizzas;
    }

    public void setFavoritePizzas(Set<Pizza> favoritePizzas) {
        this.favoritePizzas = favoritePizzas;
    }

    @Override
    public String toString() {
        return "Customer{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}

package be.vives.pizzastore.domain;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "customers")
public class Customer {

    @Id
    private String id;

    private String name;

    @Indexed(unique = true)
    private String email;

    private String password;

    private String phone;

    private String address;

    private Role role = Role.CUSTOMER;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // @ManyToMany in the JPA version, backed by a customer_favorite_pizzas join table.
    // MongoDB has no join tables, so the many-to-many becomes a plain list of referenced
    // Pizza ids, stored only on this side (see the lesson README for why).
    private List<String> favoritePizzaIds = new ArrayList<>();

    // Orders are NOT embedded or referenced here (unlike a JPA @OneToMany): an order list
    // can grow without bound, and Order already stores its own customerId. Look them up
    // with OrderRepository.findByCustomerId(customer.getId()) instead.

    // Constructors
    public Customer() {
    }

    public Customer(String name, String email) {
        this.name = name;
        this.email = email;
    }

    // Helper methods
    public void addFavoritePizza(String pizzaId) {
        favoritePizzaIds.add(pizzaId);
    }

    public void removeFavoritePizza(String pizzaId) {
        favoritePizzaIds.remove(pizzaId);
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
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

    public List<String> getFavoritePizzaIds() {
        return favoritePizzaIds;
    }

    public void setFavoritePizzaIds(List<String> favoritePizzaIds) {
        this.favoritePizzaIds = favoritePizzaIds;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "Customer{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                '}';
    }
}

package be.vives.pizzastore.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "nutritional_info")
public class NutritionalInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer calories;

    private java.math.BigDecimal protein;

    private java.math.BigDecimal carbohydrates;

    private java.math.BigDecimal fat;

    // @OneToOne: NutritionalInfo belongs to one Pizza
    @OneToOne
    @JoinColumn(name = "pizza_id", nullable = false)
    private Pizza pizza;

    // Constructors
    public NutritionalInfo() {
    }

    public NutritionalInfo(Integer calories, java.math.BigDecimal protein, java.math.BigDecimal carbohydrates, java.math.BigDecimal fat) {
        this.calories = calories;
        this.protein = protein;
        this.carbohydrates = carbohydrates;
        this.fat = fat;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getCalories() {
        return calories;
    }

    public void setCalories(Integer calories) {
        this.calories = calories;
    }

    public java.math.BigDecimal getProtein() {
        return protein;
    }

    public void setProtein(java.math.BigDecimal protein) {
        this.protein = protein;
    }

    public java.math.BigDecimal getCarbohydrates() {
        return carbohydrates;
    }

    public void setCarbohydrates(java.math.BigDecimal carbohydrates) {
        this.carbohydrates = carbohydrates;
    }

    public java.math.BigDecimal getFat() {
        return fat;
    }

    public void setFat(java.math.BigDecimal fat) {
        this.fat = fat;
    }

    public Pizza getPizza() {
        return pizza;
    }

    public void setPizza(Pizza pizza) {
        this.pizza = pizza;
    }

    @Override
    public String toString() {
        return "NutritionalInfo{" +
                "id=" + id +
                ", calories=" + calories +
                ", protein=" + protein +
                ", carbohydrates=" + carbohydrates +
                ", fat=" + fat +
                '}';
    }
}

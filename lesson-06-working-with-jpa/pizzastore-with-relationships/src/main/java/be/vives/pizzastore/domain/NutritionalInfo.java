package be.vives.pizzastore.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "nutritional_info")
public class NutritionalInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer calories;

    @Column(nullable = false)
    private Integer protein;

    @Column(nullable = false)
    private Integer carbohydrates;

    @Column(nullable = false)
    private Integer fat;

    // @OneToOne: NutritionalInfo belongs to one Pizza
    @OneToOne
    @JoinColumn(name = "pizza_id", nullable = false)
    private Pizza pizza;

    // Constructors
    public NutritionalInfo() {
    }

    public NutritionalInfo(Integer calories, Integer protein, Integer carbohydrates, Integer fat) {
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

    public Integer getProtein() {
        return protein;
    }

    public void setProtein(Integer protein) {
        this.protein = protein;
    }

    public Integer getCarbohydrates() {
        return carbohydrates;
    }

    public void setCarbohydrates(Integer carbohydrates) {
        this.carbohydrates = carbohydrates;
    }

    public Integer getFat() {
        return fat;
    }

    public void setFat(Integer fat) {
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

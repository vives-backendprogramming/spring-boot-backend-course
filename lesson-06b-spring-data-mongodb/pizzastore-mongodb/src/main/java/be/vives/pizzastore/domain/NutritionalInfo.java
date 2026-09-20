package be.vives.pizzastore.domain;

import java.math.BigDecimal;

// Embedded value object: no @Document, no @Id. In the JPA version this was its own table
// linked by a @OneToOne foreign key; in MongoDB it is nested directly inside the Pizza
// document, since it never needs to be queried or saved independently of its pizza.
public class NutritionalInfo {

    private Integer calories;

    private BigDecimal protein;

    private BigDecimal carbohydrates;

    private BigDecimal fat;

    // Constructors
    public NutritionalInfo() {
    }

    public NutritionalInfo(Integer calories, BigDecimal protein, BigDecimal carbohydrates, BigDecimal fat) {
        this.calories = calories;
        this.protein = protein;
        this.carbohydrates = carbohydrates;
        this.fat = fat;
    }

    // Getters and Setters
    public Integer getCalories() {
        return calories;
    }

    public void setCalories(Integer calories) {
        this.calories = calories;
    }

    public BigDecimal getProtein() {
        return protein;
    }

    public void setProtein(BigDecimal protein) {
        this.protein = protein;
    }

    public BigDecimal getCarbohydrates() {
        return carbohydrates;
    }

    public void setCarbohydrates(BigDecimal carbohydrates) {
        this.carbohydrates = carbohydrates;
    }

    public BigDecimal getFat() {
        return fat;
    }

    public void setFat(BigDecimal fat) {
        this.fat = fat;
    }

    @Override
    public String toString() {
        return "NutritionalInfo{" +
                "calories=" + calories +
                ", protein=" + protein +
                ", carbohydrates=" + carbohydrates +
                ", fat=" + fat +
                '}';
    }
}

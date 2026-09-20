package be.vives.pizzastore.config;

import be.vives.pizzastore.domain.Customer;
import be.vives.pizzastore.domain.NutritionalInfo;
import be.vives.pizzastore.domain.Order;
import be.vives.pizzastore.domain.OrderLine;
import be.vives.pizzastore.domain.OrderStatus;
import be.vives.pizzastore.domain.Pizza;
import be.vives.pizzastore.domain.Role;
import be.vives.pizzastore.repository.CustomerRepository;
import be.vives.pizzastore.repository.OrderRepository;
import be.vives.pizzastore.repository.PizzaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

// MongoDB has no data.sql equivalent - that is a relational, SQL-initialization mechanism,
// and Spring Data MongoDB does not run against a fixed schema. The idiomatic way to seed
// dummy data is a CommandLineRunner like this one, guarded so it only inserts once.
@Component
public class DataSeeder implements CommandLineRunner {

    private final PizzaRepository pizzaRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;

    public DataSeeder(PizzaRepository pizzaRepository, CustomerRepository customerRepository, OrderRepository orderRepository) {
        this.pizzaRepository = pizzaRepository;
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public void run(String... args) {
        if (pizzaRepository.count() > 0) {
            return;
        }

        Pizza margherita = new Pizza("Margherita", new BigDecimal("8.99"), "Classic tomato sauce, fresh mozzarella, basil, and extra virgin olive oil");
        margherita.setImageUrl("https://images.unsplash.com/photo-1574071318508-1cdbab80d002");
        margherita.setNutritionalInfo(new NutritionalInfo(266, new BigDecimal("11.0"), new BigDecimal("33.0"), new BigDecimal("10.0")));

        Pizza pepperoni = new Pizza("Pepperoni", new BigDecimal("10.99"), "Tomato sauce, mozzarella, and spicy pepperoni slices");
        pepperoni.setImageUrl("https://images.unsplash.com/photo-1628840042765-356cda07504e");
        pepperoni.setNutritionalInfo(new NutritionalInfo(298, new BigDecimal("13.5"), new BigDecimal("36.0"), new BigDecimal("12.5")));

        Pizza quattroFormaggi = new Pizza("Quattro Formaggi", new BigDecimal("11.99"), "Four cheese blend: mozzarella, gorgonzola, parmesan, and fontina");
        quattroFormaggi.setImageUrl("https://images.unsplash.com/photo-1571997478779-2adcbbe9ab2f");
        quattroFormaggi.setNutritionalInfo(new NutritionalInfo(320, new BigDecimal("15.0"), new BigDecimal("35.0"), new BigDecimal("14.0")));

        Pizza vegetariana = new Pizza("Vegetariana", new BigDecimal("9.99"), "Fresh vegetables: bell peppers, mushrooms, onions, tomatoes, and olives");
        vegetariana.setImageUrl("https://images.unsplash.com/photo-1627626775846-122c3f8e3e3b");
        vegetariana.setNutritionalInfo(new NutritionalInfo(245, new BigDecimal("9.0"), new BigDecimal("38.0"), new BigDecimal("8.5")));

        Pizza diavola = new Pizza("Diavola", new BigDecimal("12.99"), "Spicy salami, hot peppers, tomato sauce, and mozzarella");
        diavola.setImageUrl("https://images.unsplash.com/photo-1593560708920-61dd98c46a4e");
        diavola.setNutritionalInfo(new NutritionalInfo(310, new BigDecimal("14.0"), new BigDecimal("36.0"), new BigDecimal("13.0")));

        Pizza hawaii = new Pizza("Hawaii", new BigDecimal("11.49"), "Ham, pineapple, tomato sauce, and mozzarella");
        hawaii.setImageUrl("https://images.unsplash.com/photo-1565299624946-b28f40a0ae38");
        hawaii.setNutritionalInfo(new NutritionalInfo(275, new BigDecimal("12.0"), new BigDecimal("39.0"), new BigDecimal("9.5")));

        pizzaRepository.saveAll(List.of(margherita, pepperoni, quattroFormaggi, vegetariana, diavola, hawaii));

        // BCrypt hash for "password123" - same seed value as the final pizzastore project
        String passwordHash = "$2a$10$wvw30spxLOR1gV/NYh86ruw8J1rPa8MvkZwG0ru7VuRECMfARo0ri";

        Customer emma = new Customer("Emma Johnson", "emma.johnson@example.com");
        emma.setPassword(passwordHash);
        emma.setPhone("+32 470 12 34 56");
        emma.setAddress("Rue de la Loi 123, 1000 Brussels");
        emma.addFavoritePizza(margherita.getId());
        emma.addFavoritePizza(quattroFormaggi.getId());
        emma.addFavoritePizza(diavola.getId());

        Customer liam = new Customer("Liam Smith", "liam.smith@example.com");
        liam.setPassword(passwordHash);
        liam.setPhone("+32 471 23 45 67");
        liam.setAddress("Meir 45, 2000 Antwerp");
        liam.addFavoritePizza(pepperoni.getId());
        liam.addFavoritePizza(diavola.getId());

        Customer admin = new Customer("Admin User", "admin@pizzastore.be");
        admin.setPassword(passwordHash);
        admin.setPhone("+32 475 67 89 01");
        admin.setAddress("Headquarters, 1000 Brussels");
        admin.setRole(Role.ADMIN);

        customerRepository.saveAll(List.of(emma, liam, admin));

        Order order1 = new Order("ORD-20240115-00001", emma.getId(), OrderStatus.DELIVERED);
        order1.addOrderLine(new OrderLine(margherita.getId(), margherita.getPrice(), 2));
        order1.addOrderLine(new OrderLine(quattroFormaggi.getId(), quattroFormaggi.getPrice(), 1));

        Order order2 = new Order("ORD-20240115-00002", liam.getId(), OrderStatus.DELIVERED);
        order2.addOrderLine(new OrderLine(pepperoni.getId(), pepperoni.getPrice(), 1));
        order2.addOrderLine(new OrderLine(diavola.getId(), diavola.getPrice(), 1));

        Order order3 = new Order("ORD-20240118-00003", emma.getId(), OrderStatus.CONFIRMED);
        order3.addOrderLine(new OrderLine(vegetariana.getId(), vegetariana.getPrice(), 1));
        order3.addOrderLine(new OrderLine(hawaii.getId(), hawaii.getPrice(), 1));

        orderRepository.saveAll(List.of(order1, order2, order3));
    }
}

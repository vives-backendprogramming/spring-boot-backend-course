# Lesson 3: Properties & Profiles in Spring

**Make the same code run in every environment: external configuration, profiles and logging**

---

## 📋 Learning Objectives

By the end of this lesson, you will be able to:

- Explain why configuration should be externalized
- Use **`@Value`** to inject property values, with and without defaults
- Use **`@PropertySource`** to load a properties file in plain Spring
- Explain which property sources exist and which one wins
- Create **environment-specific configuration** with profiles and **`@Profile`**
- Activate profiles in different ways
- Log with **SLF4J + Logback** at the right level

---

## 📚 Table of Contents

1. [Recap: Where Lesson 2 Left Us](#-recap-where-lesson-2-left-us)
2. [Why Externalize Configuration?](#-why-externalize-configuration)
3. [@Value Annotation](#-value-annotation)
4. [Where Properties Come From (Property Sources & Priority)](#-where-properties-come-from-property-sources--priority)
5. [Property Files](#-property-files)
6. [YAML Files](#-yaml-files)
7. [@PropertySource](#-propertysource)
8. [Spring Profiles](#-spring-profiles)
9. [What are Spring Profiles?](#-what-are-spring-profiles)
10. [@Profile Annotation](#️-profile-annotation)
11. [Profile-Specific Property Files](#-profile-specific-property-files)
12. [Activating Profiles](#-activating-profiles)
13. [Logging in Spring Applications](#-logging-in-spring-applications)
14. [Log Levels](#-log-levels)
15. [Using Logging in Code](#-using-logging-in-code)
16. [Configuring Logging](#️-configuring-logging)
17. [Logging Best Practices](#-logging-best-practices)
18. [Best Practices](#-best-practices)
19. [Summary](#-summary)
20. [Additional Resources](#-additional-resources)

---

## 🔄 Recap: Where Lesson 2 Left Us

In Lesson 2, the Spring container took over creating and wiring our objects. But the beans themselves still contain **hardcoded values**: a database URL, a username, a password — written straight into Java code.

In real-world applications, configuration differs between environments (development, testing, production). Hardcoding these values makes applications inflexible and difficult to maintain.

Spring provides mechanisms to **externalize configuration** through **properties files** and to manage different environments using **profiles**. Additionally, proper **logging** is essential for monitoring and debugging applications. Like Lesson 2, this lesson uses plain Spring; where Spring Boot (Lesson 4) behaves differently, the text says so.

---

## 🔧 Why Externalize Configuration?

### The Problem: Hardcoded Values

```java
@Configuration
public class DataSourceConfig {
    
    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("oracle.jdbc.driver.OracleDriver");
        ds.setUrl("jdbc:oracle:thin:@localhost:1521:XE");
        ds.setUsername("username");
        ds.setPassword("password");
        return ds;
    }
}
```

**Problems**:

- ❌ Cannot change without recompiling
- ❌ Different values for dev/test/prod
- ❌ Credentials in source code (security risk!)
- ❌ Hard to test with different configurations

### The Solution: Externalized Configuration

> **"Spring lets you externalize your configuration so that you can work with the same application code in different environments."**

**You can use**:

- Properties files
- YAML files  — not used in this course, but good to recognize
- Environment variables
- Command-line arguments

**Property values can be injected directly into your beans using the `@Value` annotation.**

---

## 💉 @Value Annotation

The `@Value` annotation injects property values into your Spring beans.

### Step 1: Extract to Variables

```java
@Configuration
public class DataSourceConfig {
    
    private String driver = "oracle.jdbc.driver.OracleDriver";
    private String jdbcUrl = "jdbc:oracle:thin:@localhost:1521:XE";
    private String username = "username";
    private String password = "password";
    
    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName(driver);
        ds.setUrl(jdbcUrl);
        ds.setUsername(username);
        ds.setPassword(password);
        return ds;
    }
}
```

Still hardcoded, but better organized.

### Step 2: Use @Value with Property Placeholders

```java
@Configuration
public class DataSourceConfig {
    
    @Value("${vives.jdbc.oracle.driver}")
    private String driver;
    
    @Value("${vives.jdbc.oracle.url}")
    private String jdbcUrl;
    
    @Value("${vives.jdbc.oracle.username}")
    private String username;
    
    @Value("${vives.jdbc.oracle.password}")
    private String password;
    
    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName(driver);
        ds.setUrl(jdbcUrl);
        ds.setUsername(username);
        ds.setPassword(password);
        return ds;
    }
}
```

**Syntax**: `@Value("${property.name}")`

### @Value with Default Values

```java
@Value("${server.port:8080}")
private int serverPort;  // Uses 8080 if property not found

@Value("${app.name:MyApp}")
private String appName;  // Uses "MyApp" if property not found
```

**Syntax**: `@Value("${property.name:defaultValue}")`

### @Value on Constructor Parameters

The examples above use field injection to keep them short. Lesson 2 recommended constructor injection, and `@Value` works there too:

```java
@Service
public class PizzaStoreInfoService {

    private final String storeName;

    public PizzaStoreInfoService(@Value("${pizzastore.name:PizzaStore}") String storeName) {
        this.storeName = storeName;
    }
}
```

The field can now be `final`, and a unit test can pass any value straight into the constructor.

---

## 📍 Where Properties Come From (Property Sources & Priority)

`@Value("${...}")` doesn't read a single file — it reads from the **Spring `Environment`**. The `Environment` pulls properties from several different **property sources** (JVM system properties, OS environment variables, property files, ...) and exposes them to your beans as one unified set of values, so a bean never has to know *where* a property actually came from.

**In plain Spring** (this lesson), a fresh `AnnotationConfigApplicationContext` knows only three kinds of source, in this order of priority:

1. **JVM system properties** (e.g. `-Dpizzastore.name=Mario`)
2. **OS environment variables**
3. **Files you register yourself with `@PropertySource`**

**In Spring Boot** (from Lesson 4), the list grows, and Boot loads `application.properties` for you:

1. **Command-line arguments** (e.g. `--server.port=8080`)
2. **JVM system properties** (e.g. `-Dserver.port=8080`)
3. **OS environment variables**
4. **`application.properties` file outside the application** (e.g. next to the `.jar`)
5. **`application.properties` file inside the application** (on the classpath, `src/main/resources`)
6. **Property sources supplied via `@PropertySource`/`@PropertySources`**

Both lists are in **order of priority**: a property source higher in the list overrides the same property defined in a source lower in the list. So if you set a property both in a properties file *and* as a JVM system property (`-D...`), the JVM system property wins because it's higher up.

This is exactly why the [Configuration Hierarchy](#configuration-hierarchy) at the end of this lesson puts command-line arguments and environment variables above property files.

---

## 📄 Property Files

### Creating application.properties

Create a file named **`application.properties`** in **`src/main/resources`**:

```properties
# Database Configuration
vives.jdbc.oracle.driver=oracle.jdbc.driver.OracleDriver
vives.jdbc.oracle.url=jdbc:oracle:thin:@localhost:1521:XE
vives.jdbc.oracle.username=username
vives.jdbc.oracle.password=password
```

**Location**: conventionally on the classpath root (`src/main/resources`) — but unlike Spring Boot later in this course, plain Spring does **not** discover this file automatically. You have to point Spring at it yourself with `@PropertySource` (see below).

### Property File Conventions

```properties
# Use lowercase with dots or hyphens
app.name=PizzaStore
app.version=1.0.0

# Group related properties
pizzastore.name=Mario's Pizza
pizzastore.max-orders=100

# Comments start with #
# Database settings
db.host=localhost
db.port=5432
```

**Best Practices**:

- Use meaningful, hierarchical names
- Group related properties with common prefix
- Document properties with comments
- Use lowercase with dots as separators

---

## 📃 YAML Files

Besides `.properties`, Spring also supports **YAML** (`.yml`) as a property file format. The same data, just written hierarchically instead of as flat `key=value` pairs:

**`application.properties`**:

```properties
server.port=9090
spring.application.name=demoservice
datasource.url=jdbc:mysql://localhost:3601/db
```

**`application.yml`** (equivalent):

```yaml
spring:
  application:
    name: demoservice
server:
  port: 9090
datasource:
  url: jdbc:mysql://localhost:3601/db
```

Both resolve to the exact same properties (`server.port`, `spring.application.name`, `datasource.url`) once loaded into the `Environment` — `@Value("${server.port}")` works identically regardless of which format produced the value.

**⚠️ Does this work in plain Spring?** Not out of the box. Unlike `.properties`, plain Spring **cannot** parse a `.yml` file without an extra dependency: `org.yaml.snakeyaml` must be added to the classpath yourself. **Spring Boot**, starting in [Lesson 4](../lesson-04-spring-boot-intro/README.md), ships this dependency automatically via `spring-boot-starter`, which is why YAML "just works" there with zero extra setup.

This course uses `.properties` throughout, so YAML itself is not covered in depth — but recognize it when you see it, since it's the format Spring Boot projects (including the book) tend to default to.

---

## 🔌 @PropertySource

The `@PropertySource` annotation tells Spring where to find your properties file.

### Add to Configuration Class

```java
@Configuration
@PropertySource("classpath:/application.properties")
public class AppConfig {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }
}
```

**`classpath:`** refers to `src/main/resources/`

**⚠️ Why the `PropertySourcesPlaceholderConfigurer` bean?** Since Spring 4.3, `@Value("${...}")` resolves against the `Environment` even without it — but *leniently*: a property that doesn't exist (a typo, a missing file) is not an error. The field silently receives the literal text `${vives.jdbc.oracle.url}`, and you only find out when the connection fails much later. With this bean registered, an unresolvable placeholder **fails at startup** with `Could not resolve placeholder 'vives.jdbc.oracle.url'`. The bean must be `static` so Spring can create it very early, before the beans that use `@Value`. (Spring Boot registers it for you.)

### Multiple Property Sources

```java
@Configuration
@PropertySource("classpath:/application.properties")
@PropertySource("classpath:/database.properties")
public class AppConfig {
}
```

Or use `@PropertySources`:

```java
@Configuration
@PropertySources({
    @PropertySource("classpath:/application.properties"),
    @PropertySource("classpath:/database.properties")
})
public class AppConfig {
}
```

---

## 🌍 Spring Profiles

### Why Profiles?

Applications are deployed to different environments:

- **Development** - Local machine, H2 database, detailed logging
- **Test** - QA environment, test database, moderate logging
- **Production** - Live environment, production database, minimal logging

**Each environment needs different configuration!**

**Problems without profiles**:

- Managing multiple property files manually
- Not tracked in source control
- Difficult to rollback changes
- Manual switching between configurations
- Error-prone

**Solution**: **Spring Profiles** 🎯

---

## 🎭 What are Spring Profiles?

> **Spring Profiles are a type of conditional configuration where different beans, configuration classes, and configuration properties are applied or ignored based on what profiles are active at runtime.**

**Two main approaches**:

1. **`@Profile`** annotation - Conditional bean loading
2. **Profile-specific property files** - Environment-specific values

---

## 🏷️ @Profile Annotation

Use `@Profile` to conditionally load beans based on the active profile.

### Basic Usage on Configuration Class

```java
@Configuration
@Profile("production")
public class ProductionConfig {
    
    @Bean
    public DataSource dataSource() {
        // Production database configuration
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setUrl("jdbc:postgresql://prod-server:5432/pizzastore");
        ds.setUsername("prod_user");
        ds.setPassword("secure_password");
        return ds;
    }
}
```

This configuration **only loads** when the `production` profile is active.

### On Component Classes

```java
@Service
@Profile("development")
public class MockEmailService {
    
    public void sendEmail(String to, String message) {
        System.out.println("MOCK EMAIL to " + to + ": " + message);
    }
}

@Service
@Profile("production")
public class RealEmailService {
    
    public void sendEmail(String to, String message) {
        // Send actual email via SMTP
    }
}
```

### Multiple Profiles (OR logic)

```java
@Component
@Profile({"production", "qa"})
public class ErrorLogger {
    // Only loaded in production OR qa
}
```

### Negation - NOT Profile

```java
@Service
@Profile("!development")
public class ProductionEmailService {
    // Loaded in ALL profiles EXCEPT development
}
```

### On Bean Methods

```java
@Configuration
public class DataSourceConfig {
    
    @Bean
    @Profile("development")
    public DataSource devDataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.h2.Driver");
        ds.setUrl("jdbc:h2:mem:testdb");
        ds.setUsername("sa");
        ds.setPassword("");
        return ds;
    }
    
    @Bean
    @Profile("production")
    public DataSource prodDataSource(@Value("${DB_PASSWORD}") String password) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setUrl("jdbc:postgresql://prod-server:5432/pizzastore");
        ds.setUsername("prod_user");
        ds.setPassword(password);   // read from the DB_PASSWORD environment variable
        return ds;
    }
}
```

Note that `"${DB_PASSWORD}"` only means something inside `@Value` (or a properties file): passed as a normal Java string to `setPassword(...)`, it would literally become the password.

---

## 📁 Profile-Specific Property Files

### Naming Convention

Profile-specific properties use this naming pattern:

```
application-{profile}.properties
```

**Examples**:

- `application-dev.properties`
- `application-test.properties`
- `application-prod.properties`

### File Structure

```
src/main/resources/
  ├── application.properties          # Common properties (all environments)
  ├── application-dev.properties      # Development only
  ├── application-test.properties     # Testing only
  └── application-prod.properties     # Production only
```

### How It Works (in Spring Boot — coming in Lesson 4)

Once we introduce Spring Boot in [Lesson 4](../lesson-04-spring-boot-intro/README.md), this naming convention becomes fully automatic:

1. Spring Boot always loads `application.properties` first
2. If a profile is active, Spring Boot also loads `application-{profile}.properties`
3. Profile-specific properties **override** common properties
4. You can have multiple profiles active simultaneously

### Wiring It Up Yourself (plain Spring, no Boot)

Plain Spring Framework does **not** know this naming convention — nothing loads `application-{profile}.properties` for you automatically. You wire it up yourself by combining `@Profile` with `@PropertySource`, one `@Configuration` class per environment:

```java
@Configuration
@Profile("dev")
@PropertySource("classpath:/application-dev.properties")
public class DevConfig {
}

@Configuration
@Profile("prod")
@PropertySource("classpath:/application-prod.properties")
public class ProdConfig {
}
```

Only the `@Configuration` class matching the active profile gets processed, so only its `@PropertySource` gets loaded. This is exactly the pattern Spring Boot automates for you later: **conditional configuration based on a property**. The same underlying idea powers Boot's auto-configuration classes — there it's `@ConditionalOnProperty` / `@ConditionalOnClass` instead of `@Profile`, but the mechanism (a `@Configuration` class that only activates under certain conditions) is identical.

**What actually retires in Spring Boot — and what doesn't.** It's tempting to read the above as "this is all obsolete once Boot arrives." Be precise about *what* gets automated:

- ❌ **Obsolete in Boot**: this exact ritual — a `PropertySourcesPlaceholderConfigurer` bean, plus one hand-written `@Configuration` class per environment whose only job is to glue a profile name to a properties filename. You will essentially never write this again once you're on Spring Boot; `application-{profile}.properties` just loads itself.
- ✅ **Still very real in Boot**: `@Profile` itself. You'll keep using it directly on `@Component`/`@Bean` for things that have nothing to do with property files — e.g. `@Profile("test")` on a mock/stub bean, `@Profile("!prod")` on a dev-only controller. Boot automates the *file-loading* convention, not the annotation.
- ✅ **Still very real in Boot**: `@PropertySource` itself. Boot only auto-loads `application.properties` / `application-{profile}.properties` by convention — the moment you need a *non-standard* properties file (a legacy config file, a third-party library's properties, something outside the `application*` naming), you reach for `@PropertySource` on a `@Configuration` class exactly as you did here.

So what you're learning here isn't dead knowledge that Boot throws away — it's the general-purpose mechanism (`Environment`, profiles, property sources) that Boot *also* runs on. Boot just removes one specific, repetitive wiring chore built on top of it.

---

## 🚀 Activating Profiles

> **Note**: the runnable `.jar` shown below (`java -jar your-app.jar`) is a Spring Boot packaging feature you'll get in [Lesson 4](../lesson-04-spring-boot-intro/README.md). For now, in plain Spring, apply the same `-D`/`--` flags to however you currently launch your `main()` class — directly with `java`, or via your IDE's Run Configuration.

### ❌ Option 1: In Property File (Not Recommended)

**application.properties**:

```properties
spring.profiles.active=production
```

**Why avoid this?**

- Defeats the purpose of externalized configuration
- Would need to change file for each environment
- Configuration should be external to the application
- **Only use during development for convenience!**

### ✅ Option 2: Outside the Application

These are the **recommended** ways to activate profiles:

#### 1. JVM System Property

```bash
java -Dspring.profiles.active=dev -jar your-app.jar
```

**Watch the order**: `-D` system properties must come *before* `-jar` on the command line — anything after `-jar your-app.jar` is passed to your application as a plain argument, not read by the JVM as a system property.

#### 2. Environment Variable

**Linux/Mac**:

```bash
export SPRING_PROFILES_ACTIVE=dev
java -jar your-app.jar
```

**Windows**:

```cmd
set SPRING_PROFILES_ACTIVE=dev
java -jar your-app.jar
```

Environment variables can't contain dots, so Spring maps `SPRING_PROFILES_ACTIVE` to `spring.profiles.active` (upper case and underscores is the convention).

#### 3. Command Line Argument

```bash
java -jar your-app.jar --spring.profiles.active=prod
```

**Plain Spring caveat**: this `--flag=value` parsing (backed by Spring Framework's `SimpleCommandLinePropertySource`) doesn't happen automatically in a plain `AnnotationConfigApplicationContext` — you'd register it yourself in `main()`. Spring Boot's `SpringApplication.run(args)` wires this in for you, which is why it "just works" there.

#### 4. In IDE (IntelliJ IDEA)

1. **Run → Edit Configurations**
2. **VM options**: `-Dspring.profiles.active=dev`
3. Or **Program arguments**: `--spring.profiles.active=dev`

### Multiple Active Profiles

You can activate multiple profiles:

```bash
java -jar your-app.jar --spring.profiles.active=prod,monitoring,ssl
```

Properties are loaded in order, with later profiles overriding earlier ones.

---

## 📊 Logging in Spring Applications

### Why Logging?

Logging is essential for:

- **Debugging** - Understanding what's happening in your code
- **Monitoring** - Tracking application behavior in production
- **Troubleshooting** - Diagnosing issues when they occur
- **Auditing** - Recording important business operations
- **Performance tracking** - Measuring execution times

### Logging in Spring

Spring Framework uses **Commons Logging** internally but leaves the concrete logging implementation open; in practice, almost every project pairs it with **SLF4J** (Simple Logging Facade for Java) and **Logback**.

**In plain Spring, you add these yourself**: `slf4j-api` and `logback-classic` as Maven dependencies. (Once we move to Spring Boot in [Lesson 4](../lesson-04-spring-boot-intro/README.md), `spring-boot-starter` brings both in automatically — one less thing to configure.)

---

## 📝 Log Levels

Logging frameworks provide different severity levels:

| Level | Purpose | Example |
|-------|---------|---------|
| **TRACE** | Very detailed debugging | Method entry/exit, variable values |
| **DEBUG** | Detailed information for debugging | SQL queries, request details |
| **INFO** | General informational messages | Application started, user logged in |
| **WARN** | Warning messages | Deprecated API usage, recoverable errors |
| **ERROR** | Error messages | Exceptions, failures |

**Production**: Use WARN or INFO  
**Development**: Use DEBUG or TRACE

---

## 🔧 Using Logging in Code

### Basic Setup

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PizzaService {
    
    // Create logger for this class
    private static final Logger log = LoggerFactory.getLogger(PizzaService.class);
    
    private final PizzaRepository pizzaRepository;
    
    public PizzaService(PizzaRepository pizzaRepository) {
        this.pizzaRepository = pizzaRepository;
    }
    
    public List<Pizza> getAllPizzas() {
        log.debug("Fetching all pizzas from repository");
        List<Pizza> pizzas = pizzaRepository.findAll();
        log.info("Found {} pizzas", pizzas.size());
        return pizzas;
    }
}
```

### Different Log Levels

```java
@Service
public class PizzaService {
    
    private static final Logger log = LoggerFactory.getLogger(PizzaService.class);
    
    public Pizza getPizzaById(Long id) {
        log.trace("Entering getPizzaById with id: {}", id);
        log.debug("Fetching pizza with id: {}", id);
        
        Pizza pizza = pizzaRepository.findById(id)
            .orElseThrow(() -> {
                log.error("Pizza not found with id: {}", id);
                return new RuntimeException("Pizza not found");
            });
        
        log.info("Successfully retrieved pizza: {}", pizza.getName());
        log.trace("Exiting getPizzaById");
        return pizza;
    }
    
    public Pizza createPizza(Pizza pizza) {
        log.info("Creating new pizza: {}", pizza.getName());
        
        if (pizza.getPrice() == null) {
            log.warn("Pizza price is null, using default value");
            pizza.setPrice(new BigDecimal("10.00"));
        }
        
        try {
            Pizza saved = pizzaRepository.save(pizza);
            log.info("Pizza created successfully with id: {}", saved.getId());
            return saved;
        } catch (Exception e) {
            log.error("Failed to create pizza: {}", pizza.getName(), e);
            throw e;
        }
    }
}
```

### Parameterized Logging

✅ **Good - Use placeholders**:

```java
log.info("User {} ordered {} pizzas", username, count);
```

❌ **Bad - String concatenation**:

```java
log.info("User " + username + " ordered " + count + " pizzas");  // Slow!
```

**Why?** Placeholders only build the string if the log level is enabled. String concatenation always happens.

### Logging Exceptions

Always include the exception object:

```java
try {
    processOrder(order);
} catch (Exception e) {
    log.error("Failed to process order {}", order.getId(), e);  // ← Include 'e'
    throw e;
}
```

This logs the full stack trace!

---

## ⚙️ Configuring Logging

### In Plain Spring: logback.xml

In plain Spring, log levels are configured in Logback's own file, `src/main/resources/logback.xml`. Without that file, Logback logs everything at `DEBUG` to the console:

```xml
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <logger name="com.example.pizzastore" level="DEBUG"/>
    <logger name="org.springframework" level="WARN"/>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

### In application.properties (Spring Boot)

Spring Boot (Lesson 4) lets you set the same levels as ordinary properties. These `logging.level.*` keys do **nothing** in plain Spring — only Boot reads them:

```properties
# Root level (applies to all packages)
logging.level.root=INFO

# Your application package
logging.level.com.example.pizzastore=DEBUG

# Specific package
logging.level.com.example.pizzastore.service=TRACE
logging.level.com.example.pizzastore.repository=DEBUG

# Third-party libraries
logging.level.org.springframework=WARN
logging.level.org.hibernate=INFO
logging.level.org.hibernate.SQL=DEBUG
```

### Environment-Specific Logging (Spring Boot)

**application-dev.properties**:

```properties
# Detailed logging for development
logging.level.root=INFO
logging.level.com.example.pizzastore=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.org.hibernate.SQL=DEBUG
```

**application-test.properties**:

```properties
# Moderate logging for testing
logging.level.root=INFO
logging.level.com.example.pizzastore=INFO
```

**application-prod.properties**:

```properties
# Minimal logging for production
logging.level.root=WARN
logging.level.com.example.pizzastore=INFO
logging.level.org.springframework=ERROR
```

---

## 🎯 Logging Best Practices

### ✅ DO

1. **Use appropriate log levels**

   ```java
   log.trace("Method entry/exit");
   log.debug("Detailed debugging info");
   log.info("Important business events");
   log.warn("Potential problems");
   log.error("Actual errors");
   ```

2. **Use parameterized messages**

   ```java
   log.info("User {} logged in at {}", username, timestamp);
   ```

3. **Log important business operations**

   ```java
   log.info("Order {} placed by user {}", orderId, userId);
   ```

4. **Always include exception in error logs**

   ```java
   log.error("Failed to process payment", exception);
   ```

5. **Use meaningful messages**

   ```java
   log.info("Pizza {} added to cart by user {}", pizzaId, userId);
   ```

### ❌ DON'T

1. **Don't log sensitive information**

   ```java
   log.info("Password: {}", password);  // ❌ Never!
   log.info("Credit card: {}", card);   // ❌ Never!
   ```

2. **Don't use System.out.println()**

   ```java
   System.out.println("User logged in");  // ❌ Use logging!
   ```

3. **Don't concatenate strings**

   ```java
   log.info("User " + name + " age " + age);  // ❌ Slow!
   log.info("User {} age {}", name, age);     // ✅ Fast!
   ```

4. **Don't log too much in production**
   - Set appropriate log levels (WARN or INFO)
   - Avoid TRACE/DEBUG in production

5. **Don't swallow exceptions silently**

   ```java
   try {
       doSomething();
   } catch (Exception e) {
       // ❌ Silent failure - no one knows what happened!
   }
   
   try {
       doSomething();
   } catch (Exception e) {
       log.error("Failed to do something", e);  // ✅ Logged!
       throw e;
   }
   ```

---

## 💡 Best Practices

✅ **DO**:

- Externalize all environment-specific configuration
- Use profiles for different environments (dev, test, prod)
- Register a `PropertySourcesPlaceholderConfigurer` in plain Spring, so a missing property fails at startup
- Prefer `@Value` on constructor parameters over fields
- Use environment variables for sensitive data in production
- Use meaningful, hierarchical property names
- Log at appropriate levels, with parameterized messages

❌ **DON'T**:

- Hardcode configuration values
- Commit passwords to source control or put them in property files
- Activate profiles in `application.properties`
- Log sensitive information
- Use `System.out.println()` instead of logging
- Over-log in production

---

## 🎓 Summary

### What We Learned

1. **Externalized Configuration**
   - Keep configuration out of code
   - Use property files for flexibility
   - Different configs for different environments

2. **@Value Annotation**
   - Inject property values: `@Value("${property.name}")`
   - Support default values: `@Value("${property.name:default}")`

3. **@PropertySource**
   - Load property files: `@PropertySource("classpath:/application.properties")`
   - Add a static `PropertySourcesPlaceholderConfigurer` bean for strict placeholder resolution

4. **Spring Profiles**
   - Environment-specific configuration
   - Conditional bean loading with `@Profile`
   - Profile-specific property files: `application-{profile}.properties`
   - Multiple ways to activate: JVM args, env variables, command line

5. **Logging with SLF4J**
   - Five log levels: TRACE, DEBUG, INFO, WARN, ERROR
   - Logger per class: `LoggerFactory.getLogger(MyClass.class)`
   - Parameterized messages for performance
   - Levels in `logback.xml` (plain Spring) or `logging.level.*` (Spring Boot)

### Configuration Hierarchy

The full Spring Boot property-source priority order, as covered in [Where Properties Come From](#-where-properties-come-from-property-sources--priority) (plain Spring has only the JVM system properties, environment variables and `@PropertySource` rows):

```
Command-Line Arguments               ← Highest Priority
JVM System Properties (-D...)
OS Environment Variables
application.properties (outside the app)
application.properties (on the classpath)
@PropertySource-supplied files       ← Lowest Priority
```

Within that "property files" tier, once profiles enter the picture (Spring Boot, [Lesson 4](../lesson-04-spring-boot-intro/README.md)): `application-{profile}.properties` overrides `application.properties` for any key they both define.

### Key Takeaways

- The **same build** should run in every environment; only its configuration changes
- Spring reads every property through one **`Environment`**, so a bean never cares where a value came from
- **Profiles** switch both values (property files) and implementations (`@Profile` beans)
- Good **logging** is configuration too: the level is set per environment, not in code

### What's Next?

Everything in Lessons 2 and 3 needed hand-written wiring: a configuration class, `@ComponentScan`, `@PropertySource`, a placeholder configurer, Logback setup. Lesson 4 introduces **Spring Boot**, which does all of that by convention, and builds the first PizzaStore.

---

## 📖 Additional Resources

- 🧪 **[Hands-On Exercise](EXERCISE.md)** — externalize config and swap implementations with `@Profile` on `SchoolAdministration`
- [Spring Environment Abstraction](https://docs.spring.io/spring-framework/reference/core/beans/environment.html)
- [Spring Profiles Documentation](https://docs.spring.io/spring-framework/reference/core/beans/environment.html#beans-definition-profiles)
- [Using @Value](https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/value-annotations.html)
- [SLF4J Documentation](https://www.slf4j.org/)
- [Logback Documentation](https://logback.qos.ch/)

**Note on the book**: *Pro Spring Boot 4* skips this "plain Spring" stage entirely and starts from Spring Boot, in Chapter 2: *Spring Boot Internals and Features* — *Externalized Configuration* (p. 38) and *Type-Safe Configuration Properties* with `@ConfigurationProperties` / `@ConfigurationPropertiesScan` (p. 39), *Profiles* with profile-specific properties and beans (p. 42), and *Logging* with log levels and log groups (pp. 43–45). Once you've finished [Lesson 4](../lesson-04-spring-boot-intro/README.md), it's worth reading those sections to see the auto-configured version of everything you just did by hand here. `@ConfigurationProperties` itself is not used in this course; PizzaStore sticks to `@Value`.

---

**Congratulations!** 🎉 You now know how to create flexible, configurable, environment-aware Spring applications with proper logging. Continue to [Lesson 4: Getting Started with Spring Boot](../lesson-04-spring-boot-intro/README.md) to see Spring Boot automate this configuration.

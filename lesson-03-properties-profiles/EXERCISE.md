# Lesson 3 — Exercise: Properties & Profiles on SchoolAdministration

## 📚 Table of Contents

- [📘 Overview](#-overview)
- [🎯 Learning Objectives](#-learning-objectives)
- [🚀 Getting Started](#-getting-started)
- [🔧 Part 1: Externalizing the Datasource Credentials](#-part-1-externalizing-the-datasource-credentials)
- [📝 Part 2: Logging the Injected Properties](#-part-2-logging-the-injected-properties)
- [🌍 Part 3: Environment-Aware Email Sending with Profiles](#-part-3-environment-aware-email-sending-with-profiles)
- [✅ Verification Checklist](#-verification-checklist)
- [📤 Submitting Your Work](#-submitting-your-work)
- [🔍 Reference Solution](#-reference-solution)

---

## 📘 Overview

This exercise continues directly on the **SchoolAdministration** project from the [Lesson 2 exercise](../lesson-02-spring-di-ioc/EXERCISE.md). You'll externalize a hardcoded "database connection" into a properties file, and use **Spring Profiles** to switch between a dev-safe fake email sender and a "production" one — without changing a single line of code between environments.

> **Prerequisite**: finish the [Lesson 2 exercise](../lesson-02-spring-di-ioc/EXERCISE.md) first (or start from the `springcontextaware` branch of the [reference solution](https://github.com/vives-backendprogramming/SchoolAdministration)). This exercise assumes `DummyDataSource`, `SchoolDatabaseStub`, and all services/DAOs are already Spring beans wired through `ApplicationConfiguration`.

## 🎯 Learning Objectives

By the end of this exercise, you will have:
- Externalized configuration values with `@Value` and an `application.properties` file
- Registered a non-default properties file with `@PropertySource`, including the `PropertySourcesPlaceholderConfigurer` bean plain Spring requires
- Replaced `System.out.println` with proper **SLF4J + Logback** logging
- Introduced an interface to allow `@Profile` to swap two implementations of the same contract at runtime
- Seen, first-hand, the exact error Spring throws when two beans of the same type are ambiguous — and how `@Profile` resolves it

## 🚀 Getting Started

Continue in the same project you built in Lesson 2. `DummyDataSource` currently just prints a message and returns `null` from `getConnection()` — you're about to make it read real (fictional) credentials from a properties file.

## 🔧 Part 1: Externalizing the Datasource Credentials

1. Create a `resources` folder under `src/main`. In IntelliJ, right-click it → **Mark Directory as → Resources Root**.
2. Create `src/main/resources/application.properties` with these three fictional database credentials:

   ```properties
   datasource.username=admin
   datasource.password=PazzW0rt
   datasource.url=jdbc:sqlserver://not-a-real-student-database.database.windows.net:1433;database=Students
   ```

   These don't point to a real database — `DummyDataSource` only *pretends* to connect. `getConnection()` should still return `null`, exactly as before.

3. Register the file with Spring by adding `@PropertySource` to `ApplicationConfiguration` — **and don't skip the `PropertySourcesPlaceholderConfigurer` bean**. Without it, `@Value("${...}")` placeholders silently fail to resolve in plain Spring (see the main [Lesson 3 README](README.md#-propertysource) for why):

   ```java
   @Configuration
   @ComponentScan("be.vives.ti")
   @PropertySource("classpath:/application.properties")
   public class ApplicationConfiguration {

       @Bean
       public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
           return new PropertySourcesPlaceholderConfigurer();
       }

       // existing vivesMailTemplate() @Bean stays unchanged
   }
   ```

4. In `DummyDataSource`, add three `@Value`-annotated fields for the properties above:

   ```java
   @Value("${datasource.username}")
   private String username;

   @Value("${datasource.password}")
   private String password;

   @Value("${datasource.url}")
   private String url;
   ```

## 📝 Part 2: Logging the Injected Properties

The main lesson teaches **SLF4J + Logback**, not `java.util.logging` — stay consistent with that (and with how the rest of the course logs, including the testing lesson later on).

1. Add these dependencies to `pom.xml`:

   ```xml
   <dependency>
       <groupId>org.slf4j</groupId>
       <artifactId>slf4j-api</artifactId>
       <version>2.0.16</version>
   </dependency>
   <dependency>
       <groupId>ch.qos.logback</groupId>
       <artifactId>logback-classic</artifactId>
       <version>1.5.12</version>
   </dependency>
   ```

2. In `DummyDataSource`, create a logger and use it in `getConnection()` to log the three injected values every time the method is called:

   ```java
   private static final Logger log = LoggerFactory.getLogger(DummyDataSource.class);

   @Override
   public Connection getConnection() throws SQLException {
       log.info("Connecting with username={}, url={}", username, url);
       log.debug("Using password={}", password); // never log secrets at INFO or above in real code!
       return null;
   }
   ```

   > **Note on the `password` log line**: logging a password is normally forbidden (see the main lesson's Logging Best Practices). It's included here at `DEBUG` level purely so you can *see* that property injection worked end to end on a fictional, non-existent database — treat it as a one-off teaching device, not a pattern to reuse.

3. Run `SchoolAdminApp.main()` and confirm the three property values appear in the console log output.

## 🌍 Part 3: Environment-Aware Email Sending with Profiles

Right now, `DummyEmailService` always "sends" (prints) emails, in every environment. In practice, you never want a development run to actually notify real students. We'll fix this with `@Profile` — the same conditional-configuration mechanism from the main lesson.

1. **Create an interface** `be.vives.ti.service.EmailService`:

   ```java
   public interface EmailService {
       void sendEmail(Teacher teacher, String message, Student student);
   }
   ```

2. **Make `DummyEmailService` implement it**, and annotate its `sendEmail` method with `@Override`.

3. **Refactor `TeacherService`** so its field and constructor parameter are typed `EmailService` instead of the concrete `DummyEmailService`. This is the same "depend on an abstraction" idea behind the DAO/service split from Lesson 2.

4. Run the application again — it should behave exactly as before. Spring only has one bean of type `EmailService` (`DummyEmailService`), so nothing is ambiguous yet.

5. **Create a second implementation**, `ProdEmailService`, also implementing `EmailService` and annotated `@Service`:

   ```java
   @Service
   public class ProdEmailService implements EmailService {

       private static final Logger log = LoggerFactory.getLogger(ProdEmailService.class);

       @Override
       public void sendEmail(Teacher teacher, String message, Student student) {
           log.info("Email is sent");
       }
   }
   ```

6. **Run the application now, before adding `@Profile` to either class.** You should see Spring fail to start the context with an error along these lines:

   ```
   UnsatisfiedDependencyException: ... No qualifying bean of type 'be.vives.ti.service.EmailService' available:
   expected single matching bean but found 2: dummyEmailService,prodEmailService
   ```

   This is the exact situation `@Profile` exists to solve: two beans satisfy the same type, and Spring has no way to choose between them on its own.

7. **Add `@Profile`** to both implementations:

   ```java
   @Service
   @Profile("dev")
   public class DummyEmailService implements EmailService { ... }

   @Service
   @Profile("prod")
   public class ProdEmailService implements EmailService { ... }
   ```

8. Set the active profile to `dev` as [a VM option in your IntelliJ](https://www.jetbrains.com/help/idea/program-arguments-and-environment-variables.html#vm_options) run configuration (`-Dspring.profiles.active=dev`) and run the app — the email content should be logged to the console, exactly like before.

9. Change the VM option to `-Dspring.profiles.active=prod` and run again — only `"Email is sent"` should be logged; the message content should **not** appear, since `ProdEmailService` never logs it.

## ✅ Verification Checklist

- [ ] `application.properties` exists under `src/main/resources` (marked as Resources Root) with the three `datasource.*` properties
- [ ] `ApplicationConfiguration` has `@PropertySource` **and** the `PropertySourcesPlaceholderConfigurer` bean
- [ ] `DummyDataSource` logs the three injected values (via SLF4J, not `System.out.println` or `java.util.logging`) every time `getConnection()` runs
- [ ] `EmailService` interface exists; `TeacherService` depends on the interface, not on `DummyEmailService` directly
- [ ] Both `DummyEmailService` (`@Profile("dev")`) and `ProdEmailService` (`@Profile("prod")`) exist and compile
- [ ] Running with `dev` active logs the email content; running with `prod` active does not

## 📤 Submitting Your Work

Commit and push your changes to your own GitHub Classroom repository, as instructed on Toledo.

## 🔍 Reference Solution

At the time of writing, no reference-solution branch for this lesson's exercise has been published yet in [`vives-backendprogramming/SchoolAdministration`](https://github.com/vives-backendprogramming/SchoolAdministration) (only `opgave`, `ioctoepassen`, and `springcontextaware` exist, covering Lesson 2). Use the [Lesson 2 reference solution](../lesson-02-spring-di-ioc/EXERCISE.md#-reference-solution) as your starting point and this document as your guide.

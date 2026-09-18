# Lesson 2 — Exercise: Refactoring SchoolAdministration to IoC & DI

## 📚 Table of Contents

- [📘 Overview](#-overview)
- [🎯 Learning Objectives](#-learning-objectives)
- [🚀 Getting Started](#-getting-started)
- [🧩 Starting Point: A Tightly-Coupled Application](#-starting-point-a-tightly-coupled-application)
- [🔧 Step 1: Manual Dependency Injection (No Spring Yet)](#-step-1-manual-dependency-injection-no-spring-yet)
- [🌱 Step 2: Introduce the Spring IoC Container](#-step-2-introduce-the-spring-ioc-container)
- [✅ Verification Checklist](#-verification-checklist)
- [📤 Submitting Your Work](#-submitting-your-work)
- [🔍 Reference Solution](#-reference-solution)

---

## 📘 Overview

This hands-on exercise puts the theory from the [README](README.md) into practice. You'll take a small, tightly-coupled console application — **SchoolAdministration** — through the exact same two-stage journey described there: first apply **Inversion of Control by hand** (no framework), then let the **Spring IoC container** do the wiring for you.

The application is a "send a message from a teacher to a student" tool. It's deliberately small so the coupling problem — and the improvement DI brings — is easy to see end to end.

## 🎯 Learning Objectives

By the end of this exercise, you will have:
- Recognized tight coupling in real code (classes `new`-ing their own dependencies) and removed it via constructor injection, **without any framework**
- Applied the correct Spring stereotype annotations (`@Repository`, `@Service`, `@Component`) to an existing codebase
- Written a Java-based Spring configuration class (`@Configuration`, `@ComponentScan`, `@Bean`) for a bean that Spring can't construct on its own
- Retrieved a bean from an `ApplicationContext` by type and used it exactly like any other object

## 🚀 Getting Started

1. Clone the starter project: [`vives-backendprogramming/SchoolAdministrationStart`](https://github.com/vives-backendprogramming/SchoolAdministrationStart)
2. Open it in IntelliJ.
3. Run `SchoolAdminApp.main()` and look at the console output — you should see a mock "email" printed for a message sent from a teacher to one student, and a batch of mock emails sent to an entire class.
4. Explore the project. Make sure you understand each class's responsibility before touching anything:

| Class | Responsibility |
|---|---|
| `SchoolAdminApp` | Entry point (`main` method) |
| `TeacherService` | Business logic for sending messages, orchestrates `TeacherDao`, `StudentService`, `DummyEmailService` |
| `StudentService` | Business logic for looking up students, orchestrates `StudentDao` |
| `TeacherDao` / `StudentDao` | "Database access" (via the dummy datasource below) |
| `DummyDataSource` | Fake `javax.sql.DataSource` — simulates a DB connection that doesn't really exist |
| `SchoolDatabaseStub` | In-memory fake database (hardcoded students & teachers) |
| `DummyEmailService` | Fakes sending an email by printing it to the console, using `TemplateService` for the header/footer |
| `TemplateService` | Formats the email header/footer from a `MailTemplate` |
| `MailTemplate` | Plain data holder (header, footer, logo) |
| `Person` / `Student` / `Teacher` | Domain model |

## 🧩 Starting Point: A Tightly-Coupled Application

Look closely at the constructors. Every service and DAO creates its own collaborators with `new`:

```java
public class TeacherService {
    private TeacherDao teacherDao;
    private StudentService studentService;
    private DummyEmailService dummyEmailService;

    public TeacherService() {
        this.teacherDao = new TeacherDao();
        this.studentService = new StudentService();
        this.dummyEmailService = new DummyEmailService();
    }
    // ...
}
```

This is the "traditional approach (no IoC)" from the main lesson's theory, applied to a whole object graph instead of one class. Problems this creates:
- **Impossible to unit test** — you can't substitute a fake `TeacherDao` into `TeacherService` without changing its source code
- **Duplicated instances** — every `new TeacherService()` silently creates its *own* `TeacherDao`, `StudentDao`, `DummyDataSource`, `SchoolDatabaseStub`, etc., even though these are stateless and could be shared
- **Hidden dependency graph** — you can't tell what an object needs just by looking at how it's constructed from the outside

## 🔧 Step 1: Manual Dependency Injection (No Spring Yet)

**Goal**: refactor the application to the Inversion of Control principle — **without adding Spring**. Prove to yourself that IoC is a *design principle*, not something Spring invents.

> **IoC in one line**: *a dependency is not created by the class that needs it — some other class is "in control" and hands it over.*

**Rules for this step:**
1. Go through `TeacherDao`, `StudentDao`, `TemplateService`, `DummyEmailService`, `StudentService` and `TeacherService`. For each one, figure out for yourself what it currently creates internally with `new` — and turn that into a constructor parameter instead. If class `A` currently does `new B()` inside its own constructor, `A` should end up with a constructor that simply *receives* a `B`.
2. The **only** place still allowed to use `new` for these six classes afterwards is `SchoolAdminApp.main()`. It becomes the application's single *composition root* — the one place responsible for building the whole object graph and handing each object what it needs.
3. `SchoolDatabaseStub` and `DummyDataSource` have no dependencies of their own — their constructors don't change.
4. **The end result must behave identically.** Same console output as before you started. You are only changing *how the objects come into existence*, not what they do.

In `SchoolAdminApp.main()`, construct everything **once**, bottom-up (dependencies before the things that need them), then wire it all into `TeacherService` until it has what it needs to run the same two lines it ran before.

Run it again — the console output should be unchanged.

## 🌱 Step 2: Introduce the Spring IoC Container

**Goal**: let Spring do what you just did by hand in `SchoolAdminApp.main()`.

1. **Add Spring to `pom.xml`.** You need `spring-core` and `spring-context`. Look up the latest stable **Spring Framework 7.x** version on [mvnrepository.com](https://mvnrepository.com/artifact/org.springframework/spring-context) and introduce a `<spring.version>` property so the version number appears **exactly once** in your `pom.xml`:

   ```xml
   <properties>
       <spring.version>7.0.9</spring.version> <!-- check mvnrepository.com for the newest 7.x.y patch -->
   </properties>

   <dependencies>
       <dependency>
           <groupId>org.springframework</groupId>
           <artifactId>spring-core</artifactId>
           <version>${spring.version}</version>
       </dependency>
       <dependency>
           <groupId>org.springframework</groupId>
           <artifactId>spring-context</artifactId>
           <version>${spring.version}</version>
       </dependency>
   </dependencies>
   ```

2. **Determine which classes should become Spring beans** and need to be picked up by the Spring application context when it starts. Give each one the correct annotation — choose the right stereotype: `@Component`, `@Service`, `@Repository`, `@Controller`.

   Base your choice on what you learned in the theory (recall the [Spring Stereotype Annotations](README.md#-spring-stereotype-annotations) section): what layer does each class belong to — data access, business logic, or neither? And think about it the other way round too: **not every class in this project needs to become a bean at all.** Which ones represent behavior Spring should manage as a shared, reusable object, and which ones are just data, created fresh for a specific value each time?

3. **Create a Java-based configuration class** `be.vives.ti.config.ApplicationConfiguration`:

   ```java
   @Configuration
   @ComponentScan("be.vives.ti")
   public class ApplicationConfiguration {

       @Bean
       public MailTemplate vivesMailTemplate() {
           return new MailTemplate(
                   "VIVES - Design your future",
                   "VIVES - all rights reserved",
                   "vives.jpg");
       }
   }
   ```

   `MailTemplate` needs its own `@Bean` method because it's a plain data object created with specific literal values — Spring has no way to construct it on its own the way it can for a `@Component`-annotated class.

4. **Rewrite `SchoolAdminApp.main()`** to use the Spring container instead of manual wiring:

   ```java
   public class SchoolAdminApp {
       public static void main(String[] args) {
           ApplicationContext context = new AnnotationConfigApplicationContext(ApplicationConfiguration.class);

           TeacherService teacherService = context.getBean(TeacherService.class);
           teacherService.sendMessage(1, "Waarom was je afwezig?", 10);
           teacherService.sendMessageToAllStudentsOfClass(1, "3SD", "Afwerken tegen volgende les");
       }
   }
   ```

   Every line with `new` for your own classes should now be gone from `main()` — the only object you construct yourself is the `ApplicationContext`.

5. Run it. **Same console output, once again** — but now Spring owns the object graph, and your classes no longer know or care how their dependencies were built.

## ✅ Verification Checklist

- [ ] No class other than `SchoolAdminApp` contains a `new` call for one of its own collaborators
- [ ] `SchoolAdminApp.main()` only creates the `ApplicationContext` and asks it for `TeacherService`
- [ ] Every class that should be a Spring bean has a stereotype annotation matching its layer — and classes that shouldn't be beans don't have one
- [ ] `ApplicationConfiguration` uses `@ComponentScan` + a `@Bean` method for `MailTemplate`
- [ ] The console output after Step 2 is identical to the output before you started

## 📤 Submitting Your Work

Commit and push your changes to your own Classroom repository

## 🔍 Reference Solution

A reference solution is available at [`vives-backendprogramming/SchoolAdministration`](https://github.com/vives-backendprogramming/SchoolAdministration):
- Branch [`ioctoepassen`](https://github.com/vives-backendprogramming/SchoolAdministration/tree/ioctoepassen) — end state of Step 1
- Branch [`springcontextaware`](https://github.com/vives-backendprogramming/SchoolAdministration/tree/springcontextaware) — end state of Step 2

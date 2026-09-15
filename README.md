# Eventa

Eventa is a web application for browsing events and buying tickets, built with Java and Spring Boot as a student project for the Internet Software Architectures course.

Visitors can find events, buy tickets and print them. Administrators manage events and organizers, track orders and check tickets at the entrance. Payments are simulated, so no real money is charged. The application interface is in Serbian.

## Screenshots

**Home page**

The event catalog includes search and filters by city and category.

![Home page with event search and catalog](docs/images/pocetna.png)

<details>
<summary>Admin dashboard and tickets</summary>

**Admin dashboard**

An overview of events, ticket sales, registered users and recent orders.

![Admin dashboard](docs/images/administracija.png)

**Printable tickets**

Each ticket contains event details and a unique code for entry checks. Tickets can also be saved as a PDF through the browser's print dialog.

![An order with two tickets ready for printing](docs/images/ulaznice-stampa.png)

</details>

## Features

- Search events and view venues, dates, organizers and available ticket types.
- Create an account, sign in and buy Standard or VIP tickets.
- View your orders, print tickets and cancel orders before the event starts.
- Add and edit events, venues, categories, organizers and ticket types in the admin area.
- Manage users, orders and tickets.
- Check ticket codes and record entry, preventing the same ticket from being used twice.

The server calculates prices and checks availability and capacity during checkout. Transactions and locking prevent two customers from buying the last available ticket. Events can have multiple organizers, and organizers can be linked to multiple events.

## Tech stack

| Area | Technologies |
| --- | --- |
| Backend | Java 21, Spring Boot, Spring MVC |
| Frontend | Thymeleaf, HTML, CSS, JavaScript |
| Authentication and access control | Spring Security, BCrypt, sessions and CSRF protection |
| Data | Spring Data JPA / Hibernate, MySQL, H2, Flyway |
| Testing | JUnit 5, MockMvc, Playwright |
| Build and deployment | Maven, Docker Compose |

## Run with Docker

Install and start Docker Desktop, then run:

```sh
git clone https://github.com/ToroRossoLGD/eventa.git
cd eventa
docker compose up --build -d
```

If you already have the repository, run the last command from the project folder. The first build takes longer while Docker images and dependencies are downloaded.

Open **[localhost:8080](http://localhost:8080)**. Compose starts the application and MySQL. Demo events and accounts are created on the first run.

| Role | Email | Password |
| --- | --- | --- |
| Administrator | `admin@eventa.rs` | `Admin123!` |
| Visitor | `ana@eventa.rs` | `Posetilac123!` |

To try the main flow, sign in as a visitor, choose an event and buy a ticket. Then sign in as the administrator to see the order and dashboard.

Useful commands:

```sh
docker compose logs -f app  # View application logs
docker compose stop        # Stop the containers
docker compose up -d       # Start them again
```

Data is stored in a Docker volume and is kept after `docker compose down`. MySQL is available at `localhost:3307`, with `eventa` as both the database name and username. The default local password is `eventa_local_demo`. You can set database passwords through `DB_PASSWORD` and `DB_ROOT_PASSWORD`.

If port 8080 is busy, stop the other application or change the port mapping in [compose.yaml](compose.yaml). If the application cannot connect to the database on the first run, wait for MySQL to finish initializing and run `docker compose up -d app`.

<details>
<summary>Run without Docker</summary>

With Java 21 and Maven 3.9+ installed:

```sh
mvn spring-boot:run
```

The default profile uses H2 and stores data in the `data/` folder. The URL and demo accounts are the same as for Docker.

On Windows, these scripts can set up Java and Maven in the local `.tools/` folder without changing the system PATH:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/bootstrap.ps1
powershell -ExecutionPolicy Bypass -File scripts/start.ps1
```

Press `Ctrl+C` in the terminal to stop the application. The local and Docker versions need different ports if you want to run both at once.

</details>

## Tests

Integration tests cover purchases, price calculations, access control, cancellations, entry checks and concurrent requests for the last available ticket.

```sh
mvn -B verify
```

On Windows, you can also run `.\mvnw.cmd -B verify`. This project script uses Maven from `.tools/` or the system PATH.

Browser tests require Node.js and a running demo instance:

```sh
npm ci
npx playwright install chromium
npm run test:e2e
```

Playwright tests use demo accounts and create test data. MySQL integration tests require a separate test database because they delete its application data. See the [testing report](docs/TESTIRANJE.md) for details and previous results. The report is in Serbian.

## Project structure

```text
src/main/java/rs/singidunum/eventa/
  config/         Security configuration and demo data
  domain/         JPA entities
  repository/     Database access
  service/        Business logic
  web/            MVC controllers and forms

src/main/resources/
  db/migration/   Flyway migrations
  templates/      Thymeleaf pages
  static/         CSS, JavaScript and illustrations

src/test/         Integration tests
tests/e2e/        Browser tests
scripts/          Local setup and startup scripts
docs/             Documentation and screenshots
```

The [project documentation](docs/PROJEKTNA_DOKUMENTACIJA.md) describes the database model, entity relationships and business rules in Serbian.

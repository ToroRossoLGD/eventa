# Eventa — organizacija događaja i prodaja ulaznica

Studentski projekat za predmet **Internet softverske arhitekture**. Spring MVC aplikacija sa korisničkim delom, administratorskim panelom, osam poslovnih tabela i spojnom tabelom za događaje i organizatore.

Provereno: **24 integraciona testa na H2, istih 24 na MySQL i 4 Chromium E2E testa**. Docker aplikacija je izgrađena i pokrenuta sa MySQL-om.

![Početna stranica aplikacije Eventa](docs/images/pocetna.png)

## Pokretanje na ovom Windows računaru

Na ovom računaru je već pokrenuta Docker varijanta na **http://localhost:8080**. Ponovno pokretanje Docker varijante: `docker compose up -d`. Sledeće komande pokreću alternativni lokalni demo režim; prethodno zaustavite Docker aplikaciju komandom `docker compose stop app` da oslobodite port 8080.

U PowerShell terminalu otvorenom u folderu projekta:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start.ps1
```

Otvorite **http://localhost:8080**. Prvo pokretanje kreira bazu i demo podatke. Podaci ostaju sačuvani u `data/` i posle gašenja aplikacije. Zaustavljanje: `Ctrl+C` u terminalu u kome aplikacija radi.

Java 21 i Maven 3.9.11 su lokalno pripremljeni u ignorisanom direktorijumu `.tools/`. Na novom Windows računaru prvo pokrenite:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/bootstrap.ps1
powershell -ExecutionPolicy Bypass -File scripts/start.ps1
```

Bootstrap preuzima Microsoft OpenJDK i Apache Maven sa zvaničnih adresa; ne menja sistemski PATH. Ako već imate Java 21 i Maven, dovoljno je `mvn spring-boot:run` (Windows: može i `mvnw.cmd spring-boot:run`). `mvnw.cmd` je praktični lokalni pokretač, nije standardni Maven Wrapper. Za Linux/macOS koristite instalirane Java 21 i Maven 3.9+ ili Docker.

### Demo nalozi

| Uloga | Email | Lozinka |
|---|---|---|
| Administrator | `admin@eventa.rs` | `Admin123!` |
| Posetilac | `ana@eventa.rs` | `Posetilac123!` |

Demo nalozi i početni događaji kreiraju se samo kada je `app.seed=true` i tabela korisnika prazna. Datumi događaja računaju se u odnosu na prvo pokretanje. Za prikaz posle dužeg vremena administrator može izmeniti datume događaja. Demo podaci su izmišljeni primeri; nisu stvarna ponuda događaja. Plaćanje je **simulirano**, bez kartica i naplate.

## MySQL i Docker

Uz pokrenut Docker Desktop:

```powershell
docker compose up --build -d
```

Aplikacija: **http://localhost:8080**. MySQL: `localhost:3307`, baza `eventa`, korisnik `eventa`, lokalna demo lozinka `eventa_local_demo`. Docker volume `eventa_mysql` čuva podatke. Portovi su dostupni samo sa lokalnog računara. Pre pokretanja Docker aplikacije zaustavite lokalnu aplikaciju na portu 8080 ili promenite mapiranje porta.

```powershell
docker compose logs -f app
docker compose down
```

`docker compose down` zadržava podatke. Konfiguracija u `compose.yaml` namenjena je lokalnoj demonstraciji. Za drugo okruženje postavite svoje lozinke kroz `DB_PASSWORD` i `DB_ROOT_PASSWORD` i isključite demo inicijalizaciju.

### Lokalna Java aplikacija + MySQL iz Dockera

```powershell
docker compose up -d db
$env:DB_URL='jdbc:mysql://localhost:3307/eventa?connectionTimeZone=Europe/Belgrade&allowPublicKeyRetrieval=true&useSSL=false'
$env:DB_USER='eventa'
$env:DB_PASSWORD='eventa_local_demo'
$env:APP_SEED='true'
powershell -ExecutionPolicy Bypass -File scripts/start.ps1 -Profile mysql
```

Za već instalirani MySQL prvo napravite praznu bazu i korisnika, zatim prilagodite `DB_URL`, `DB_USER`, `DB_PASSWORD`. Šemu automatski pravi **Flyway** iz verzionisanih SQL datoteka u `src/main/resources/db/migration/`; Hibernate je samo proverava (`ddl-auto=validate`). Demo H2 i MySQL koriste istu migraciju. Flyway dodaje i svoju tehničku tabelu istorije migracija, pored osam poslovnih tabela i spojne tabele `event_organizers`.

## Funkcionalnosti

- ManyToMany veza događaja i organizatora preko `event_organizers`: više organizatora po događaju i više događaja po organizatoru. Izbor se uređuje čekiranjem u formi događaja; javni detalji prikazuju organizatore i kontakte. Povezani organizator se prvo uklanja sa događaja, pa se može obrisati.

- Katalog događaja sa pretragom po tekstu, kategoriji i gradu.
- Detalji događaja, lokacija, datum i dostupne Standard/VIP ulaznice.
- Registracija i prijava; BCrypt lozinke, sesije, CSRF zaštita i dve uloge.
- Kupovina 1–10 ulaznica po porudžbini, server računa cenu.
- Jedinstveni UUID kod svake ulaznice, pregled i štampa/PDF iz pregledača.
- Pregled sopstvenih porudžbina i otkazivanje pre početka događaja.
- CRUD za korisnike, lokacije, kategorije, događaje, tipove ulaznica, porudžbine, ulaznice i organizatore.
- Administratorska kontrolna tabla: događaji, korisnici, prodate ulaznice i simulirani prihod.
- Provera koda i jednokratna evidencija ulaska, od dva sata pre do 12 sati posle početka.
- Transakcije i zaključavanje događaja za zaštitu od prekomerne prodaje i dvostrukog ulaska.
- Validacija kapaciteta lokacije, količina, cena i povezanih zapisa pri brisanju.
- Prikaz na telefonu i računaru, lokalne SVG ilustracije, bez obaveznih spoljašnjih fontova ili CDN resursa.

Porudžbina pripada jednom događaju. Kod kupovine se bira jedan tip i količina; administrator može dodati drugi tip istog događaja. Statusi porudžbina i ulaznica menjaju se kroz poslovna pravila: otkazani zapisi se ne reaktiviraju, a iskorišćene ulaznice ne brišu. Ukupna vrednost porudžbine je istorijski zbir cena njenih sačuvanih ulaznica; otkazivanje ne briše taj iznos. Kontrolna tabla u prihod računa samo neotkazane ulaznice plaćenih porudžbina.

## Testovi

```powershell
.\mvnw.cmd -B verify
```

Integracioni testovi koriste izdvojenu H2 bazu u memoriji i fiksni sat. Proveravaju HTTP prikaze, bezbednost, CRUD, tačan obračun, kapacitete, otkazivanje, ulazak i konkurentne zahteve. Izveštaji su u `target/surefire-reports/`.

Za testiranje u pravom pregledaču, uz pokrenutu demo aplikaciju sa početnih šest događaja:

```powershell
npm ci
npx playwright install chromium
npm run test:e2e
```

Playwright testovi proveravaju desktop i mobilni prikaz, registraciju, kupovinu, štampu, otkazivanje i administratorski CRUD. Koriste demo naloge i kreiraju sopstvene probne zapise koje po uspešnom završetku uklanjaju. Pokrećite ih na lokalnoj demo instanci. Snimci i HTML izveštaj nalaze se u `artifacts/`. Node.js je potreban samo za ove opcione testove i formatiranje, ne za rad Java aplikacije.

Isti testovi podržavaju MySQL kroz `TEST_DB_URL`, `TEST_DB_USER`, `TEST_DB_PASSWORD`. **Koristite posebnu praznu test bazu**: testovi brišu poslovne zapise pre svakog scenarija. Primer URL-a: `jdbc:mysql://localhost:3307/eventa_test?connectionTimeZone=Europe/Belgrade&allowPublicKeyRetrieval=true&useSSL=false`.

## Struktura

```text
src/main/java/rs/singidunum/eventa/
  config/       Spring Security i demo podaci
  domain/       Osam JPA entiteta
  repository/   Spring Data JPA repozitorijumi
  service/      Kupovina, administracija i nalozi
  web/          MVC kontroleri, registracija i obrada grešaka
src/main/resources/
  db/migration/ Flyway SQL migracija
  templates/    Thymeleaf stranice i fragmenti
  static/       CSS, JavaScript i SVG ilustracije
src/test/       Integracioni testovi
docs/           Projektna dokumentacija
scripts/        Priprema okruženja i pokretanje
```

Detaljno: [Projektna dokumentacija](docs/PROJEKTNA_DOKUMENTACIJA.md), [Izveštaj provere](docs/TESTIRANJE.md).

Tehnologije: Java 21, Spring Boot 3.5.16, Spring MVC, Thymeleaf, Spring Security, Spring Data JPA/Hibernate, Flyway, MySQL 8.4, H2, JUnit 5 i MockMvc. Verzija Jave je u podržanom opsegu iz [zvaničnih Spring Boot sistemskih zahteva](https://docs.spring.io/spring-boot/3.5/system-requirements.html).

# Eventa

Eventa je studentski projekat iz predmeta Internet softverske arhitekture — web aplikacija za pregled događaja i kupovinu ulaznica, napravljena u Javi i Spring Boot-u.

Posetilac može da pronađe događaj, kupi ulaznice i sačuva ih za štampu. Administrator uređuje događaje i organizatore, prati porudžbine i proverava ulaznice na ulazu. Kupovina je demonstraciona, bez stvarne naplate.

## Kako izgleda

**Početna stranica** — katalog događaja sa pretragom i filterima po gradu i kategoriji.

![Početna stranica sa pretragom i katalogom događaja](docs/images/pocetna.png)

<details>
<summary>Administracija i izgled ulaznica</summary>

**Administracija** — pregled događaja, prodatih ulaznica, korisnika i poslednjih porudžbina.

![Administratorska kontrolna tabla](docs/images/administracija.png)

**Ulaznice za štampu** — svaka ulaznica ima podatke o događaju i jedinstveni kod za proveru ulaska. Iz pregledača se mogu sačuvati i kao PDF.

![Porudžbina sa dve ulaznice pripremljene za štampu](docs/images/ulaznice-stampa.png)

</details>

## Šta može da se radi

- Pretraga događaja i pregled lokacije, termina, organizatora i dostupnih tipova ulaznica.
- Registracija, prijava i kupovina Standard ili VIP ulaznica.
- Pregled svojih porudžbina, štampa ulaznica i otkazivanje pre početka događaja.
- Dodavanje i izmena događaja, lokacija, kategorija, organizatora i tipova ulaznica kroz administraciju.
- Upravljanje korisnicima, porudžbinama i ulaznicama.
- Provera koda ulaznice i evidencija ulaska, uz sprečavanje ponovnog korišćenja iste ulaznice.

Pri kupovini se proveravaju raspoloživost i kapacitet, a cena se računa na serveru. Transakcije i zaključavanje sprečavaju da dva korisnika kupe poslednju raspoloživu ulaznicu. Jedan događaj može imati više organizatora, a isti organizator može učestvovati na više događaja.

## Tehnologije

| Deo projekta | Tehnologije |
| --- | --- |
| Backend | Java 21, Spring Boot, Spring MVC |
| Korisnički interfejs | Thymeleaf, HTML, CSS, JavaScript |
| Prijava i prava pristupa | Spring Security, BCrypt, sesije i CSRF zaštita |
| Podaci | Spring Data JPA / Hibernate, MySQL, H2, Flyway |
| Testovi | JUnit 5, MockMvc, Playwright |
| Pokretanje | Maven, Docker Compose |

## Pokretanje preko Dockera

Potreban je instaliran i pokrenut Docker Desktop.

```sh
git clone https://github.com/ToroRossoLGD/eventa.git
cd eventa
docker compose up --build -d
```

Ako je projekat već preuzet, pokreni poslednju komandu iz njegovog foldera. Prva izgradnja traje nešto duže zbog preuzimanja biblioteka i Docker slika.

Aplikacija se otvara na **[localhost:8080](http://localhost:8080)**. Compose pokreće aplikaciju i MySQL bazu, a pri prvom pokretanju se dodaju demo događaji i nalozi.

| Uloga | Email | Lozinka |
| --- | --- | --- |
| Administrator | `admin@eventa.rs` | `Admin123!` |
| Posetilac | `ana@eventa.rs` | `Posetilac123!` |

Za kratak obilazak prijavi se kao posetilac, izaberi događaj i kupi ulaznicu. Zatim se prijavi kao administrator i pogledaj porudžbinu i kontrolnu tablu.

Korisne komande:

```sh
docker compose logs -f app  # Logovi aplikacije
docker compose stop        # Zaustavljanje
docker compose up -d       # Ponovno pokretanje
```

Podaci ostaju sačuvani u Docker volume-u i nakon `docker compose down`. MySQL je dostupan na `localhost:3307`, sa bazom i korisnikom `eventa`. Podrazumevana lokalna lozinka je `eventa_local_demo`; lozinke se mogu podesiti promenljivama `DB_PASSWORD` i `DB_ROOT_PASSWORD`.

Ako je port 8080 zauzet, zaustavi drugu aplikaciju ili promeni mapiranje u [compose.yaml](compose.yaml). Ako pri prvom pokretanju aplikacija prijavi da ne može da se poveže sa bazom, sačekaj da MySQL završi inicijalizaciju i pokreni `docker compose up -d app`.

<details>
<summary>Pokretanje bez Dockera</summary>

Uz instalirane Java 21 i Maven 3.9+:

```sh
mvn spring-boot:run
```

Podrazumevani profil koristi H2 bazu i čuva podatke u folderu `data/`. Adresa i demo nalozi su isti kao u Docker varijanti.

Na Windows-u možeš koristiti i skripte koje pripremaju Javu i Maven u lokalnom folderu `.tools/`, bez promene sistemskog PATH-a:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/bootstrap.ps1
powershell -ExecutionPolicy Bypass -File scripts/start.ps1
```

Aplikacija se zaustavlja sa `Ctrl+C` u terminalu. Nemoj istovremeno pokretati lokalnu i Docker varijantu na istom portu.

</details>

## Testovi

Integracioni testovi proveravaju kupovinu, obračun cene, prava pristupa, otkazivanje, proveru ulaska i istovremene zahteve za poslednju ulaznicu.

```sh
mvn -B verify
```

Na Windows-u je dostupan i lokalni pokretač `mvnw.cmd`: `.\mvnw.cmd -B verify` (koristi Maven iz `.tools/` ili sistemskog PATH-a).

Za testove u pregledaču potrebni su Node.js i pokrenuta demo aplikacija:

```sh
npm ci
npx playwright install chromium
npm run test:e2e
```

Playwright testovi koriste demo naloge i kreiraju probne podatke. Za integracione testove sa MySQL-om koristi se posebna test baza, jer testovi brišu njene poslovne podatke. Detalji i prethodni rezultati nalaze se u [izveštaju testiranja](docs/TESTIRANJE.md).

## Organizacija koda

```text
src/main/java/rs/singidunum/eventa/
├── config/       Podešavanja bezbednosti i demo podaci
├── domain/       JPA entiteti
├── repository/   Pristup bazi
├── service/      Poslovna logika
└── web/          MVC kontroleri i forme

src/main/resources/
├── db/migration/ Flyway migracije
├── templates/    Thymeleaf stranice
└── static/       CSS, JavaScript i ilustracije

src/test/         Integracioni testovi
tests/e2e/        Testovi u pregledaču
scripts/          Skripte za lokalno pokretanje
docs/             Dokumentacija i snimci ekrana
```

Model baze, veze između entiteta i poslovna pravila opisani su u [projektnoj dokumentaciji](docs/PROJEKTNA_DOKUMENTACIJA.md).

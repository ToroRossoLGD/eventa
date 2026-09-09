# Priprema odbrane — Eventa

## Demonstracija za približno 8–10 minuta

1. **Tema i tehnologija (1 minut).** „Eventa je sistem za organizaciju događaja i prodaju ulaznica. Izabrao sam Spring MVC varijantu projekta. Koristim Java 21, Spring Boot, Thymeleaf, Spring Security, JPA i relacionu bazu. Projekat ima sedam poslovnih tabela.”
2. **Katalog (1 minut).** Otvorite početnu, pretražite „Jazz”, promenite kategoriju i grad. Otvorite događaj i pokažite lokaciju, termin, cenu i preostalu količinu.
3. **Korisnik i kupovina (2 minuta).** Registrujte probni nalog ili se prijavite kao `ana@eventa.rs`. Kupite dve ulaznice. Pokažite izračunat iznos, napomenu o simulaciji, jedinstvene kodove, Moje ulaznice i prikaz za štampu.
4. **Otkazivanje (1 minut).** Otkažite porudžbinu. Otvorite događaj i pokažite da su mesta ponovo raspoloživa. Objasnite da iskorišćene ulaznice sprečavaju otkazivanje porudžbine.
5. **Administracija (2 minuta).** Prijavite se kao `admin@eventa.rs`. Pokažite kontrolnu tablu i sedam evidencija. Dodajte i izmenite kategoriju, zatim je obrišite. Pokažite da povezanu lokaciju nije moguće obrisati. Promenite količinu tipa na broj iznad kapaciteta i pokažite validaciju.
6. **Ulazak (1 minut).** Za poseban demonstracioni događaj postavite početak oko 30 minuta u budućnosti. Kupite jednu ulaznicu, kopirajte kod i otvorite administrativnu proveru ulaza. Prva provera uspeva, druga sa istim kodom se odbija.
7. **Arhitektura i testovi (1–2 minuta).** Pokažite ER dijagram, `BookingService.purchase`, zaključavanje u `EventRepository` i rezultat `mvnw.cmd verify`. Objasnite zašto provera količine i upis moraju biti u istoj transakciji.

Ne menjajte sistemski sat radi demonstracije. Demo događaji su u budućnosti, pa za kontrolu ulaza promenite datum jednog probnog događaja u administratorskoj formi. Vreme se tumači u zoni Europe/Belgrade. Plaćanje se ne naplaćuje stvarno.

## Pitanja koja treba razumeti

**Šta je MVC?** Model nosi podatke, View ih prikazuje, a Controller prihvata zahtev i određuje odgovor. U projektu su kontroleri u `web`, prikazi u `templates`, a domen i servisi upravljaju podacima i pravilima.

**Šta donosi Spring Boot?** Automatsku konfiguraciju, ugrađeni web server i jednostavno pokretanje. Ne zamenjuje MVC: olakšava podešavanje aplikacije koja koristi MVC.

**Zašto servisni sloj?** Pravila kupovine treba da budu ista bez obzira da li je poziva korisnički ili administratorski kontroler. Servis je i granica transakcije.

**Šta je JPA, a šta Hibernate?** JPA definiše standard za relaciono mapiranje Java objekata. Hibernate je implementacija tog standarda. Spring Data JPA daje repozitorijumske interfejse i generiše uobičajene operacije.

**Šta znači CRUD?** Create, Read, Update, Delete. U aplikaciji svaka od sedam evidencija ima prikaz, formu kreiranja, izmenu i brisanje uz poslovna ograničenja.

**Zašto `BigDecimal` za cenu?** Decimalne cene treba obračunavati bez grešaka binarne reprezentacije koje može dati `double`.

**Kako se sprečava prekomerna prodaja?** Zaključava se red događaja, proverava stvarno raspoloživa količina i upisuje kupovina unutar jedne transakcije. Konkurentni zahtev čeka i zatim vidi novu raspoloživost.

**Zašto `READ_COMMITTED`?** Upit posle čekanja na zaključavanje treba da vidi potvrđene ulaznice prethodne transakcije. Time se izbegava stari snapshot za brojanje na MySQL-u.

**Zašto osvežavanje entiteta posle zaključavanja?** Entitet je možda učitan pre nego što je zahtev čekao drugi zahtev. `refresh` čita trenutno stanje iz baze umesto ranije vrednosti iz persistence context-a.

**Kako se čuvaju lozinke?** Kao BCrypt hash sa salt-om. Originalna lozinka se ne čuva i ne prikazuje u administratorskoj formi.

**Autentifikacija naspram autorizacije?** Autentifikacija utvrđuje ko je korisnik. Autorizacija proverava koje operacije sme da radi. Dodatno proveravamo vlasništvo konkretne porudžbine.

**Čemu CSRF token?** Sprečava da druga stranica bez odgovarajućeg tokena pošalje važeću izmenu koristeći korisnikovu sesiju.

**Zašto posebna istorijska cena na ulaznici?** Promena današnje cene ne sme promeniti iznos prethodne kupovine.

**Kako rade migracije?** Flyway izvršava verzionisani SQL pri pokretanju i pamti šta je izvršeno. Hibernate zatim proverava da šema odgovara entitetima.

**Zašto postoje H2 i MySQL profili?** H2 omogućava brzo pokretanje i izolovane testove. MySQL profil i Docker Compose omogućavaju rad sa zasebnim relacionim serverom. Isti Java kod i SQL migracija koriste se u oba profila.

**Da li je ovo REST ili mikroservisni projekat?** Ovo je Spring MVC realizacija: server vraća HTML i aplikacija se pokreće kao jedna celina. To je jedna od dozvoljenih opcija iz uputstva.

## Pre predaje

- Dopunite ime, prezime i broj indeksa u projektnoj dokumentaciji.
- Pokrenite aplikaciju i prođite demonstraciju na računaru za odbranu.
- Pripremite događaj za proveru ulaza i proverite datume ostalih događaja.
- Pokrenite `mvnw.cmd verify` i sačuvajte izveštaj.
- Predajte izvorni kod, `pom.xml`, SQL migraciju, dokumentaciju, skripte i Docker konfiguraciju. `.tools`, `node_modules`, privatne `.env` podatke i runtime bazu ne treba uključivati u izvorni projekat.

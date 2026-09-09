# Izveštaj testiranja

Datum provere: **9. septembar 2026.**

## Automatske integracione provere

Test klasa: `src/test/java/rs/singidunum/eventa/EventaIntegrationTest.java`.

| Okruženje | Rezultat |
|---|---|
| Java 21, Spring Boot 3.5.16, H2 2.3, test profil | 21 test prošao, 0 neuspešnih, 0 grešaka |
| Java 21, Spring Boot 3.5.16, MySQL 8.4 u Docker-u, posebna `eventa_test` baza | 21 test prošao, 0 neuspešnih, 0 grešaka |
| Docker Compose: izgradnja aplikacije i pokretanje sa MySQL | Uspešno; aplikacija dostupna na localhost:8080 |
| Chromium, aplikacija u Docker-u sa MySQL bazom | 3 E2E testa prošla |

Provereno:

1. Javni prikazi, pretraga i filteri.
2. Zaštita administracije, kupovine i CSRF.
3. Registracija, BCrypt, prijava i duplikat email adrese.
4. Tačan iznos, jedinstveni kodovi i istorijska cena.
5. Nedovoljna količina i neispravne vrednosti bez delimičnog upisa.
6. Zabrana kupovine prošlog i otkazanog događaja.
7. Otkazivanje vraća raspoloživost; ponovljeno otkazivanje je idempotentno.
8. Zabrana čitanja i otkazivanja tuđe porudžbine.
9. Kompletan MVC tok korisničke kupovine, pregleda i otkazivanja.
10. Jednokratni ulazak i zabrana otkazivanja iskorišćene ulaznice.
11. Nevažeći kod, otkazana ulaznica i vremenski prozor ulaska.
12. Dve istovremene kupovine poslednje ulaznice: tačno jedna uspeva.
13. Dve istovremene provere istog koda: tačno jedna odobrava ulaz.
14. Renderovanje kontrolne table, provere ulaza i svih administratorskih lista, novih i izmenjenih formi.
15. CRUD korisnika, lokacija, kategorija, događaja i tipova ulaznica.
16. CRUD porudžbina i ulaznica sa usklađivanjem cene i inventara.
17. Ograničenje kapaciteta i brisanja povezanih zapisa.
18. Otkazivanje događaja poništava porudžbine i ulaznice i zadržava druge izmene.
19. Neispravne administratorske vrednosti, očuvanje formulara, HTTP 400 i 404.
20. Zabrana reaktivacije otkazanih zapisa i brisanja sopstvenog administratora.
21. Očuvanje iskorišćene ulaznice u evidenciji i nakon otkazivanja celog događaja.

Konkurentni testovi koriste dve niti, zajedničku startnu barijeru i posebne transakcije. Ne predstavljaju test velikog opterećenja. Sat u integracionim testovima je fiksiran, pa vremenski testovi ne zavise od dana pokretanja.

## Provere u Chromium pregledaču

Testovi: `tests/e2e/eventa.spec.js`. Playwright 1.60.0.

- Katalog i pretraga „Jazz”; desktop 1440 × 1000 i telefon 390 × 844.
- Provera odsustva horizontalnog prelivanja na katalogu, detaljima i administraciji.
- Registracija novog naloga, stvarna forma prijave, izbor dve ulaznice, iznos, kupovina, prikaz kodova i print CSS.
- Otkazivanje porudžbine i statusi ulaznica.
- Kontrolna tabla, kreiranje/izmena/brisanje probne kategorije i odbijanje nepostojećeg koda na ulazu.

Snimci ekrana kataloga, administracije i štampe vizuelno su pregledani. Izveštaj i snimci generišu se u `artifacts/`. Ugrađeni pregledač nije mogao da se poveže zbog greške alata; provera je izvršena u lokalnom Chromium-u kroz Playwright.

## Reprodukcija

```powershell
.\mvnw.cmd -B verify
npm ci
npx playwright install chromium
npm run test:e2e
```

Pre E2E testova aplikacija treba da bude pokrenuta sa demo nalozima i šest početnih događaja. `EVENTA_BASE_URL` menja adresu instance. Probni zapisi se po uspešnom scenariju brišu kroz administraciju.

Za MySQL testove napravite izdvojenu bazu i dodelite test korisniku prava nad njom. Zatim postavite `TEST_DB_URL`, `TEST_DB_USER`, `TEST_DB_PASSWORD` i pokrenite `mvnw.cmd test`. Testovi brišu poslovne zapise u toj bazi; ne koristiti radnu bazu aplikacije.

## Granice provere

Provereni su lokalni scenariji na H2 i MySQL bazama, uz automatizovani Chromium. Nisu rađeni testovi velikog opterećenja, potpuni bezbednosni audit, testovi drugih browser engine-a ili integracija sa bankom. Plaćanje je namerno simulirano. Prikaz za štampu pregledan je kroz print media režim; rezultat fizičkog štampača zavisi od njegovih postavki.

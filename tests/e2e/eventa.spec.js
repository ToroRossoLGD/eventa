const { test, expect } = require("@playwright/test");

test("organizers can be assigned together and removed from an event", async ({
  page,
}) => {
  const suffix = Date.now();
  const names = [`Organizator A ${suffix}`, `Organizator B ${suffix}`];
  const title = `Zajednički događaj ${suffix}`;
  await page.goto("/login");
  await page
    .getByRole("textbox", { name: "Email adresa" })
    .fill("admin@eventa.rs");
  await page.getByLabel("Lozinka", { exact: true }).fill("Admin123!");
  await page.getByRole("button", { name: "Prijavi se" }).click();
  for (const name of names) {
    await page.goto("/admin/organizers/new");
    await page.getByRole("textbox", { name: "Naziv", exact: true }).fill(name);
    await page
      .getByRole("textbox", { name: "Email", exact: true })
      .fill("organizer@eventa.test");
    await page.getByRole("button", { name: "Sačuvaj promene" }).click();
    await expect(page.getByRole("status")).toContainText("Promene su sačuvane");
  }
  await page.goto("/admin/events/new");
  await page.getByRole("textbox", { name: "Naziv događaja" }).fill(title);
  await page
    .getByRole("textbox", { name: "Opis", exact: true })
    .fill("Događaj sa dva organizatora.");
  await page.locator('[name="startsAt"]').fill("2030-10-10T19:00");
  for (const label of ["Lokacija", "Kategorija", "Vizuelni stil", "Status"]) {
    await page
      .getByRole("combobox", { name: label, exact: true })
      .selectOption({ index: 1 });
  }
  for (const name of names)
    await page.getByRole("checkbox", { name, exact: true }).check();
  await page.screenshot({
    path: "artifacts/organizers-form.png",
    fullPage: true,
  });
  await page.getByRole("button", { name: "Sačuvaj promene" }).click();
  await expect(page.getByRole("status")).toContainText("Promene su sačuvane");
  const row = page.getByRole("row").filter({ hasText: title });
  const editUrl = await row
    .getByRole("link", { name: "Izmeni" })
    .getAttribute("href");
  const eventId = editUrl.split("/")[3];
  await page.goto(`/events/${eventId}`);
  for (const name of names)
    await expect(page.locator(".event-organizers")).toContainText(name);
  await page.goto(editUrl);
  for (const name of names)
    await expect(
      page.getByRole("checkbox", { name, exact: true }),
    ).toBeChecked();
  await page.getByRole("checkbox", { name: names[1], exact: true }).uncheck();
  await page.getByRole("button", { name: "Sačuvaj promene" }).click();
  await expect(page.getByRole("status")).toContainText("Promene su sačuvane");
  await page.goto(`/events/${eventId}`);
  await expect(page.locator(".event-organizers")).toContainText(names[0]);
  await expect(page.locator(".event-organizers")).not.toContainText(names[1]);
  await page.goto("/admin/events");
  page.once("dialog", (dialog) => dialog.accept());
  await page
    .getByRole("row")
    .filter({ hasText: title })
    .getByRole("button", { name: "Obriši" })
    .click();
  await expect(page.getByRole("status")).toContainText("Zapis je obrisan");
  for (const name of names) {
    await page.goto("/admin/organizers");
    page.once("dialog", (dialog) => dialog.accept());
    await page
      .getByRole("row")
      .filter({ hasText: name })
      .getByRole("button", { name: "Obriši" })
      .click();
    await expect(page.getByRole("status")).toContainText("Zapis je obrisan");
  }
});

test("desktop and phone catalog are usable and have no horizontal overflow", async ({
  page,
}) => {
  await page.goto("/");
  await expect(
    page.getByRole("heading", { name: "Ne propusti ono što se pamti." }),
  ).toBeVisible();
  await expect(page.locator(".event-card")).toHaveCount(6);
  await page.screenshot({ path: "artifacts/home-desktop.png", fullPage: true });
  await page.getByRole("searchbox", { name: "Pretraga događaja" }).fill("Jazz");
  await page.getByRole("button", { name: "Pretraži" }).click();
  await expect(page.locator(".event-card")).toHaveCount(1);
  await expect(
    page.getByRole("heading", { name: "Jazz pod zvezdama" }),
  ).toBeVisible();
  await page.goto("/");
  await page.setViewportSize({ width: 390, height: 844 });
  await page.screenshot({ path: "artifacts/home-mobile.png", fullPage: true });
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBeTruthy();
  await page.getByRole("heading", { name: "Noć elektronskog zvuka" }).click();
  await expect(
    page.getByRole("heading", { name: "Izaberi ulaznicu" }),
  ).toBeVisible();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBeTruthy();
});

test("registration, purchase, ticket printing layout and cancellation", async ({
  page,
}) => {
  const email = `browser-${Date.now()}@eventa.test`;
  await page.goto("/register");
  await page
    .getByRole("textbox", { name: "Ime i prezime" })
    .fill("Probni posetilac");
  await page.getByRole("textbox", { name: "Email adresa" }).fill(email);
  await page.getByLabel("Lozinka", { exact: true }).fill("Browser123!");
  await page.getByRole("button", { name: "Napravi nalog" }).click();
  await expect(page).toHaveURL(/\/login/);
  await page.getByRole("textbox", { name: "Email adresa" }).fill(email);
  await page.getByLabel("Lozinka", { exact: true }).fill("Browser123!");
  await page.getByRole("button", { name: "Prijavi se" }).click();
  await page.getByRole("heading", { name: "Noć elektronskog zvuka" }).click();
  await page
    .locator(".ticket-option")
    .first()
    .getByRole("link", { name: "Izaberi" })
    .click();
  await page.getByRole("spinbutton", { name: "Broj ulaznica" }).fill("2");
  await expect(page.locator("#checkout-total")).toContainText("3.600,00 RSD");
  await page.getByRole("button", { name: "Potvrdi kupovinu" }).click();
  await expect(page.locator(".admission-ticket")).toHaveCount(2);
  await expect(page.getByRole("status")).toContainText("Kupovina je uspešna");
  await page.screenshot({
    path: "artifacts/order-desktop.png",
    fullPage: true,
  });
  await page.emulateMedia({ media: "print" });
  await page.screenshot({
    path: "artifacts/tickets-print.png",
    fullPage: true,
  });
  await page.emulateMedia({ media: "screen" });
  page.once("dialog", (dialog) => dialog.accept());
  await page.getByRole("button", { name: "Otkaži porudžbinu" }).click();
  await expect(page.getByRole("status")).toContainText(
    "Porudžbina je otkazana",
  );
  await expect(page.locator(".admission-ticket .badge").first()).toHaveText(
    "Otkazano",
  );
  await page.getByRole("button", { name: "Odjavi se" }).click();

  // Clean only the isolated records created by this browser scenario through the app's own admin UI.
  await page
    .getByRole("textbox", { name: "Email adresa" })
    .fill("admin@eventa.rs");
  await page.getByLabel("Lozinka", { exact: true }).fill("Admin123!");
  await page.getByRole("button", { name: "Prijavi se" }).click();
  await page.goto("/admin/orders");
  const orderRow = page.getByRole("row").filter({ hasText: email });
  page.once("dialog", (dialog) => dialog.accept());
  await orderRow.getByRole("button", { name: "Obriši" }).click();
  await expect(page.getByRole("status")).toContainText("Zapis je obrisan");
  await page.goto("/admin/users");
  page.once("dialog", (dialog) => dialog.accept());
  await page
    .getByRole("row")
    .filter({ hasText: email })
    .getByRole("button", { name: "Obriši" })
    .click();
  await expect(page.getByRole("status")).toContainText("Zapis je obrisan");
});

test("admin dashboard, validation and category CRUD", async ({ page }) => {
  await page.goto("/login");
  await page
    .getByRole("textbox", { name: "Email adresa" })
    .fill("admin@eventa.rs");
  await page.getByLabel("Lozinka", { exact: true }).fill("Admin123!");
  await page.getByRole("button", { name: "Prijavi se" }).click();
  await page.getByRole("link", { name: "Administracija", exact: true }).click();
  await expect(page.locator(".stat-card")).toHaveCount(4);
  await page.screenshot({
    path: "artifacts/admin-desktop.png",
    fullPage: true,
  });
  await page.setViewportSize({ width: 390, height: 844 });
  await page.screenshot({ path: "artifacts/admin-mobile.png", fullPage: true });
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBeTruthy();
  await page.setViewportSize({ width: 1440, height: 1000 });
  await page.getByRole("link", { name: "Kategorije", exact: true }).click();
  await page.getByRole("link", { name: "Dodaj kategoriju" }).click();
  const category = `Probna kategorija ${Date.now()}`;
  await page.getByLabel("Naziv", { exact: true }).fill(category);
  await page
    .getByLabel("Opis", { exact: true })
    .fill("Probni opis za proveru administracije.");
  await page.getByRole("button", { name: "Sačuvaj promene" }).click();
  await expect(page.getByRole("status")).toContainText("Promene su sačuvane");
  await page
    .getByRole("row")
    .filter({ hasText: category })
    .getByRole("link", { name: "Izmeni" })
    .click();
  await page
    .getByRole("textbox", { name: "Opis", exact: true })
    .fill("Izmenjeni opis.");
  await page.getByRole("button", { name: "Sačuvaj promene" }).click();
  await expect(
    page.getByRole("row").filter({ hasText: category }),
  ).toContainText("Izmenjeni opis.");
  page.once("dialog", (dialog) => dialog.accept());
  await page
    .getByRole("row")
    .filter({ hasText: category })
    .getByRole("button", { name: "Obriši" })
    .click();
  await expect(page.getByRole("row").filter({ hasText: category })).toHaveCount(
    0,
  );
  await page.getByRole("link", { name: "Provera ulaznica" }).click();
  await page
    .getByRole("textbox", { name: "Kod ulaznice" })
    .fill("nepostojeci-kod");
  await page
    .getByRole("button", { name: "Proveri i evidentiraj ulaz" })
    .click();
  await expect(page.getByRole("alert")).toContainText("ne postoji");
});

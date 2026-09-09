document.querySelectorAll("form[data-confirm]").forEach((form) => {
  form.addEventListener("submit", (event) => {
    if (!window.confirm(form.dataset.confirm)) event.preventDefault();
  });
});
document
  .querySelectorAll("[data-print]")
  .forEach((button) => button.addEventListener("click", () => window.print()));
document.querySelectorAll("[data-copy]").forEach((button) =>
  button.addEventListener("click", async () => {
    try {
      await navigator.clipboard.writeText(button.dataset.copy);
      button.textContent = "Kopirano ✓";
    } catch {
      button.textContent = "Označi i kopiraj kod iznad";
    }
  }),
);
const quantity = document.querySelector("#quantity");
if (quantity)
  quantity.addEventListener("input", () => {
    const total = Number(quantity.value) * Number(quantity.dataset.price);
    document.querySelector("#checkout-total").textContent = Number.isFinite(
      total,
    )
      ? new Intl.NumberFormat("sr-RS", {
          minimumFractionDigits: 2,
          maximumFractionDigits: 2,
        }).format(total) + " RSD"
      : "—";
  });
const search = document.querySelector("#table-search");
if (search)
  search.addEventListener("input", () => {
    const query = search.value.toLocaleLowerCase("sr-Latn");
    let count = 0;
    document.querySelectorAll("#admin-table tbody tr").forEach((row) => {
      row.hidden = !row.textContent
        .toLocaleLowerCase("sr-Latn")
        .includes(query);
      if (!row.hidden) count++;
    });
    document.querySelector("#no-table-results").hidden = count > 0;
  });

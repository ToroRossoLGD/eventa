package rs.singidunum.eventa;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import rs.singidunum.eventa.domain.*;
import rs.singidunum.eventa.repository.*;
import rs.singidunum.eventa.service.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class EventaIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired BookingService booking;
    @Autowired AdminService admin;
    @Autowired AppUserRepository users;
    @Autowired VenueRepository venues;
    @Autowired CategoryRepository categories;
    @Autowired EventRepository events;
    @Autowired TicketTypeRepository types;
    @Autowired PurchaseOrderRepository orders;
    @Autowired TicketRepository tickets;
    @Autowired PasswordEncoder encoder;
    @Autowired Clock clock;
    AppUser buyer; Venue venue; Category category; Event event; TicketType type;

    @TestConfiguration static class TimeConfig {
        @Bean @Primary Clock fixedClock() { return Clock.fixed(Instant.parse("2026-09-10T10:00:00Z"),ZoneId.of("Europe/Belgrade")); }
    }

    @BeforeEach void setup() {
        tickets.deleteAll(); orders.deleteAll(); types.deleteAll(); events.deleteAll(); categories.deleteAll(); venues.deleteAll(); users.deleteAll();
        buyer=account("buyer@test.rs","USER"); account("admin@test.rs","ADMIN"); account("other@test.rs","USER");
        venue=new Venue(); venue.setName("Test sala");venue.setAddress("Test adresa 1");venue.setCity("Beograd");venue.setCapacity(100);venues.save(venue);
        category=new Category();category.setName("Muzika");category.setDescription("Koncerti");categories.save(category);
        event=new Event();event.setTitle("Test koncert");event.setDescription("Opis test događaja.");event.setVenue(venue);event.setCategory(category);event.setStartsAt(LocalDateTime.now(clock).plusHours(1));event.setTheme("violet");event.setStatus("PUBLISHED");events.save(event);
        type=new TicketType();type.setEvent(event);type.setName("Standard");type.setPrice(new BigDecimal("1200.50"));type.setQuantity(3);types.save(type);
    }
    private AppUser account(String email,String role) { AppUser u=new AppUser();u.setName("Test korisnik");u.setEmail(email);u.setPassword(encoder.encode("Test12345!"));u.setRole(role);return users.save(u); }
    private Map<String,String> eventData(String status) { return Map.of("title","Izmenjen koncert","description","Izmenjen opis","venueId",venue.getId().toString(),"categoryId",category.getId().toString(),"startsAt",event.getStartsAt().toString(),"theme","blue","status",status); }

    @Test void publicPagesRenderAndFiltersWork() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Test koncert")));
        mvc.perform(get("/events").param("q","nepostojece")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Nismo pronašli")));
        mvc.perform(get("/events").param("category",category.getId().toString()).param("city","Beograd")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Test koncert")));
        mvc.perform(get("/events/"+event.getId())).andExpect(status().isOk());
        mvc.perform(get("/login")).andExpect(status().isOk()); mvc.perform(get("/register")).andExpect(status().isOk());
    }
    @Test void securityProtectsAdminCheckoutAndCsrf() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/checkout/"+type.getId())).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin").with(user(buyer.getEmail()).roles("USER"))).andExpect(status().isForbidden());
        mvc.perform(post("/checkout/"+type.getId()).param("quantity","1").with(user(buyer.getEmail()))).andExpect(status().isForbidden());
        assertThat(orders.count()).isZero();
    }
    @Test void registrationLoginAndDuplicateValidation() throws Exception {
        mvc.perform(post("/register").with(csrf()).param("name","Novi korisnik").param("email","NOVI@test.rs").param("password","Nova12345!")).andExpect(redirectedUrl("/login"));
        AppUser created=users.findByEmail("novi@test.rs").orElseThrow(); assertThat(created.getRole()).isEqualTo("USER"); assertThat(encoder.matches("Nova12345!",created.getPassword())).isTrue();
        mvc.perform(formLogin("/login").user("email","NOVI@test.rs").password("Nova12345!")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/"));
        mvc.perform(post("/register").with(csrf()).param("name","Novi").param("email","novi@test.rs").param("password","Nova12345!")).andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form","email"));
        mvc.perform(post("/register").with(csrf()).param("name","").param("email","x").param("password","1")).andExpect(model().attributeHasErrors("form"));
    }
    @Test void purchaseIssuesUniqueTicketsAndPreservesPrice() {
        PurchaseOrder order=booking.purchase(buyer.getEmail(),type.getId(),2);
        assertThat(order.getTotal()).isEqualByComparingTo("2401.00");
        var issued=tickets.findByOrderIdOrderByIdAsc(order.getId()); assertThat(issued).hasSize(2);
        assertThat(issued).extracting(Ticket::getCode).doesNotHaveDuplicates(); assertThat(booking.remaining(type)).isEqualTo(1);
        type.setPrice(new BigDecimal("9000.00"));types.save(type);
        assertThat(tickets.findById(issued.getFirst().getId()).orElseThrow().getUnitPrice()).isEqualByComparingTo("1200.50");
    }
    @Test void soldOutAndInvalidQuantityDoNotCreatePartialOrders() {
        assertThatThrownBy(() -> booking.purchase(buyer.getEmail(),type.getId(),4)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> booking.purchase(buyer.getEmail(),type.getId(),0)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> booking.purchase(buyer.getEmail(),type.getId(),11)).isInstanceOf(BusinessException.class);
        assertThat(orders.count()).isZero(); assertThat(tickets.count()).isZero();
    }
    @Test void pastAndCancelledEventsCannotBePurchased() {
        event.setStartsAt(LocalDateTime.now(clock).minusMinutes(1));events.save(event);
        assertThatThrownBy(() -> booking.purchase(buyer.getEmail(),type.getId(),1)).isInstanceOf(BusinessException.class);
        event.setStartsAt(LocalDateTime.now(clock).plusDays(1));event.setStatus("CANCELLED");events.save(event);
        assertThatThrownBy(() -> booking.purchase(buyer.getEmail(),type.getId(),1)).isInstanceOf(BusinessException.class);
        assertThat(orders.count()).isZero();
    }
    @Test void cancellationRestoresInventoryAndIsIdempotent() {
        var order=booking.purchase(buyer.getEmail(),type.getId(),3);booking.cancelOrder(order.getId(),buyer.getEmail(),false);booking.cancelOrder(order.getId(),buyer.getEmail(),false);
        assertThat(booking.remaining(type)).isEqualTo(3); assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo("CANCELLED");
        assertThat(tickets.findByOrderIdOrderByIdAsc(order.getId())).allMatch(t -> t.getStatus().equals("CANCELLED"));
    }
    @Test void ownershipIsCheckedForViewingAndCancelling() throws Exception {
        var order=booking.purchase(buyer.getEmail(),type.getId(),1);
        mvc.perform(get("/orders/"+order.getId()).with(user("other@test.rs"))).andExpect(status().isForbidden());
        mvc.perform(post("/orders/"+order.getId()+"/cancel").with(user("other@test.rs")).with(csrf())).andExpect(status().isForbidden());
        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo("PAID");
    }
    @Test void fullBuyerMvcFlowRendersTicketsAndCancellation() throws Exception {
        mvc.perform(get("/checkout/"+type.getId()).with(user(buyer.getEmail()))).andExpect(status().isOk());
        mvc.perform(post("/checkout/"+type.getId()).with(user(buyer.getEmail())).with(csrf()).param("quantity","2")).andExpect(status().is3xxRedirection());
        var order=orders.findAll().getFirst();
        mvc.perform(get("/orders").with(user(buyer.getEmail()))).andExpect(status().isOk());
        mvc.perform(get("/orders/"+order.getId()).with(user(buyer.getEmail()))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString(tickets.findByOrderIdOrderByIdAsc(order.getId()).getFirst().getCode())));
        mvc.perform(post("/orders/"+order.getId()+"/cancel").with(user(buyer.getEmail())).with(csrf())).andExpect(redirectedUrl("/orders/"+order.getId()));
    }
    @Test void checkInIsSingleUseAndUsedTicketCannotBeCancelled() {
        var order=booking.purchase(buyer.getEmail(),type.getId(),1); var ticket=tickets.findByOrderIdOrderByIdAsc(order.getId()).getFirst();
        assertThat(booking.checkIn(ticket.getCode()).getStatus()).isEqualTo("USED");
        assertThatThrownBy(() -> booking.checkIn(ticket.getCode())).isInstanceOf(BusinessException.class).hasMessageContaining("već iskorišćena");
        assertThatThrownBy(() -> booking.cancelOrder(order.getId(),buyer.getEmail(),false)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> booking.deleteTicket(ticket.getId())).isInstanceOf(BusinessException.class);
    }
    @Test void invalidCancelledAndOutsideWindowTicketsAreRejected() {
        assertThatThrownBy(() -> booking.checkIn("invalid")).isInstanceOf(BusinessException.class);
        var order=booking.purchase(buyer.getEmail(),type.getId(),1); var ticket=tickets.findByOrderIdOrderByIdAsc(order.getId()).getFirst();
        event.setStartsAt(LocalDateTime.now(clock).plusDays(5));events.save(event);
        assertThatThrownBy(() -> booking.checkIn(ticket.getCode())).isInstanceOf(BusinessException.class).hasMessageContaining("2 sata");
        booking.cancelOrder(order.getId(),buyer.getEmail(),false);
        assertThatThrownBy(() -> booking.checkIn(ticket.getCode())).isInstanceOf(BusinessException.class);
    }
    @Test void concurrentBuyersCannotOversellLastTicket() throws Exception {
        type.setQuantity(1);types.save(type);
        assertThat(runTogether(() -> { booking.purchase(buyer.getEmail(),type.getId(),1); })).containsExactlyInAnyOrder(true,false);
        assertThat(tickets.count()).isEqualTo(1);assertThat(orders.count()).isEqualTo(1);assertThat(booking.remaining(type)).isZero();
    }
    @Test void simultaneousCheckInsOnlyAdmitOnce() throws Exception {
        var order=booking.purchase(buyer.getEmail(),type.getId(),1); String code=tickets.findByOrderIdOrderByIdAsc(order.getId()).getFirst().getCode();
        assertThat(runTogether(() -> { booking.checkIn(code); })).containsExactlyInAnyOrder(true,false);
    }
    private List<Boolean> runTogether(Runnable work) throws Exception {
        try(var pool=Executors.newFixedThreadPool(2)) {
            CountDownLatch ready=new CountDownLatch(2),go=new CountDownLatch(1);
            Callable<Boolean> task=() -> { ready.countDown();go.await();try {work.run();return true;}catch(BusinessException ex){return false;} };
            Future<Boolean> a=pool.submit(task),b=pool.submit(task);assertThat(ready.await(5,TimeUnit.SECONDS)).isTrue();go.countDown();
            return List.of(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS));
        }
    }
    @Test void allAdminListsCreateAndEditPagesRender() throws Exception {
        var order=booking.purchase(buyer.getEmail(),type.getId(),1);var ticket=tickets.findByOrderIdOrderByIdAsc(order.getId()).getFirst();
        var ids=Map.of("users",buyer.getId(),"venues",venue.getId(),"categories",category.getId(),"events",event.getId(),"types",type.getId(),"orders",order.getId(),"tickets",ticket.getId());
        mvc.perform(get("/admin").with(user("admin@test.rs").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/admin/check-in").with(user("admin@test.rs").roles("ADMIN"))).andExpect(status().isOk());
        for(var entry:ids.entrySet()) for(String path:List.of("/admin/"+entry.getKey(),"/admin/"+entry.getKey()+"/new","/admin/"+entry.getKey()+"/"+entry.getValue()+"/edit"))
            mvc.perform(get(path).with(user("admin@test.rs").roles("ADMIN"))).andExpect(status().isOk());
    }
    @Test void adminCrudForUsersVenuesCategoriesEventsAndTypes() {
        admin.save("users",null,Map.of("name","Novi","email","new@test.rs","password","Test12345!","role","USER"),"admin@test.rs");
        var u=users.findByEmail("new@test.rs").orElseThrow();admin.save("users",u.getId(),Map.of("name","Izmenjen","email","new@test.rs","password","","role","USER"),"admin@test.rs"); assertThat(users.findById(u.getId()).orElseThrow().getName()).isEqualTo("Izmenjen");admin.delete("users",u.getId(),"admin@test.rs");
        admin.save("venues",null,Map.of("name","Nova sala","address","Adresa","city","Niš","capacity","30"),"admin@test.rs");var v=venues.findAll().stream().filter(x -> x.getName().equals("Nova sala")).findFirst().orElseThrow();admin.save("venues",v.getId(),Map.of("name","Nova sala 2","address","Adresa","city","Niš","capacity","50"),"admin@test.rs");assertThat(venues.findById(v.getId()).orElseThrow().getCapacity()).isEqualTo(50);admin.delete("venues",v.getId(),"admin@test.rs");
        admin.save("categories",null,Map.of("name","Sport","description","Takmičenja"),"admin@test.rs");var c=categories.findAll().stream().filter(x -> x.getName().equals("Sport")).findFirst().orElseThrow();admin.save("categories",c.getId(),Map.of("name","Sport 2","description","Takmičenja 2"),"admin@test.rs");assertThat(categories.findById(c.getId()).orElseThrow().getName()).isEqualTo("Sport 2");admin.delete("categories",c.getId(),"admin@test.rs");
        admin.save("events",null,eventData("PUBLISHED"),"admin@test.rs");var e=events.findAll().stream().filter(x -> !x.getId().equals(event.getId())).findFirst().orElseThrow();admin.save("events",e.getId(),eventData("PUBLISHED"),"admin@test.rs");assertThat(events.findById(e.getId()).orElseThrow().getTheme()).isEqualTo("blue");
        var data=Map.of("name","VIP","eventId",e.getId().toString(),"quantity","10","price","500.50");admin.save("types",null,data,"admin@test.rs");var t=types.findByEventIdOrderByPriceAsc(e.getId()).getFirst();admin.save("types",t.getId(),Map.of("name","VIP Plus","eventId",e.getId().toString(),"quantity","12","price","700"),"admin@test.rs");assertThat(types.findById(t.getId()).orElseThrow().getQuantity()).isEqualTo(12);admin.delete("types",t.getId(),"admin@test.rs");admin.delete("events",e.getId(),"admin@test.rs");
    }
    @Test void adminOrderAndTicketCrudMaintainsTotalsAndStock() {
        admin.save("orders",null,Map.of("userId",buyer.getId().toString(),"typeId",type.getId().toString(),"quantity","1"),"admin@test.rs");var o=orders.findAll().getFirst();
        admin.save("tickets",null,Map.of("orderId",o.getId().toString(),"typeId",type.getId().toString()),"admin@test.rs");assertThat(orders.findById(o.getId()).orElseThrow().getTotal()).isEqualByComparingTo("2401.00");
        var t=tickets.findByOrderIdOrderByIdAsc(o.getId()).getLast();admin.save("tickets",t.getId(),Map.of("status","CANCELLED"),"admin@test.rs");assertThat(booking.remaining(type)).isEqualTo(2);admin.delete("tickets",t.getId(),"admin@test.rs");assertThat(orders.findById(o.getId()).orElseThrow().getTotal()).isEqualByComparingTo("1200.50");
        admin.save("orders",o.getId(),Map.of("status","CANCELLED"),"admin@test.rs");assertThat(booking.remaining(type)).isEqualTo(3);admin.delete("orders",o.getId(),"admin@test.rs");assertThat(orders.count()).isZero();assertThat(tickets.count()).isZero();
    }
    @Test void capacityAndReferentialIntegrityAreEnforced() {
        assertThatThrownBy(() -> admin.save("types",null,Map.of("eventId",event.getId().toString(),"name","VIP","price","5","quantity","98"),"admin@test.rs")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> admin.save("venues",venue.getId(),Map.of("name","x","address","y","city","z","capacity","2"),"admin@test.rs")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> admin.delete("categories",category.getId(),"admin@test.rs")).isInstanceOf(BusinessException.class);
        booking.purchase(buyer.getEmail(),type.getId(),3);
        assertThatThrownBy(() -> admin.save("types",type.getId(),Map.of("eventId",event.getId().toString(),"name","Standard","price","5","quantity","2"),"admin@test.rs")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> admin.delete("types",type.getId(),"admin@test.rs")).isInstanceOf(BusinessException.class);
    }
    @Test void cancellingEventInvalidatesTicketsAndSavesOtherEdits() {
        var o=booking.purchase(buyer.getEmail(),type.getId(),1);admin.save("events",event.getId(),eventData("CANCELLED"),"admin@test.rs");
        var updated=events.findById(event.getId()).orElseThrow();assertThat(updated.getStatus()).isEqualTo("CANCELLED");assertThat(updated.getTitle()).isEqualTo("Izmenjen koncert");assertThat(updated.getTheme()).isEqualTo("blue");
        assertThat(orders.findById(o.getId()).orElseThrow().getStatus()).isEqualTo("CANCELLED");assertThat(tickets.findByOrderIdOrderByIdAsc(o.getId()).getFirst().getStatus()).isEqualTo("CANCELLED");
    }
    @Test void adminValidationPreservesFormAndRejectsInvalidNumbers() throws Exception {
        mvc.perform(post("/admin/types/save").with(user("admin@test.rs").roles("ADMIN")).with(csrf()).param("name","Loša cena").param("eventId",event.getId().toString()).param("quantity","2").param("price","-5")).andExpect(status().isOk()).andExpect(model().attributeExists("error")).andExpect(content().string(org.hamcrest.Matchers.containsString("Loša cena")));
        mvc.perform(get("/events/not-a-number")).andExpect(status().isBadRequest());
        mvc.perform(get("/events/99999999")).andExpect(status().isNotFound());
    }
    @Test void cancelledRecordsCannotBeReactivatedAndOwnAdminCannotBeDeleted() {
        var o=booking.purchase(buyer.getEmail(),type.getId(),1);var t=tickets.findByOrderIdOrderByIdAsc(o.getId()).getFirst();booking.cancelOrder(o.getId(),buyer.getEmail(),false);
        assertThatThrownBy(() -> admin.save("orders",o.getId(),Map.of("status","PAID"),"admin@test.rs")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> booking.changeTicketStatus(t.getId(),"ACTIVE")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> admin.delete("users",users.findByEmail("admin@test.rs").orElseThrow().getId(),"admin@test.rs")).isInstanceOf(BusinessException.class);
    }
    @Test void usedTicketRemainsInAuditEvenAfterEventCancellation() {
        var order=booking.purchase(buyer.getEmail(),type.getId(),1);
        var ticket=tickets.findByOrderIdOrderByIdAsc(order.getId()).getFirst();
        booking.checkIn(ticket.getCode());
        admin.save("events",event.getId(),eventData("CANCELLED"),"admin@test.rs");
        assertThatThrownBy(() -> booking.deleteOrder(order.getId())).isInstanceOf(BusinessException.class);
        assertThat(tickets.findById(ticket.getId()).orElseThrow().getStatus()).isEqualTo("USED");
        assertThat(orders.existsById(order.getId())).isTrue();
    }
}

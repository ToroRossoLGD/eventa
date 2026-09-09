package rs.singidunum.eventa.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.dao.DataIntegrityViolationException;
import rs.singidunum.eventa.domain.*;
import rs.singidunum.eventa.repository.*;
import rs.singidunum.eventa.service.*;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.*;

@Controller @RequiredArgsConstructor
public class PublicController {
    private final EventRepository events;
    private final CategoryRepository categories;
    private final TicketTypeRepository types;
    private final PurchaseOrderRepository orders;
    private final TicketRepository tickets;
    private final BookingService booking;
    private final AccountService accounts;
    private final OrganizerRepository organizers;

    public record EventCard(Event event, BigDecimal price, long available) {}
    private EventCard card(Event event) {
        List<TicketType> list = types.findByEventIdOrderByPriceAsc(event.getId());
        return new EventCard(event,list.stream().map(TicketType::getPrice).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO),
            list.stream().mapToLong(booking::remaining).sum());
    }

    @GetMapping({"/", "/events"})
    String home(@RequestParam(defaultValue="") String q, @RequestParam(required=false) Long category,
                @RequestParam(defaultValue="") String city, Model model) {
        List<Event> available = events.findAllByOrderByStartsAtAsc().stream().filter(booking::onSale).toList();
        String search = q.strip().toLowerCase(Locale.ROOT);
        model.addAttribute("cards", available.stream()
            .filter(e -> category==null || e.getCategory().getId().equals(category))
            .filter(e -> city.isBlank() || e.getVenue().getCity().equals(city))
            .filter(e -> (e.getTitle()+" "+e.getDescription()+" "+e.getVenue().getName()).toLowerCase(Locale.ROOT).contains(search))
            .map(this::card).toList());
        model.addAttribute("categories",categories.findAll());
        model.addAttribute("cities",available.stream().map(e -> e.getVenue().getCity()).distinct().sorted().toList());
        model.addAttribute("eventCount",available.size());
        model.addAttribute("q",q); model.addAttribute("selectedCategory",category); model.addAttribute("selectedCity",city);
        return "home";
    }

    @GetMapping("/events/{id}")
    String event(@PathVariable Long id, Model model) {
        model.addAttribute("organizers",organizers.findByEventsIdOrderByNameAscIdAsc(id));
        Event event = events.findById(id).orElseThrow(NotFoundException::new);
        List<TicketType> list = types.findByEventIdOrderByPriceAsc(id);
        Map<Long,Long> remaining = new HashMap<>(); list.forEach(t -> remaining.put(t.getId(), booking.remaining(t)));
        model.addAttribute("event",event); model.addAttribute("types",list);
        model.addAttribute("remaining",remaining); model.addAttribute("onSale",booking.onSale(event));
        return "event";
    }

    @GetMapping("/login") String login() { return "login"; }
    @GetMapping("/register") String register(Model model) { model.addAttribute("form",new RegistrationForm()); return "register"; }
    @PostMapping("/register")
    String register(@Valid @ModelAttribute("form") RegistrationForm form, BindingResult errors, RedirectAttributes flash) {
        if (errors.hasErrors()) return "register";
        try { accounts.register(form.getName(),form.getEmail(),form.getPassword()); }
        catch (BusinessException e) {
            errors.rejectValue(e.getMessage().contains("Lozinka") ? "password" : "email","invalid",e.getMessage()); return "register";
        } catch (DataIntegrityViolationException e) {
            errors.rejectValue("email","duplicate","Nalog sa ovom email adresom već postoji."); return "register";
        }
        flash.addFlashAttribute("success","Nalog je napravljen. Prijavite se i pronađite svoj sledeći događaj.");
        return "redirect:/login";
    }

    @GetMapping("/checkout/{typeId}")
    String checkout(@PathVariable Long typeId, Model model) {
        TicketType type = types.findById(typeId).orElseThrow(NotFoundException::new);
        model.addAttribute("type",type); model.addAttribute("remaining",booking.remaining(type));
        model.addAttribute("onSale",booking.onSale(type.getEvent()));
        return "checkout";
    }

    @PostMapping("/checkout/{typeId}")
    String buy(@PathVariable Long typeId, @RequestParam int quantity, Principal principal, RedirectAttributes flash) {
        try {
            PurchaseOrder order = booking.purchase(principal.getName(),typeId,quantity);
            flash.addFlashAttribute("success","Kupovina je uspešna! Vaše ulaznice su spremne.");
            return "redirect:/orders/"+order.getId();
        } catch (BusinessException e) { flash.addFlashAttribute("error",e.getMessage()); return "redirect:/checkout/"+typeId; }
    }

    @GetMapping("/orders")
    String orders(Principal principal, Model model) {
        model.addAttribute("orders", orders.findByUserEmailOrderByCreatedAtDesc(principal.getName())); return "orders";
    }
    @GetMapping("/orders/{id}")
    String order(@PathVariable Long id, Principal principal, Model model) {
        PurchaseOrder order = booking.ownedOrder(id,principal.getName());
        model.addAttribute("order",order); model.addAttribute("tickets",tickets.findByOrderIdOrderByIdAsc(id));
        model.addAttribute("canCancel",booking.onSale(order.getEvent()) && order.getStatus().equals("PAID")
            && tickets.findByOrderIdOrderByIdAsc(id).stream().noneMatch(t -> t.getStatus().equals("USED")));
        return "order";
    }
    @PostMapping("/orders/{id}/cancel")
    String cancel(@PathVariable Long id, Principal principal, RedirectAttributes flash) {
        try { booking.cancelOrder(id,principal.getName(),false); flash.addFlashAttribute("success","Porudžbina je otkazana, a ulaznice vraćene u prodaju."); }
        catch (BusinessException e) { flash.addFlashAttribute("error",e.getMessage()); }
        return "redirect:/orders/"+id;
    }
}

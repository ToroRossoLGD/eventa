package rs.singidunum.eventa.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import rs.singidunum.eventa.domain.*;
import rs.singidunum.eventa.repository.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;

@Service @RequiredArgsConstructor
public class BookingService {
    private final EventRepository events;
    private final TicketTypeRepository types;
    private final PurchaseOrderRepository orders;
    private final TicketRepository tickets;
    private final AppUserRepository users;
    private final Clock clock;

    public long remaining(TicketType type) {
        return type.getQuantity() - tickets.countByTicketTypeIdAndStatusNot(type.getId(), "CANCELLED");
    }

    public boolean onSale(Event event) {
        return event.getStatus().equals("PUBLISHED") && event.getStartsAt().isAfter(LocalDateTime.now(clock));
    }

    public Event lockEvent(Long id) {
        Event event = events.lockById(id).orElseThrow(NotFoundException::new);
        entityManager.refresh(event);
        return event;
    }

    private void requireSale(Event event) {
        if (!onSale(event)) throw new BusinessException("Prodaja za ovaj događaj nije dostupna.");
    }

    // All inventory mutations acquire the same event lock before checking capacity.
    @Transactional
    public PurchaseOrder purchase(String email, Long typeId, int quantity) {
        if (quantity < 1 || quantity > 10) throw new BusinessException("Izaberite od 1 do 10 ulaznica.");
        TicketType type = types.findById(typeId).orElseThrow(NotFoundException::new);
        Event event = lockEvent(type.getEvent().getId());
        entityManager.refresh(type);
        requireSale(event);
        if (remaining(type) < quantity) throw new BusinessException("Nema dovoljno raspoloživih ulaznica. Izaberite manju količinu.");
        PurchaseOrder order = new PurchaseOrder();
        order.setUser(users.findByEmail(email).orElseThrow(NotFoundException::new));
        order.setEvent(event);
        order.setCreatedAt(LocalDateTime.now(clock));
        order.setStatus("PAID");
        order.setTotal(type.getPrice().multiply(BigDecimal.valueOf(quantity)));
        orders.save(order);
        for (int i=0; i<quantity; i++) issue(order, type);
        return order;
    }

    private Ticket issue(PurchaseOrder order, TicketType type) {
        Ticket ticket = new Ticket();
        ticket.setOrder(order); ticket.setTicketType(type);
        ticket.setCode(UUID.randomUUID().toString());
        ticket.setUnitPrice(type.getPrice()); ticket.setStatus("ACTIVE");
        return tickets.save(ticket);
    }

    public PurchaseOrder ownedOrder(Long id, String email) {
        PurchaseOrder order = orders.findById(id).orElseThrow(NotFoundException::new);
        if (!order.getUser().getEmail().equals(email)) throw new AccessDeniedException("Pristup nije dozvoljen.");
        return order;
    }

    @Transactional
    public void cancelOrder(Long id, String email, boolean admin) {
        PurchaseOrder order = orders.findById(id).orElseThrow(NotFoundException::new);
        lockEvent(order.getEvent().getId());
        entityManager.refresh(order);
        if (!admin && !order.getUser().getEmail().equals(email)) throw new AccessDeniedException("Pristup nije dozvoljen.");
        if (order.getStatus().equals("CANCELLED")) return;
        List<Ticket> entries = tickets.findByOrderIdOrderByIdAsc(id);
        if (entries.stream().anyMatch(t -> t.getStatus().equals("USED")))
            throw new BusinessException("Porudžbina sadrži iskorišćenu ulaznicu i ne može se otkazati.");
        if (!admin && !order.getEvent().getStartsAt().isAfter(LocalDateTime.now(clock)))
            throw new BusinessException("Otkazivanje je dostupno samo pre početka događaja.");
        entries.forEach(t -> t.setStatus("CANCELLED"));
        order.setStatus("CANCELLED");
    }

    @Transactional
    public Ticket checkIn(String code) {
        Ticket found = tickets.findByCode(code.trim()).orElseThrow(() -> new BusinessException("Ulaznica sa ovim kodom ne postoji."));
        Event event = lockEvent(found.getTicketType().getEvent().getId());
        // Reload after the event lock; a simultaneous check-in may have changed this ticket.
        return checkInLocked(found.getId(), event);
    }

    private Ticket checkInLocked(Long id, Event event) {
        Ticket ticket = freshTicket(id);
        if (!event.getStatus().equals("PUBLISHED") || !ticket.getOrder().getStatus().equals("PAID"))
            throw new BusinessException("Događaj ili porudžbina su otkazani.");
        if (!ticket.getStatus().equals("ACTIVE"))
            throw new BusinessException(ticket.getStatus().equals("USED") ? "Ulaznica je već iskorišćena." : "Ulaznica je otkazana.");
        LocalDateTime now = LocalDateTime.now(clock);
        if (now.isBefore(event.getStartsAt().minusHours(2)) || now.isAfter(event.getStartsAt().plusHours(12)))
            throw new BusinessException("Ulaz je moguć od 2 sata pre do 12 sati posle početka događaja.");
        ticket.setStatus("USED");
        return ticket;
    }

    private final jakarta.persistence.EntityManager entityManager;
    private Ticket freshTicket(Long id) {
        Ticket ticket = tickets.findById(id).orElseThrow(NotFoundException::new);
        entityManager.refresh(ticket);
        entityManager.refresh(ticket.getOrder());
        return ticket;
    }

    @Transactional
    public Ticket addTicket(Long orderId, Long typeId) {
        PurchaseOrder order = orders.findById(orderId).orElseThrow(NotFoundException::new);
        Event event = lockEvent(order.getEvent().getId());
        entityManager.refresh(order);
        TicketType type = types.findById(typeId).orElseThrow(NotFoundException::new);
        requireSale(event);
        if (!order.getStatus().equals("PAID")) throw new BusinessException("Otkazanoj porudžbini nije moguće dodati ulaznicu.");
        if (!type.getEvent().getId().equals(event.getId())) throw new BusinessException("Tip ulaznice mora pripadati događaju porudžbine.");
        if (remaining(type)<1) throw new BusinessException("Izabrani tip ulaznice je rasprodat.");
        Ticket ticket = issue(order,type);
        order.setTotal(order.getTotal().add(type.getPrice()));
        return ticket;
    }

    @Transactional
    public void changeTicketStatus(Long id, String status) {
        Ticket ticket = tickets.findById(id).orElseThrow(NotFoundException::new);
        Event event = lockEvent(ticket.getTicketType().getEvent().getId());
        ticket = freshTicket(id);
        if (ticket.getStatus().equals(status)) return;
        if (status.equals("USED")) { checkInLocked(id,event); return; }
        if (!status.equals("CANCELLED")) throw new BusinessException("Otkazanu ili iskorišćenu ulaznicu nije moguće ponovo aktivirati.");
        if (ticket.getStatus().equals("USED")) throw new BusinessException("Iskorišćenu ulaznicu nije moguće otkazati.");
        ticket.setStatus("CANCELLED");
    }

    @Transactional
    public void deleteTicket(Long id) {
        Ticket ticket = tickets.findById(id).orElseThrow(NotFoundException::new);
        lockEvent(ticket.getTicketType().getEvent().getId());
        ticket = freshTicket(id);
        if (ticket.getStatus().equals("USED")) throw new BusinessException("Iskorišćena ulaznica ostaje u evidenciji.");
        PurchaseOrder order = ticket.getOrder();
        order.setTotal(order.getTotal().subtract(ticket.getUnitPrice()));
        tickets.delete(ticket); tickets.flush();
        if (tickets.findByOrderIdOrderByIdAsc(order.getId()).isEmpty()) order.setStatus("CANCELLED");
    }

    @Transactional
    public void deleteOrder(Long id) {
        cancelOrder(id, "", true);
        List<Ticket> entries = tickets.findByOrderIdOrderByIdAsc(id);
        if (entries.stream().anyMatch(t -> t.getStatus().equals("USED")))
            throw new BusinessException("Porudžbina sa iskorišćenom ulaznicom ostaje u evidenciji.");
        tickets.deleteAll(entries);
        orders.deleteById(id);
    }

    @Transactional
    public void cancelEvent(Event event) {
        lockEvent(event.getId());
        for (PurchaseOrder order : orders.findByEventId(event.getId())) {
            for (Ticket ticket : tickets.findByOrderIdOrderByIdAsc(order.getId()))
                if (ticket.getStatus().equals("ACTIVE")) ticket.setStatus("CANCELLED");
            order.setStatus("CANCELLED");
        }
        event.setStatus("CANCELLED");
    }
}

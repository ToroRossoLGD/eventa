package rs.singidunum.eventa.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import rs.singidunum.eventa.domain.*;
import rs.singidunum.eventa.repository.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service @RequiredArgsConstructor
public class AdminService {
    private final AppUserRepository users;
    private final VenueRepository venues;
    private final CategoryRepository categories;
    private final OrganizerRepository organizers;
    private final EventRepository events;
    private final TicketTypeRepository types;
    private final PurchaseOrderRepository orders;
    private final TicketRepository tickets;
    private final BookingService booking;
    private final PasswordEncoder encoder;
    private final jakarta.persistence.EntityManager em;

    public record Resource(String key,String label,String singular,String description) {}
    public record Option(String value,String label) {}
    public record Field(String name,String label,String type,String value,List<Option> options,boolean required,String hint) {}
    public record Row(Long id,List<String> cells) {}
    public List<Resource> resources() { return List.of(
        new Resource("organizers","Organizatori","organizatora","Organizatori i kontakti događaja."),
        new Resource("events","Događaji","događaj","Program, termini i status prodaje."),
        new Resource("venues","Lokacije","lokaciju","Prostori u kojima nastaju doživljaji."),
        new Resource("categories","Kategorije","kategoriju","Organizujte događaje po interesovanjima."),
        new Resource("types","Tipovi ulaznica","tip ulaznice","Cene i kapacitet za svaki događaj."),
        new Resource("users","Korisnici","korisnika","Nalozi posetilaca i administratora."),
        new Resource("orders","Porudžbine","porudžbinu","Pregled simuliranih kupovina i otkazivanja."),
        new Resource("tickets","Ulaznice","ulaznicu","Jedinstveni kodovi i evidencija ulaska.")); }
    public Resource resource(String key) { return resources().stream().filter(r -> r.key().equals(key)).findFirst().orElseThrow(NotFoundException::new); }
    public List<String> headers(String key) { return switch(key) {
        case "organizers" -> List.of("Naziv","Email");
        case "events" -> List.of("Naziv","Termin","Lokacija","Status");
        case "venues" -> List.of("Naziv","Adresa","Grad","Kapacitet");
        case "categories" -> List.of("Naziv","Opis");
        case "types" -> List.of("Naziv","Događaj","Cena (RSD)","Preostalo / ukupno");
        case "users" -> List.of("Ime i prezime","Email","Uloga");
        case "orders" -> List.of("Kupac","Događaj","Ukupno (RSD)","Status");
        case "tickets" -> List.of("Kod","Porudžbina","Tip ulaznice","Status");
        default -> throw new NotFoundException(); }; }
    private String label(String status) { return switch(status) {
        case "PAID" -> "Plaćeno"; case "CANCELLED" -> "Otkazano"; case "PUBLISHED" -> "U prodaji";
        case "ACTIVE" -> "Važeća"; case "USED" -> "Iskorišćena"; case "ADMIN" -> "Administrator"; default -> "Posetilac"; }; }
    public List<Row> rows(String key) { return switch(key) {
        case "organizers" -> organizers.findAllByOrderByNameAscIdAsc().stream().map(o -> new Row(o.getId(),List.of(o.getName(),o.getEmail()))).toList();
        case "events" -> events.findAllByOrderByStartsAtAsc().stream().map(e -> new Row(e.getId(),List.of(e.getTitle(),e.getStartsAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy. HH:mm")),e.getVenue().getName(),label(e.getStatus())))).toList();
        case "venues" -> venues.findAll().stream().map(v -> new Row(v.getId(),List.of(v.getName(),v.getAddress(),v.getCity(),""+v.getCapacity()))).toList();
        case "categories" -> categories.findAll().stream().map(c -> new Row(c.getId(),List.of(c.getName(),c.getDescription()))).toList();
        case "types" -> types.findAll().stream().map(t -> new Row(t.getId(),List.of(t.getName(),t.getEvent().getTitle(),t.getPrice().toPlainString(),booking.remaining(t)+" / "+t.getQuantity()))).toList();
        case "users" -> users.findAll().stream().map(u -> new Row(u.getId(),List.of(u.getName(),u.getEmail(),label(u.getRole())))).toList();
        case "orders" -> orders.findAll().stream().sorted(Comparator.comparing(PurchaseOrder::getCreatedAt).reversed()).map(o -> new Row(o.getId(),List.of(o.getUser().getEmail(),o.getEvent().getTitle(),o.getTotal().toPlainString(),label(o.getStatus())))).toList();
        case "tickets" -> tickets.findAll().stream().map(t -> new Row(t.getId(),List.of(t.getCode(),"#"+t.getOrder().getId(),t.getTicketType().getName(),label(t.getStatus())))).toList();
        default -> throw new NotFoundException(); }; }

    private Field field(String name,String label,String type,Object value) { return new Field(name,label,type,value==null?"":value.toString(),List.of(),true,""); }
    private Field select(String name,String label,Object value,List<Option> options) { return new Field(name,label,"select",value==null?"":value.toString(),options,true,""); }
    private List<Option> statuses(String... values) { return Arrays.stream(values).map(s -> new Option(s,label(s))).toList(); }
    private List<Option> eventOptions() { return events.findAll().stream().map(e -> new Option(""+e.getId(),e.getTitle())).toList(); }
    private List<Option> typeOptions() { return types.findAll().stream().map(t -> new Option(""+t.getId(),t.getEvent().getTitle()+" · "+t.getName())).toList(); }
    public List<Field> fields(String key, Long id) {
        boolean edit=id!=null;
        return switch(key) {
            case "organizers" -> { Organizer o=edit?organizers.findById(id).orElseThrow(NotFoundException::new):new Organizer();
                yield List.of(field("name","Naziv","text",o.getName()),field("email","Email","email",o.getEmail())); }
            case "users" -> { AppUser u=edit?users.findById(id).orElseThrow(NotFoundException::new):new AppUser();
                yield List.of(field("name","Ime i prezime","text",u.getName()),field("email","Email","email",u.getEmail()),
                    new Field("password","Lozinka","password","",List.of(),!edit,edit?"Ostavite prazno da zadržite postojeću lozinku.":"Od 8 do 60 znakova."),select("role","Uloga",u.getRole(),statuses("USER","ADMIN"))); }
            case "venues" -> { Venue v=edit?venues.findById(id).orElseThrow(NotFoundException::new):new Venue();
                yield List.of(field("name","Naziv","text",v.getName()),field("address","Adresa","text",v.getAddress()),field("city","Grad","text",v.getCity()),field("capacity","Kapacitet","number",edit?v.getCapacity():100)); }
            case "categories" -> { Category c=edit?categories.findById(id).orElseThrow(NotFoundException::new):new Category();
                yield List.of(field("name","Naziv","text",c.getName()),field("description","Opis","textarea",c.getDescription())); }
            case "events" -> { Event e=edit?events.findById(id).orElseThrow(NotFoundException::new):new Event();
                yield List.of(field("title","Naziv događaja","text",e.getTitle()),field("description","Opis","textarea",e.getDescription()),field("startsAt","Datum i vreme (Beograd)","datetime-local",e.getStartsAt()),
                    select("venueId","Lokacija",edit?e.getVenue().getId():null,venues.findAll().stream().map(v -> new Option(""+v.getId(),v.getName()+" · "+v.getCity())).toList()),
                    select("categoryId","Kategorija",edit?e.getCategory().getId():null,categories.findAll().stream().map(c -> new Option(""+c.getId(),c.getName())).toList()),
                    select("theme","Vizuelni stil",e.getTheme(),List.of(new Option("violet","Ljubičasti zvučni talasi"),new Option("orange","Narandžasta scena"),new Option("blue","Plava orbita"),new Option("green","Zelena perspektiva"))),
                    select("status","Status",e.getStatus(),statuses("PUBLISHED","CANCELLED"))); }
            case "types" -> { TicketType t=edit?types.findById(id).orElseThrow(NotFoundException::new):new TicketType();
                yield List.of(field("name","Naziv tipa","text",t.getName()),select("eventId","Događaj",edit?t.getEvent().getId():null,eventOptions()),field("price","Cena (RSD)","decimal",t.getPrice()),field("quantity","Ukupna količina","number",edit?t.getQuantity():50)); }
            case "orders" -> { if (edit) { PurchaseOrder o=orders.findById(id).orElseThrow(NotFoundException::new);
                    yield List.of(select("status","Status porudžbine",o.getStatus(),statuses("PAID","CANCELLED"))); }
                yield List.of(select("userId","Kupac",null,users.findAll().stream().map(u -> new Option(""+u.getId(),u.getEmail())).toList()),select("typeId","Tip ulaznice",null,typeOptions()),field("quantity","Broj ulaznica (1–10)","number",1)); }
            case "tickets" -> { if (edit) { Ticket t=tickets.findById(id).orElseThrow(NotFoundException::new); yield List.of(select("status","Status ulaznice",t.getStatus(),statuses("ACTIVE","USED","CANCELLED"))); }
                yield List.of(select("orderId","Porudžbina",null,orders.findAll().stream().filter(o -> o.getStatus().equals("PAID")).map(o -> new Option(""+o.getId(),"#"+o.getId()+" · "+o.getUser().getEmail()+" · "+o.getEvent().getTitle())).toList()),select("typeId","Tip ulaznice",null,typeOptions())); }
            default -> throw new NotFoundException();
        };
    }

    private String text(Map<String,String> data,String key,int max) {
        String value=data.getOrDefault(key,"").trim();
        if (value.isEmpty() || value.length()>max) throw new BusinessException("Popunite sva polja i poštujte dozvoljenu dužinu ("+key+", najviše "+max+").");
        return value;
    }
    private long id(Map<String,String> data,String key) {
        try { long value=Long.parseLong(data.getOrDefault(key,"")); if(value<1) throw new NumberFormatException(); return value; }
        catch(NumberFormatException e) { throw new BusinessException("Izaberite ispravnu vrednost za polje: "+key+"."); }
    }
    private int positive(Map<String,String> data,String key) { long n=id(data,key); if(n>1_000_000) throw new BusinessException("Količina ili kapacitet ne smeju preći 1.000.000."); return (int)n; }
    private String choice(Map<String,String> data,String key,String... values) { String value=data.get(key); if(!Arrays.asList(values).contains(value)) throw new BusinessException("Nepoznata vrednost polja: "+key); return value; }
    private void lockVenue(Venue venue) { em.lock(venue,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE); em.refresh(venue); }
    private long allocated(Long eventId) { return types.findByEventIdOrderByPriceAsc(eventId).stream().mapToLong(TicketType::getQuantity).sum(); }

    @Transactional
    public void save(String key,Long recordId,Map<String,String> data,String currentEmail) {
        save(key, recordId, data, currentEmail, null);
    }

    @Transactional
    public void save(String key,Long recordId,Map<String,String> data,String currentEmail,List<Long> organizerIds) {
        boolean edit=recordId!=null;
        switch(key) {
            case "organizers" -> {
                Organizer organizer=edit?organizers.findById(recordId).orElseThrow(NotFoundException::new):new Organizer();
                String email=text(data,"email",180).toLowerCase(Locale.ROOT);
                if(!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw new BusinessException("Email adresa nije ispravna.");
                organizer.setName(text(data,"name",100)); organizer.setEmail(email); organizers.save(organizer);
            }
            case "users" -> {
                // Lock all administrator rows in stable order to preserve the last-admin invariant.
                users.findAll().stream().filter(u -> u.getRole().equals("ADMIN")).sorted(Comparator.comparing(AppUser::getId)).forEach(u -> em.lock(u,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE));
                AppUser u=edit?users.findById(recordId).orElseThrow(NotFoundException::new):new AppUser();
                String email=text(data,"email",180).toLowerCase(Locale.ROOT);
                if(!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw new BusinessException("Email adresa nije ispravna.");
                String role=choice(data,"role","USER","ADMIN");
                if(edit && u.getEmail().equals(currentEmail) && (!u.getEmail().equals(email)||!role.equals("ADMIN"))) throw new BusinessException("Sopstvenu email adresu i administratorsku ulogu ne možete menjati ovde.");
                if(edit && u.getRole().equals("ADMIN") && !role.equals("ADMIN") && users.countByRole("ADMIN")<=1) throw new BusinessException("Sistem mora imati administratora.");
                if(users.findByEmail(email).filter(other -> !other.getId().equals(recordId)).isPresent()) throw new BusinessException("Email adresa je već zauzeta.");
                String password=data.getOrDefault("password","");
                if(!edit || !password.isBlank()) {
                    if(password.length()<8 || password.length()>60 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72) throw new BusinessException("Lozinka mora imati 8–60 znakova i najviše 72 UTF-8 bajta.");
                    u.setPassword(encoder.encode(password));
                }
                u.setName(text(data,"name",100)); u.setEmail(email); u.setRole(role); users.save(u);
            }
            case "venues" -> {
                Venue v=edit?venues.findById(recordId).orElseThrow(NotFoundException::new):new Venue();
                if(edit) lockVenue(v);
                int capacity=positive(data,"capacity");
                if(edit && events.findAll().stream().filter(e -> e.getVenue().getId().equals(recordId)).anyMatch(e -> allocated(e.getId())>capacity)) throw new BusinessException("Kapacitet je manji od već raspoređenih ulaznica za događaje na ovoj lokaciji.");
                v.setName(text(data,"name",100)); v.setAddress(text(data,"address",200)); v.setCity(text(data,"city",100)); v.setCapacity(capacity); venues.save(v);
            }
            case "categories" -> {
                Category c=edit?categories.findById(recordId).orElseThrow(NotFoundException::new):new Category();
                String name=text(data,"name",100);
                if(categories.findAll().stream().anyMatch(other -> !other.getId().equals(recordId)&&other.getName().equalsIgnoreCase(name))) throw new BusinessException("Kategorija sa tim nazivom već postoji.");
                c.setName(name); c.setDescription(text(data,"description",500)); categories.save(c);
            }
            case "events" -> {
                Event e=edit?booking.lockEvent(recordId):new Event();
                Venue venue=venues.findById(id(data,"venueId")).orElseThrow(NotFoundException::new); lockVenue(venue);
                if(edit && allocated(recordId)>venue.getCapacity()) throw new BusinessException("Lokacija nema dovoljno kapaciteta za sve tipove ulaznica.");
                LocalDateTime starts;
                try { starts=LocalDateTime.parse(text(data,"startsAt",30)); } catch(java.time.format.DateTimeParseException ex) { throw new BusinessException("Unesite ispravan datum i vreme."); }
                if(starts.getYear()<2000 || starts.getYear()>2100) throw new BusinessException("Godina mora biti između 2000 i 2100.");
                String status=choice(data,"status","PUBLISHED","CANCELLED");
                if(edit && e.getStatus().equals("CANCELLED") && status.equals("PUBLISHED")) throw new BusinessException("Otkazan događaj se ne objavljuje ponovo. Napravite novi događaj.");
                e.setTitle(text(data,"title",150)); e.setDescription(text(data,"description",4000)); e.setStartsAt(starts); e.setVenue(venue);
                e.setCategory(categories.findById(id(data,"categoryId")).orElseThrow(NotFoundException::new));
                e.setTheme(choice(data,"theme","violet","orange","blue","green"));
                if(edit && status.equals("CANCELLED")) booking.cancelEvent(e); else e.setStatus(status);
                if (organizerIds != null) {
                    Set<Long> selected = new LinkedHashSet<>(organizerIds);
                    List<Organizer> found = organizers.findAllById(selected);
                    if (found.size() != selected.size()) throw new BusinessException("Izabrani organizator više ne postoji.");
                    e.getOrganizers().clear();
                    e.getOrganizers().addAll(found);
                }
                events.save(e);
            }
            case "types" -> {
                TicketType t=edit?types.findById(recordId).orElseThrow(NotFoundException::new):new TicketType();
                Long eventId=id(data,"eventId");
                if(edit && !t.getEvent().getId().equals(eventId)) throw new BusinessException("Postojeći tip ulaznice ne može se premestiti na drugi događaj.");
                Event event=booking.lockEvent(eventId); lockVenue(event.getVenue());
                if(edit) em.refresh(t);
                int quantity=positive(data,"quantity");
                if(edit && quantity<tickets.countByTicketTypeIdAndStatusNot(recordId,"CANCELLED")) throw new BusinessException("Količina je manja od broja prodatih ulaznica.");
                if(allocated(eventId)-(edit?t.getQuantity():0)+quantity>event.getVenue().getCapacity()) throw new BusinessException("Zbir količina svih tipova prelazi kapacitet lokacije.");
                BigDecimal price;
                try { price=new BigDecimal(data.getOrDefault("price","")).setScale(2,java.math.RoundingMode.UNNECESSARY); }
                catch(IllegalArgumentException|ArithmeticException ex) { throw new BusinessException("Cena mora biti broj sa najviše dve decimale."); }
                if(price.signum()<0 || price.compareTo(new BigDecimal("10000000"))>0) throw new BusinessException("Cena mora biti između 0 i 10.000.000 RSD.");
                t.setName(text(data,"name",100)); t.setEvent(event); t.setPrice(price); t.setQuantity(quantity); types.save(t);
            }
            case "orders" -> {
                if(!edit) booking.purchase(users.findById(id(data,"userId")).orElseThrow(NotFoundException::new).getEmail(),id(data,"typeId"),positive(data,"quantity"));
                else {
                    PurchaseOrder o=orders.findById(recordId).orElseThrow(NotFoundException::new);
                    String status=choice(data,"status","PAID","CANCELLED");
                    if(status.equals("CANCELLED")) booking.cancelOrder(recordId,currentEmail,true);
                    else if(!o.getStatus().equals("PAID")) throw new BusinessException("Otkazana porudžbina se ne može ponovo aktivirati.");
                }
            }
            case "tickets" -> {
                if(edit) booking.changeTicketStatus(recordId,choice(data,"status","ACTIVE","USED","CANCELLED"));
                else booking.addTicket(id(data,"orderId"),id(data,"typeId"));
            }
            default -> throw new NotFoundException();
        }
    }

    @Transactional
    public void delete(String key,Long id,String currentEmail) {
        switch(key) {
            case "organizers" -> {
                if(events.existsByOrganizersId(id)) throw new BusinessException("Organizator je povezan sa događajima. Prvo uklonite te veze.");
                organizers.deleteById(id);
            }
            case "users" -> { AppUser u=users.findById(id).orElseThrow(NotFoundException::new);
                if(u.getEmail().equals(currentEmail)) throw new BusinessException("Ne možete obrisati sopstveni nalog.");
                if(u.getRole().equals("ADMIN") && users.countByRole("ADMIN")<=1) throw new BusinessException("Poslednji administrator ne može biti obrisan.");
                if(orders.existsByUserId(id)) throw new BusinessException("Korisnik ima porudžbine. Prvo uklonite povezane porudžbine."); users.delete(u); }
            case "venues" -> { if(events.existsByVenueId(id)) throw new BusinessException("Lokacija je povezana sa događajima."); venues.deleteById(id); }
            case "categories" -> { if(events.existsByCategoryId(id)) throw new BusinessException("Kategorija je povezana sa događajima."); categories.deleteById(id); }
            case "events" -> { booking.lockEvent(id); if(types.existsByEventId(id)||orders.existsByEventId(id)) throw new BusinessException("Događaj ima tipove ulaznica ili porudžbine. Možete ga otkazati ili prvo ukloniti povezane zapise."); events.deleteById(id); }
            case "types" -> { TicketType t=types.findById(id).orElseThrow(NotFoundException::new); booking.lockEvent(t.getEvent().getId()); if(tickets.existsByTicketTypeId(id)) throw new BusinessException("Tip ima izdate ulaznice i ne može biti obrisan."); types.delete(t); }
            case "orders" -> booking.deleteOrder(id);
            case "tickets" -> booking.deleteTicket(id);
            default -> throw new NotFoundException();
        }
    }
}

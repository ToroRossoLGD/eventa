package rs.singidunum.eventa.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import rs.singidunum.eventa.domain.*;
import rs.singidunum.eventa.repository.*;
import java.math.BigDecimal;
import java.time.*;

@Component @RequiredArgsConstructor @ConditionalOnProperty(name="app.seed",havingValue="true")
public class DemoData implements CommandLineRunner {
    private final AppUserRepository users;
    private final VenueRepository venues;
    private final CategoryRepository categories;
    private final EventRepository events;
    private final TicketTypeRepository types;
    private final PasswordEncoder encoder;
    private final Clock clock;
    @Override @Transactional
    public void run(String... args) {
        if(users.count()>0) return;
        user("Administrator","admin@eventa.rs","Admin123!","ADMIN");
        user("Ana Petrović","ana@eventa.rs","Posetilac123!","USER");
        Category music=category("Muzika","Koncerti, DJ nastupi i zvuk koji ostaje sa vama.");
        Category tech=category("Konferencije","Nove ideje, tehnologija i ljudi koji pomeraju granice.");
        Category art=category("Kultura","Pozorište, umetnost i priče iz drugog ugla.");
        Category festival=category("Festivali","Više izvođača, više doživljaja, jedan događaj.");
        Venue hangar=venue("Hangar Luke Beograd","Žorža Klemansoa 37","Beograd",1200);
        Venue mts=venue("MTS Dvorana","Dečanska 14","Beograd",800);
        Venue spens=venue("SPENS","Sutjeska 2","Novi Sad",2000);
        Venue garden=venue("Botanička bašta","Takovska 43","Beograd",400);
        Venue nis=venue("Niška tvrđava","Đuke Dinić bb","Niš",1500);
        event("Noć elektronskog zvuka","Jedna noć. Hiljadu frekvencija. Prepustite se pažljivo odabranim elektronskim setovima, svetlosnim instalacijama i atmosferi starog industrijskog Beograda. Vrata se otvaraju dva sata pre početka. Ulaznica važi za jedan ulazak.",music,hangar,7,21,"violet",1800,650);
        event("Future / Tech 2026","Mesto susreta razvoja softvera, novih ideja i praktičnog znanja. Očekuju vas predavanja, razgovori sa inženjerima i prostor za upoznavanje zajednice. Program obuhvata softverske arhitekture, razvoj proizvoda i iskustva iz prakse.",tech,mts,12,10,"blue",3500,500);
        event("Jazz pod zvezdama","Veče akustičnih tonova u zelenom srcu grada. Domaći džez sastavi donose poznate standarde i originalne kompozicije u intimnom ambijentu botaničke bašte. Broj mesta je ograničen.",music,garden,16,20,"green",2200,250);
        event("Grad na sceni","Savremena predstava o ljudima, susretima i gradu koji se menja. Otkrijte priču koja povezuje humor, muziku i svakodnevne male pobede. Predstava traje približno 100 minuta.",art,mts,20,19,"orange",1200,550);
        event("Na otvorenom","Celodnevni festival muzike, kreativnosti i dobrog društva. Dve bine, lokalni umetnici i pažljivo biran program za kraj leta. Ulaznica obuhvata pristup svim festivalskim zonama.",festival,nis,25,16,"violet",2900,1000);
        event("Design & friends","Razgovori i radionice za sve koji vole dizajn. Upoznajte autore, istražite nove pristupe i ponesite svežu inspiraciju. Program je namenjen studentima, profesionalcima i radoznalim početnicima.",tech,spens,30,11,"orange",1500,800);
    }
    private void user(String name,String email,String password,String role) { AppUser u=new AppUser();u.setName(name);u.setEmail(email);u.setPassword(encoder.encode(password));u.setRole(role);users.save(u); }
    private Category category(String name,String description) { Category c=new Category();c.setName(name);c.setDescription(description);return categories.save(c); }
    private Venue venue(String name,String address,String city,int capacity) { Venue v=new Venue();v.setName(name);v.setAddress(address);v.setCity(city);v.setCapacity(capacity);return venues.save(v); }
    private void event(String title,String description,Category category,Venue venue,int days,int hour,String theme,int price,int quantity) {
        Event e=new Event();e.setTitle(title);e.setDescription(description);e.setCategory(category);e.setVenue(venue);e.setStartsAt(LocalDate.now(clock).plusDays(days).atTime(hour,0));e.setTheme(theme);e.setStatus("PUBLISHED");events.save(e);
        type(e,"Standard",price,quantity);type(e,"VIP",price*2,Math.min(100,venue.getCapacity()-quantity));
    }
    private void type(Event e,String name,int price,int quantity) { TicketType t=new TicketType();t.setEvent(e);t.setName(name);t.setPrice(BigDecimal.valueOf(price));t.setQuantity(quantity);types.save(t); }
}

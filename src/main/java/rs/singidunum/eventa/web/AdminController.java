package rs.singidunum.eventa.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.dao.DataIntegrityViolationException;
import rs.singidunum.eventa.service.*;
import rs.singidunum.eventa.repository.*;
import java.security.Principal;
import java.math.BigDecimal;
import java.util.*;

@Controller @RequestMapping("/admin") @RequiredArgsConstructor
public class AdminController {
    private final AdminService admin;
    private final BookingService booking;
    private final EventRepository events;
    private final PurchaseOrderRepository orders;
    private final TicketRepository tickets;
    private final AppUserRepository users;

    @ModelAttribute("resources") Object resources() { return admin.resources(); }
    @GetMapping({"", "/"}) String dashboard(Model model) {
        model.addAttribute("eventCount",events.count()); model.addAttribute("userCount",users.count());
        model.addAttribute("ticketCount",tickets.findAll().stream().filter(t -> !t.getStatus().equals("CANCELLED")).count());
        model.addAttribute("revenue",tickets.findAll().stream().filter(t -> !t.getStatus().equals("CANCELLED") && t.getOrder().getStatus().equals("PAID")).map(t -> t.getUnitPrice()).reduce(BigDecimal.ZERO,BigDecimal::add));
        model.addAttribute("recent",orders.findAll().stream().sorted(Comparator.comparing((rs.singidunum.eventa.domain.PurchaseOrder o) -> o.getCreatedAt()).reversed()).limit(6).toList());
        return "admin/dashboard";
    }
    @GetMapping("/check-in") String checkIn() { return "admin/check-in"; }
    @PostMapping("/check-in") String checkIn(@RequestParam String code,RedirectAttributes flash) {
        try { var ticket=booking.checkIn(code); flash.addFlashAttribute("success","Ulaz odobren · "+ticket.getOrder().getUser().getName()+" · "+ticket.getTicketType().getEvent().getTitle()+" · "+ticket.getTicketType().getName()); }
        catch(BusinessException e) { flash.addFlashAttribute("error",e.getMessage()); }
        return "redirect:/admin/check-in";
    }
    @GetMapping("/{key}") String list(@PathVariable String key,Model model) {
        model.addAttribute("resource",admin.resource(key)); model.addAttribute("headers",admin.headers(key)); model.addAttribute("rows",admin.rows(key)); return "admin/list";
    }
    private String form(String key,Long id,Model model) {
        model.addAttribute("resource",admin.resource(key)); model.addAttribute("recordId",id); model.addAttribute("fields",admin.fields(key,id)); return "admin/form";
    }
    @GetMapping("/{key}/new") String create(@PathVariable String key,Model model) { return form(key,null,model); }
    @GetMapping("/{key}/{id}/edit") String edit(@PathVariable String key,@PathVariable Long id,Model model) { return form(key,id,model); }
    @PostMapping("/{key}/save") String save(@PathVariable String key,@RequestParam(required=false) Long recordId,@RequestParam Map<String,String> data,Principal principal,Model model,RedirectAttributes flash) {
        try { admin.save(key,recordId,data,principal.getName()); }
        catch(BusinessException|DataIntegrityViolationException e) {
            form(key,recordId,model);
            model.addAttribute("fields",admin.fields(key,recordId).stream().map(f -> new AdminService.Field(f.name(),f.label(),f.type(),f.type().equals("password")?"":data.getOrDefault(f.name(),f.value()),f.options(),f.required(),f.hint())).toList());
            model.addAttribute("error",e instanceof BusinessException?e.getMessage():"Podaci su već zauzeti ili povezani sa drugim zapisima.");
            return "admin/form";
        }
        flash.addFlashAttribute("success","Promene su sačuvane."); return "redirect:/admin/"+key;
    }
    @PostMapping("/{key}/{id}/delete") String delete(@PathVariable String key,@PathVariable Long id,Principal principal,RedirectAttributes flash) {
        try { admin.delete(key,id,principal.getName()); flash.addFlashAttribute("success","Zapis je obrisan."); }
        catch(BusinessException|DataIntegrityViolationException e) { flash.addFlashAttribute("error",e instanceof BusinessException?e.getMessage():"Zapis je povezan sa drugim podacima i ne može biti obrisan."); }
        return "redirect:/admin/"+key;
    }
}

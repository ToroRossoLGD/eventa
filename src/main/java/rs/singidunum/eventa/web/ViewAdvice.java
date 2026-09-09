package rs.singidunum.eventa.web;

import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import java.util.Map;

@ControllerAdvice
public class ViewAdvice {
    @ModelAttribute("statusLabels")
    public Map<String,String> statusLabels() { return Map.of("PUBLISHED","U prodaji","CANCELLED","Otkazano","PAID","Plaćeno","ACTIVE","Važeća","USED","Iskorišćena","USER","Posetilac","ADMIN","Administrator"); }
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String badRequest(Model model) { model.addAttribute("message","Zahtev sadrži neispravne ili nepotpune podatke."); return "error"; }
}

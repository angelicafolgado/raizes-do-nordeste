package br.com.raizesdonordeste.api;

import br.com.raizesdonordeste.application.RaizesService;
import br.com.raizesdonordeste.domain.ApiException;
import br.com.raizesdonordeste.domain.Identity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMap;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final RaizesService service;
    public ApiController(RaizesService service) { this.service = service; }
    private static Identity identity(HttpServletRequest request) {
        Object principal = request.getAttribute("identity");
        if (principal instanceof Identity identity) return identity;
        throw new ApiException("NAO_AUTENTICADO", "Autenticação obrigatória.", 401);
    }

    @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestBody(required = false) Map<String, Object> body) {
        return service.login(body);
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return service.health();
    }

    @GetMapping("/unidades")
    public Map<String, Object> units(@RequestParam MultiValueMap<String, String> query) {
        return service.units(query);
    }

    @GetMapping("/produtos")
    public Map<String, Object> products(@RequestParam MultiValueMap<String, String> query) {
        return service.products(query);
    }

    @PostMapping("/pedidos")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createOrder(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        return service.createOrder(body, identity(request));
    }

    @GetMapping("/pedidos")
    @SecurityRequirement(name = "bearerAuth")
    public Map<String, Object> orders(@RequestParam MultiValueMap<String, String> query, HttpServletRequest request) {
        return service.orders(query, identity(request));
    }

    @PostMapping("/pedidos/{id}/pagamento")
    @SecurityRequirement(name = "bearerAuth")
    public Map<String, Object> pay(@PathVariable("id") String id, @RequestBody(required = false) Map<String, Object> body,
                                   HttpServletRequest request) {
        return service.pay(id, body, identity(request));
    }

    @PatchMapping("/pedidos/{id}/status")
    @SecurityRequirement(name = "bearerAuth")
    public Map<String, Object> changeStatus(@PathVariable("id") String id, @RequestBody(required = false) Map<String, Object> body,
                                            HttpServletRequest request) {
        return service.changeStatus(id, body, identity(request));
    }

    @GetMapping("/privacidade/aviso")
    public Map<String, Object> privacyNotice() {
        return service.privacyNotice();
    }

    @PutMapping("/privacidade/consentimentos/fidelidade")
    @SecurityRequirement(name = "bearerAuth")
    public Map<String, Object> updateConsent(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        return service.updateConsent(body, identity(request));
    }

    @GetMapping("/privacidade/consentimentos")
    @SecurityRequirement(name = "bearerAuth")
    public Map<String, Object> readConsents(@RequestParam MultiValueMap<String, String> query, HttpServletRequest request) {
        return service.readConsents(query, identity(request));
    }

    @GetMapping("/auditoria")
    @SecurityRequirement(name = "bearerAuth")
    public Map<String, Object> audit(@RequestParam MultiValueMap<String, String> query, HttpServletRequest request) {
        return service.audit(query, identity(request));
    }
}

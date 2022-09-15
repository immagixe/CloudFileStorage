package ru.immagixe.CloudFileStorage;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import ru.immagixe.CloudFileStorage.security.securityDetails.PersonDetails;

@Controller
public class HelloController {

    @GetMapping("/hello")
    public String hello(Model model) {
        model.addAttribute("player1", new Object());
        model.addAttribute("test2", "TEST!!!");
        return "hello";
    }

    @GetMapping("/showUserInfo")
    public String showUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        PersonDetails personDetails = (PersonDetails) authentication.getPrincipal();
        return "hello";
    }

    @GetMapping("admin")
    public String adminPage() {
        return "admin";
    }
}

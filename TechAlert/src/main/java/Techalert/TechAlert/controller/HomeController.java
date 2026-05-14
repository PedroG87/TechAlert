package Techalert.TechAlert.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping("/home/")

public class HomeController {

    @GetMapping
    public String Home(){
        return "Home";

    }
    @GetMapping("login.html")
    public String Login() {
        return "Login";
    }

}


package com.c7smile.clinic;

import com.c7smile.clinic.service.ClinicService;
import com.c7smile.clinic.service.DentistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class HomeController {

    @Autowired
    private ClinicService clinicService;

    @Autowired
    private DentistService dentistService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("services", clinicService.getTopServices());
        return "index";
    }

    @GetMapping("/services")
    public String services(Model model) {
        model.addAttribute("services", clinicService.getAllServices());
        return "services";
    }

    @GetMapping("/dich-vu/{slug}")
    public String serviceDetail(@PathVariable String slug, Model model) {
        com.c7smile.clinic.entity.Service service = clinicService.getServiceBySlug(slug).orElse(null);
        if (service == null) {
            try {
                service = clinicService.getServiceById(Long.parseLong(slug)).orElse(null);
            } catch (Exception e) { /* ignore */ }
        }
        model.addAttribute("service", service);
        return "service/detail";
    }

    @GetMapping("/bac-si/{slug}")
    public String doctorDetail(@PathVariable String slug, Model model) {
        com.c7smile.clinic.entity.Dentist dentist = dentistService.getDentistBySlug(slug).orElse(null);
        if (dentist == null) {
            try {
                dentist = dentistService.getDentistById(Long.parseLong(slug)).orElse(null);
            } catch (Exception e) { /* ignore */ }
        }
        model.addAttribute("dentist", dentist);
        return "doctor/detail";
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("dentists", dentistService.getAllDentists());
        return "about";
    }

    @GetMapping("/contact")
    public String contact() {
        return "contact";
    }

    @GetMapping("/news")
    public String news() {
        return "news";
    }
}

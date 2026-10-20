package ru.kata.spring.boot_security.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ru.kata.spring.boot_security.demo.service.RoleService;
import ru.kata.spring.boot_security.demo.service.UserService;


@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final RoleService roleService;

    public AdminController(UserService userService, RoleService roleService) {
        this.userService = userService;
        this.roleService = roleService;
    }


    @GetMapping({"", "/"})
    public String adminPage() {
        return "index";
    }

    @GetMapping("/edit")
    public ModelAndView editUser(@RequestParam("id") Long id) {
        return userService.findUserById(id)
                .map(user -> {
                    user.setPassword(null);
                    ModelAndView mav = new ModelAndView("new");
                    mav.addObject("user", user);
                    mav.addObject("allRoles", roleService.getAllRoles());
                    return mav;
                })
                .orElseGet(() -> {
                    ModelAndView mav = new ModelAndView("error/404");
                    mav.setStatus(HttpStatus.NOT_FOUND);
                    mav.addObject("message", "User not found: id=" + id);
                    return mav;
                });
    }
}

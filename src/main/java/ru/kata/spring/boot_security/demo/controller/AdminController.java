package ru.kata.spring.boot_security.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import ru.kata.spring.boot_security.demo.model.Role;
import ru.kata.spring.boot_security.demo.model.User;
import ru.kata.spring.boot_security.demo.service.RoleService;
import ru.kata.spring.boot_security.demo.service.UserService;

import java.util.HashSet;
import java.util.Set;


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
    public ModelAndView listUsers(Model model) {
        ModelAndView mav = new ModelAndView("index");
        mav.addObject("users", userService.getAllUsers());
        mav.addObject("allRoles", roleService.getAllRoles());
        return mav;
    }

    @GetMapping("/new")
    public ModelAndView newUser(Model model) {
        ModelAndView mav = new ModelAndView("new");
        mav.addObject("user", new User());
        mav.addObject("allRoles", roleService.getAllRoles());
        return mav;
    }

    @PostMapping("/create")
    public ModelAndView createUser(@ModelAttribute("user") User user) {
        userService.saveUser(user);
        return new ModelAndView("redirect:/admin/");
    }

    @GetMapping("/edit")
    public ModelAndView editUser(@RequestParam("id") Long id) {
        return userService.getUserById(id)
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

    @PostMapping("/update")
    public ModelAndView updateUser(@ModelAttribute("user") User user) {
        userService.updateUser(user);
        return new ModelAndView("redirect:/admin/");
    }

    @PostMapping("/delete")
    public ModelAndView deleteUser(@RequestParam("id") Long id) {
        userService.deleteUser(id);
        return new ModelAndView("redirect:/admin/");
    }

}

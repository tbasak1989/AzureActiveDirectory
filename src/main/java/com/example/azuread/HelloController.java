package com.example.azuread;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
public class HelloController {

	   @PreAuthorize("hasRole('ROLE_Users')")
	   @RequestMapping("/")
	   public String helloWorld() {
	      return "Hello Users!";
	   }
	   @PreAuthorize("hasRole('ROLE_group1')")
	   @RequestMapping("/Group1")
	   public String groupOne() {
	      return "Hello Group 1 Users!";
	   }
	   @PreAuthorize("hasRole('ROLE_group2')")
	   @RequestMapping("/Group2")
	   public String groupTwo() {
	      return "Hello Group 2 Users!";
	   }
}
/**
Copyright (c) 2024 Sami Menik, PhD. All rights reserved.

This is a project developed by Dr. Menik to give the students an opportunity to apply database concepts learned in the class in a real world project. Permission is granted to host a running version of this software and to use images or videos of this work solely for the purpose of demonstrating the work to potential employers. Any form of reproduction, distribution, or transmission of the software's source code, in part or whole, without the prior written consent of the copyright owner, is strictly prohibited.
*/
package uga.menik.csx370.controllers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import uga.menik.csx370.models.FollowableUser;
import uga.menik.csx370.services.PeopleService;
import uga.menik.csx370.services.UserService;

/**
 * Handles /people URL and its sub URL paths.
 */
@Controller
@RequestMapping("/people")
public class PeopleController {
    private final UserService userService;
    private final PeopleService peopleService;

    @Autowired
    public PeopleController(PeopleService personService, UserService userService) {
        this.userService = userService;
        this.peopleService = personService;
    }
    // Inject UserService and PeopleService instances.
    // See LoginController.java to see how to do this.
    // Hint: Add a constructor with @Autowired annotation.

    /**
     * Serves the /people web page.
     * 
     * Note that this accepts a URL parameter called error.
     * The value to this parameter can be shown to the user as an error message.
     * See notes in HashtagSearchController.java regarding URL parameters.
     */
    @GetMapping
    public ModelAndView webpage(@RequestParam(name = "error", required = false) String error) {
        ModelAndView mv = new ModelAndView("people_page");
        
        // The list of followable users
        List<FollowableUser> followableUsers = new ArrayList<>();

        String errorMessage = error;

        try {
            // Getting logged in users id, then calling service to get list of users.
            String loggedInUserId = userService.getLoggedInUser().getUserId();
            followableUsers = peopleService.getFollowableUsers(loggedInUserId);
        } catch (SQLException e) {
            System.out.println("Failed to load users: " + e.getMessage());
            errorMessage = "Failed to load users. Please try again.";
        }

        mv.addObject("users", followableUsers);
        mv.addObject("errorMessage", errorMessage);
        mv.addObject("isNoContent", followableUsers.isEmpty());
        
        return mv;
    }

    /**
     * This function handles user follow and unfollow.
     * Note the URL has parameters defined as variables ie: {userId} and {isFollow}.
     * Follow and unfollow is handled by submitting a get type form to this URL 
     * by specifing the userId and the isFollow variables.
     * Learn more here: https://www.w3schools.com/tags/att_form_method.asp
     * An example URL that is handled by this function looks like below:
     * http://localhost:8081/people/1/follow/false
     * The above URL assigns 1 to userId and false to isFollow.
     */
    @GetMapping("{userId}/follow/{isFollow}")
    public String followUnfollowUser(@PathVariable("userId") String userId,
            @PathVariable("isFollow") Boolean isFollow) {
        System.out.println("User is attempting to follow/unfollow a user:");
        System.out.println("\tuserId: " + userId);
        System.out.println("\tisFollow: " + isFollow);

        try {
            // Get logged in user's id.
            final String loggedInUserId = userService.getLoggedInUser().getUserId();

            // Call the appropriate service function.
            if (isFollow) peopleService.followUser(loggedInUserId, userId);
            else peopleService.unfollowUser(loggedInUserId, userId);

            // Redirect the user if follow/unfollow is a success.
            String message = URLEncoder.encode(
                isFollow ? "User followed" : "User unfollowed",
                StandardCharsets.UTF_8
            );
            return "redirect:/people?success=" + message;
        } catch (SQLException e) {
            // Redirect the user with an error message if there was an issue.
            String message = URLEncoder.encode("Failed to (un)follow the user. Please try again.", StandardCharsets.UTF_8);
            System.out.println("Error (un)following user: " + e.getMessage());
            return "redirect:/people?error=" + message;
        }
    }

}

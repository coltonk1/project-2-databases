/**
Copyright (c) 2024 Sami Menik, PhD. All rights reserved.

This is a project developed by Dr. Menik to give the students an opportunity to apply database concepts learned in the class in a real world project. Permission is granted to host a running version of this software and to use images or videos of this work solely for the purpose of demonstrating the work to potential employers. Any form of reproduction, distribution, or transmission of the software's source code, in part or whole, without the prior written consent of the copyright owner, is strictly prohibited.
*/
package uga.menik.csx370.controllers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import uga.menik.csx370.models.ExpandedPost;
import uga.menik.csx370.models.Post;
import uga.menik.csx370.utility.Utility;
import uga.menik.csx370.models.User;
import uga.menik.csx370.services.PostService;
import uga.menik.csx370.services.UserService;

/**
 * Handles /bookmarks and its sub URLs.
 * No other URLs at this point.
 * 
 * Learn more about @Controller here: 
 * https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller.html
 */
@Controller
@RequestMapping("/bookmarks")
public class BookmarksController {
    private final PostService postService;
    private final UserService userService;

    @Autowired
    public BookmarksController(PostService postService, UserService userService) {
        this.postService = postService;
        this.userService = userService;
    }

    /**
     * /bookmarks URL itself is handled by this.
     */
    @GetMapping
    public ModelAndView webpage() {
        // posts_page is a mustache template from src/main/resources/templates.
        // ModelAndView class enables initializing one and populating placeholders
        // in the template using Java objects assigned to named properties.
        ModelAndView mv = new ModelAndView("posts_page");
        
        // The list of posts to show on the page.
        List<Post> posts = new ArrayList<>();
        // Error message to show to the user if any.
        String errorMessage = null;

        try {
            // Get bookmarked posts for the logged in user.
            String loggedInUserId = userService.getLoggedInUser().getUserId();
            posts = postService.getBookmarkedPosts(loggedInUserId);
            // Set posts property to the list of posts.
            mv.addObject("posts", posts);
        } catch (Exception e) {
            // Set error message if there was an issue.
            errorMessage = "Failed to load bookmarked posts. Please try again.";
            System.out.println("Failed to load bookmarked posts: " + e.getMessage());
        }

        // If no posts, show no content message.
        mv.addObject("isNoContent", posts.isEmpty());
        // If error, show error message.    
        mv.addObject("errorMessage", errorMessage);

        return mv;
    }
    
}

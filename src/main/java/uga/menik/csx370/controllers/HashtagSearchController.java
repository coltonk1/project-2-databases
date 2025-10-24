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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import uga.menik.csx370.models.Post;
import uga.menik.csx370.services.PostService;
import uga.menik.csx370.services.UserService;
import uga.menik.csx370.utility.Utility;

import org.springframework.web.bind.annotation.RequestParam;

/**
 * Handles /hashtagsearch URL and possibly others.
 * At this point no other URLs.
 */
@Controller
@RequestMapping("/hashtagsearch")
public class HashtagSearchController {
    
    private final UserService userService;
    private final PostService postService;

    @Autowired
    public HashtagSearchController(UserService userService, PostService postService) {
        this.userService = userService;
        this.postService = postService;
    }

    /**
     * This function handles the /hashtagsearch URL itself.
     * This URL can process a request parameter with name hashtags.
     * In the browser the URL will look something like below:
     * http://localhost:8081/hashtagsearch?hashtags=%23amazing+%23fireworks
     * Note: the value of the hashtags is URL encoded.
     */
    @GetMapping()
    public ModelAndView webpage(@RequestParam(name = "hashtags") String hashtags) {
        System.out.println("User is searching: " + hashtags);

        ModelAndView mv = new ModelAndView("posts_page");
        List<Post> posts = new ArrayList<>();
        try {
            // Ensure at least one hashtag is provided when searching
            if (hashtags == null || hashtags.trim().isEmpty()) {
                mv.addObject("errorMessage", "Please enter at least one hashtag to search.");
                mv.addObject("isNoContent", true);
                return mv;
            }

            // Normalizes input, splits hashtags, and adds them to list that will be passed as input function
            String[] parts = hashtags.split("\\s+|,");
            List<String> tags = new ArrayList<>();
            for (String p : parts) {
                if (p == null) continue; 
                String t = p.trim();
                if (t.startsWith("#")) t = t.substring(1);
                t = t.toLowerCase();
                if (!t.isEmpty() && !tags.contains(t) && postService.isHashtagInTable(t)) tags.add(t);
            }

            // If user searched for empty hashtag --> "#"
            if (tags.isEmpty()) {
                mv.addObject("errorMessage", "No valid hashtags found in the search.");
                mv.addObject("posts", posts);
                mv.addObject("isNoContent", true);
                return mv;
            }
            
            String loggedInUserId = userService.getLoggedInUser().getUserId();
            posts = postService.getPostsByHashtags(tags.toArray(new String[0]), loggedInUserId);
            mv.addObject("posts", posts);
        } catch (Exception e) {
            // Display error on page if there was an issue.
            mv.addObject("errorMessage", "There was an error loading hashtag posts! Please try again.");
            System.out.println("Error loading hashtag posts: " + e.getMessage());
        }

        return mv;
    }
    
}

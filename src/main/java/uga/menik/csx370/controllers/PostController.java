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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import uga.menik.csx370.models.ExpandedPost;
import uga.menik.csx370.services.PostService;
import uga.menik.csx370.services.UserService;

/**
 * Handles /post URL and its sub urls.
 */
@Controller
@RequestMapping("/post")
public class PostController {
    private final PostService postService;
    private final UserService userService;

    @Autowired
    public PostController(UserService userService, PostService postService) {
        this.postService = postService;
        this.userService = userService;
    }
    /**
     * This function handles the /post/{postId} URL.
     * This handlers serves the web page for a specific post.
     * Note there is a path variable {postId}.
     * An example URL handled by this function looks like below:
     * http://localhost:8081/post/1
     * The above URL assigns 1 to postId.
     * 
     * See notes from HomeController.java regardig error URL parameter.
     */
    @GetMapping("/{postId}")
    public ModelAndView webpage(@PathVariable("postId") String postId,
            @RequestParam(name = "error", required = false) String error) {
        System.out.println("The user is attempting to view post with id: " + postId);
        // See notes on ModelAndView in BookmarksController.java.
        ModelAndView mv = new ModelAndView("posts_page");

        // The list of posts to show on the page.
        List<ExpandedPost> posts = new ArrayList<>();

        String errorMessage = error;

        try {
            // Setting the posts object to the actual posts from the database.
            posts = postService.getExpandedPostsById(postId, userService.getLoggedInUser().getUserId());
            mv.addObject("posts", posts);
        } catch (SQLException e) {
            // Display error on page if there was an issue.
            errorMessage = "Failed to load the requested post. Please try again.";
            System.out.println("Error loading posts: " + e.getMessage());
        }

        // If no posts, show no content message.
        mv.addObject("isNoContent", posts.isEmpty());
        // If error, show error message.
        mv.addObject("errorMessage", errorMessage);

        return mv;
    }

    /**
     * Handles comments added on posts.
     * See comments on webpage function to see how path variables work here.
     * This function handles form posts.
     * See comments in HomeController.java regarding form submissions.
     */
    @PostMapping("/{postId}/comment")
    public String postComment(@PathVariable("postId") String postId,
            @RequestParam(name = "comment") String comment) {
        System.out.println("The user is attempting add a comment:");
        System.out.println("\tpostId: " + postId);
        System.out.println("\tcomment: " + comment);

        try {
            String loggedInUserId = userService.getLoggedInUser().getUserId();
            postService.addComment(postId, loggedInUserId, comment);
            // Redirect the user if the comment adding is a success.
            return "redirect:/post/" + postId;
        } catch (SQLException e) {
            // Redirect the user with an error message if there was an error.
            String message = URLEncoder.encode("Failed to post the comment. Please try again.",
                    StandardCharsets.UTF_8);
            System.out.println("Error loading posts: " + e.getMessage());
            return "redirect:/post/" + postId + "?error=" + message;
        }
    }
    

    /**
     * Handles likes added on posts.
     * See comments on webpage function to see how path variables work here.
     * See comments in PeopleController.java in followUnfollowUser function regarding 
     * get type form submissions and how path variables work.
     */
    @GetMapping("/{postId}/heart/{isAdd}")
    public String addOrRemoveHeart(@PathVariable("postId") String postId,
            @PathVariable("isAdd") Boolean isAdd) {
        System.out.println("The user is attempting add or remove a heart:");
        System.out.println("\tpostId: " + postId);
        System.out.println("\tisAdd: " + isAdd);

        try {
            String loggedInUserId = userService.getLoggedInUser().getUserId();

            if (isAdd) postService.addLike(loggedInUserId, postId);
            else postService.removeLike(loggedInUserId, postId);

            // Redirect the user if liking is a success.
            return "redirect:/post/" + postId;
        } catch (Exception e) {
            e.printStackTrace();
            String message = URLEncoder.encode("Failed to (un)like the post. Please try again.", StandardCharsets.UTF_8);
            System.out.println("Error (un)liking post: " + e.getMessage());
            return "redirect:/post/" + postId + "?error=" + message;
        }
    }

    /**
     * Handles bookmarking posts.
     * See comments on webpage function to see how path variables work here.
     * See comments in PeopleController.java in followUnfollowUser function regarding 
     * get type form submissions.
     */
    @GetMapping("/{postId}/bookmark/{isAdd}")
    public String addOrRemoveBookmark(@PathVariable("postId") String postId,
            @PathVariable("isAdd") Boolean isAdd) {
        System.out.println("The user is attempting add or remove a bookmark:");
        System.out.println("\tpostId: " + postId);
        System.out.println("\tisAdd: " + isAdd);

        try {
            final String loggedInUserId = userService.getLoggedInUser().getUserId();

            if (isAdd) postService.addBookmark(loggedInUserId, postId);
            else postService.removeBookmark(loggedInUserId, postId);
            
            // Redirect the user bookmarking is a success.
            return "redirect:/post/" + postId;
        } catch (SQLException e) {
            String message = URLEncoder.encode("Failed to (un)bookmark the post. Please try again. ", StandardCharsets.UTF_8);
            System.out.println("Error (un)bookmarking post: " + e.getMessage());
            return "redirect:/post/" + postId + "?error=" + message;
        }
    }

}

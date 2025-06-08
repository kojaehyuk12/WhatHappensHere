package com.example.demo.controller;

import com.example.demo.entity.BugPost;
import com.example.demo.repository.BugPostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/bug-posts")
public class BugPostController {

    @Autowired
    private BugPostRepository bugPostRepository;

    // Retrieve all bug report posts
    @GetMapping
    public List<BugPost> getAllBugPosts() {
        return bugPostRepository.findAll();
    }

    // Create a new bug report post
    @PostMapping("/create")
    public ResponseEntity<String> createBugPost(@RequestBody BugPost bugPost) {
        bugPost.setCreatedAt(LocalDateTime.now());
        bugPostRepository.save(bugPost);
        return ResponseEntity.ok("Bug report post created successfully.");
    }

    // Delete a bug report post by ID
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteBugPost(@PathVariable Long id) {
        Optional<BugPost> bugPost = bugPostRepository.findById(id);
        if (bugPost.isPresent()) {
            bugPostRepository.deleteById(id);
            return ResponseEntity.ok("Bug report post deleted successfully.");
        } else {
            return ResponseEntity.status(404).body("Bug report post not found.");
        }
    }
}

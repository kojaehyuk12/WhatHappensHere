// PostController.java
package com.example.demo.controller;

import com.example.demo.entity.Post;
import com.example.demo.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    @Autowired
    private PostRepository postRepository;

    // 모든 게시글 조회
    @GetMapping
    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }

    // 게시글 작성
    @PostMapping("/create")
    public ResponseEntity<String> createPost(@RequestBody Post post) {
        post.setCreatedAt(LocalDateTime.now());
        postRepository.save(post);
        return ResponseEntity.ok("게시글 작성 성공");

    }
    // 게시글 삭제
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deletePost(@PathVariable Long id) {
        Optional<Post> post = postRepository.findById(id);
        if (post.isPresent()) {
            postRepository.deleteById(id);
            return ResponseEntity.ok("게시글 삭제 성공");
        } else {
            return ResponseEntity.status(404).body("게시글을 찾을 수 없습니다");
        }
    }
}

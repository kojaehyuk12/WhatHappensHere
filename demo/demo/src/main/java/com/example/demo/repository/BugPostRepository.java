package com.example.demo.repository;

import com.example.demo.entity.BugPost;
import org.springframework.data.jpa.repository.JpaRepository;



public interface BugPostRepository extends JpaRepository<BugPost, Long> {
}

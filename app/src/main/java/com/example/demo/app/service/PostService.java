package com.example.demo.app.service;

import com.example.demo.app.mapper.PostMapper;
import com.example.demo.app.model.Post;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PostService {

    @Autowired
    private PostMapper postMapper;

    public List<Post> getPosts() {
        return postMapper.selectPosts();
    }
}

package com.example.demo.app.mapper;

import com.example.demo.app.model.Post;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PostMapper {
    List<Post> selectPosts();
}

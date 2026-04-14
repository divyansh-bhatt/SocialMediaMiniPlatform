package com.connectsphere.comment.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class CommentDTO {

    private int commentId;
    private int postId;
    private int authorId;
    private String content;
    private int likesCount;
    private LocalDateTime createdAt;

    private List<CommentDTO> replies = new ArrayList<>();
}

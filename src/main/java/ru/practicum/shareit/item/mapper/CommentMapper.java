package ru.practicum.shareit.item.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.model.Comment;

import java.time.LocalDateTime;

@UtilityClass
public class CommentMapper {

    public static CommentDto toCommentDto(Comment comment, String authorName) {
        if (comment == null) {
            return null;
        }
        return new CommentDto(
                comment.getId(),
                comment.getText(),
                authorName,
                comment.getCreated()
        );
    }

    public static Comment toComment(CommentDto commentDto, Long itemId, Long authorId) {
        if (commentDto == null) {
            return null;
        }
        Comment comment = new Comment();
        comment.setText(commentDto.getText());
        comment.setItemId(itemId);
        comment.setAuthorId(authorId);
        comment.setCreated(LocalDateTime.now());
        return comment;
    }
}
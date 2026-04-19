package ru.practicum.shareit.item.dto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.test.context.ContextConfiguration;
import ru.practicum.shareit.ShareItGateway;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
@ContextConfiguration(classes = ShareItGateway.class)
class CommentDtoJsonTest {

    @Autowired
    private JacksonTester<CommentDto> json;

    @Test
    void testSerialize() throws Exception {
        LocalDateTime created = LocalDateTime.of(2026, 4, 20, 10, 0, 0);
        CommentDto dto = new CommentDto(1L, "Great!", "User", created);

        JsonContent<CommentDto> result = json.write(dto);

        assertThat(result).extractingJsonPathNumberValue("$.id").isEqualTo(1);
        assertThat(result).extractingJsonPathStringValue("$.text").isEqualTo("Great!");
        assertThat(result).extractingJsonPathStringValue("$.authorName").isEqualTo("User");
        assertThat(result).extractingJsonPathStringValue("$.created").isEqualTo("2026-04-20T10:00:00");
    }

    @Test
    void testDeserialize() throws Exception {
        String content = "{\"id\":1,\"text\":\"Great!\",\"authorName\":\"User\",\"created\":\"2026-04-20T10:00:00\"}";

        CommentDto dto = json.parseObject(content);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getText()).isEqualTo("Great!");
        assertThat(dto.getAuthorName()).isEqualTo("User");
        assertThat(dto.getCreated()).isEqualTo(LocalDateTime.of(2026, 4, 20, 10, 0, 0));
    }
}
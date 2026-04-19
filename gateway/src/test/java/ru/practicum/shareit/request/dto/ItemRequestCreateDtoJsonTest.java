package ru.practicum.shareit.request.dto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.test.context.ContextConfiguration;
import ru.practicum.shareit.ShareItGateway;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
@ContextConfiguration(classes = ShareItGateway.class)
class ItemRequestCreateDtoJsonTest {

    @Autowired
    private JacksonTester<ItemRequestCreateDto> json;

    @Test
    void testSerialize() throws Exception {
        ItemRequestCreateDto dto = new ItemRequestCreateDto("Need a drill");

        JsonContent<ItemRequestCreateDto> result = json.write(dto);

        assertThat(result).extractingJsonPathStringValue("$.description").isEqualTo("Need a drill");
    }

    @Test
    void testDeserialize() throws Exception {
        String content = "{\"description\":\"Need a drill\"}";

        ItemRequestCreateDto dto = json.parseObject(content);

        assertThat(dto.getDescription()).isEqualTo("Need a drill");
    }
}
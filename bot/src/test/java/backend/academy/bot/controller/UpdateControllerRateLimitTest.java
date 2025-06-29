package backend.academy.bot.controller;

import static backend.academy.bot.TestData.LINK_UPDATE_JSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import backend.academy.bot.BaseIntegrationTest;
import backend.academy.bot.config.bucket.BucketProperties;
import backend.academy.bot.service.UpdateService;
import com.pengrad.telegrambot.TelegramBot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class UpdateControllerRateLimitTest extends BaseIntegrationTest {
    private static final String UPDATE_PATH = "/updates";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BucketProperties bucketProperties;

    @MockitoBean
    UpdateService updateService;

    @MockitoBean
    TelegramBot telegramBot;

    @MockitoBean
    KafkaTemplate<?, ?> kafkaTemplate;

    @Test
    void shouldReturn429WhenRateLimitExceeded() throws Exception {
        long allowedRequests = bucketProperties.capacity();

        for (int i = 0; i < allowedRequests; i++) {
            mockMvc.perform(post(UPDATE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(LINK_UPDATE_JSON))
                    .andExpect(status().isOk());
        }

        var result = mockMvc.perform(post(UPDATE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LINK_UPDATE_JSON))
                .andExpect(status().isTooManyRequests())
                .andReturn();
        assertEquals("Rate limit exceeded", result.getResponse().getContentAsString());
    }
}

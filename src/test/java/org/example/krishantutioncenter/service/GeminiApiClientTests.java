package org.example.krishantutioncenter.service;

import org.example.krishantutioncenter.config.*;
import org.example.krishantutioncenter.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class GeminiApiClientTests {

    @Test
    void sendsPromptWithKeyInHeaderAndParsesProviderJson() {
        AiFeatureProperties properties = new AiFeatureProperties();
        properties.setApiKey("test-secret");
        properties.setModel("gemini-2.5-flash-lite");

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent"))
                .andExpect(method(POST))
                .andExpect(header("x-goog-api-key", "test-secret"))
                .andExpect(jsonPath("$.contents[0].parts[0].text").value("notes-only question"))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[{"text":"{\\"answer\\":\\"Use the class notes.\\",\\"sources\\":[12]}"}]}}]}
                        """, MediaType.APPLICATION_JSON));

        GeminiApiClient client = new GeminiApiClient(properties, JsonMapper.builder().build(), builder.build());
        var response = client.generateJson("notes-only question");

        assertEquals("Use the class notes.", response.get("answer"));
        assertEquals(List.of(12), response.get("sources"));
        server.verify();
    }
}

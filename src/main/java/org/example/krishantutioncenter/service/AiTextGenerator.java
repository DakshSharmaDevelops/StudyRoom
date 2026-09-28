package org.example.krishantutioncenter.service;

import org.example.krishantutioncenter.config.*;
import java.util.Map;

public interface AiTextGenerator {
    Map<String, Object> generateJson(String prompt);
}

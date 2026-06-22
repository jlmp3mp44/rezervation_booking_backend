package com.horovod.hub.controller;

import com.horovod.hub.config.HorovodProperties;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/email")
public class EmailController {

    private final HorovodProperties properties;

    public EmailController(HorovodProperties properties) {
        this.properties = properties;
    }

    @PostMapping("/send")
    public Map<String, Object> send(@RequestBody Map<String, String> body) {
        if (!properties.isEmailEnabled()) {
            System.out.println("[EMAILS DISABLED] To: " + body.get("to") + " Subject: " + body.get("subject"));
            return Map.of("success", true, "disabled", true);
        }
        // Placeholder for future Resend/SMTP integration
        return Map.of("success", true);
    }
}

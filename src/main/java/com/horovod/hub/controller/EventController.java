package com.horovod.hub.controller;

import com.horovod.hub.service.EventBroadcastService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventBroadcastService eventBroadcastService;

    public EventController(EventBroadcastService eventBroadcastService) {
        this.eventBroadcastService = eventBroadcastService;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return eventBroadcastService.subscribe();
    }
}

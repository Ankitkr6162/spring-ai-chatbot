package com.example.ai.chatbot.controller;

import com.example.ai.chatbot.service.ChatbotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        String response = chatbotService.chat(request.question());
        return ResponseEntity.ok(new ChatResponse(response));
    }

    @PostMapping("/simple")
    public ResponseEntity<ChatResponse> chatSimple(@RequestBody ChatRequest request) {
        String response = chatbotService.chatSimple(request.question());
        return ResponseEntity.ok(new ChatResponse(response));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "AI Chatbot",
                "documents", chatbotService.getDocumentsInfo()
        ));
    }

    @GetMapping("/info")
    public ResponseEntity<String> info() {
        return ResponseEntity.ok(chatbotService.getDocumentsInfo());
    }
}

record ChatRequest(String question) {}
record ChatResponse(String answer) {}
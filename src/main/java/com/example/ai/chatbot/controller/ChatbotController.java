package com.example.ai.chatbot.controller;

import com.example.ai.chatbot.service.ChatbotService;
import com.example.ai.chatbot.service.DocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatbotController {

    private final ChatbotService chatbotService;
    private final DocumentService documentService;

    public ChatbotController(ChatbotService chatbotService, DocumentService documentService) {
        this.chatbotService = chatbotService;
        this.documentService = documentService;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        String response = chatbotService.chat(request.question());
        return ResponseEntity.ok(new ChatResponse(response));
    }

//    @PostMapping("/simple")
//    public ResponseEntity<ChatResponse> chatSimple(@RequestBody ChatRequest request) {
//        String response = chatbotService.chatSimple(request.question());
//        return ResponseEntity.ok(new ChatResponse(response));
//    }

//    @GetMapping("/health")
//    public ResponseEntity<Map<String, String>> health() {
//        return ResponseEntity.ok(Map.of("status", "UP", "service", "AI Chatbot", "documents", chatbotService.getDocumentsInfo()));
//    }
//
//    @GetMapping("/info")
//    public ResponseEntity<String> info() {
//        return ResponseEntity.ok(chatbotService.getDocumentsInfo());
//    }


    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadDocument(@RequestParam("file") MultipartFile file) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Validate file
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "Please select a file to upload");
                return ResponseEntity.badRequest().body(response);
            }

            // Validate file type
            String contentType = file.getContentType();
            if (!isValidFileType(contentType)) {
                response.put("success", false);
                response.put("message", "Only PDF and TXT files are allowed");
                return ResponseEntity.badRequest().body(response);
            }

            // Process and save the document
            String fileName = documentService.saveAndProcessDocument(file);

            response.put("success", true);
            response.put("message", "File uploaded and processed successfully");
            response.put("fileName", fileName);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error uploading file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listDocuments() {
        Map<String, Object> response = new HashMap<>();

        try {
            response.put("success", true);
            response.put("documents", documentService.listDocuments());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error listing documents: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @DeleteMapping("/{fileName}")
    public ResponseEntity<Map<String, Object>> deleteDocument(@PathVariable String fileName) {
        Map<String, Object> response = new HashMap<>();

        try {
            boolean deleted = documentService.deleteDocument(fileName);

            if (deleted) {
                response.put("success", true);
                response.put("message", "Document deleted successfully");
            } else {
                response.put("success", false);
                response.put("message", "Document not found");
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error deleting document: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private boolean isValidFileType(String contentType) {
        return contentType != null && (contentType.equals("application/pdf") || contentType.equals("text/plain"));
    }
}


record ChatRequest(String question) {
}

record ChatResponse(String answer) {
}
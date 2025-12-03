package com.example.ai.chatbot.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    @Value("${app.document.path}")
    private Resource documentResource;

    @Value("${app.chunk.size:1000}")
    private int chunkSize;

    @Value("${app.chunk.overlap:200}")
    private int chunkOverlap;

    private String documentContent;

    public ChatbotService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
    }

    @PostConstruct
    public void init() {
        loadDocument();
    }

    private void loadDocument() {
        try {
            TextReader textReader = new TextReader(documentResource);
            List<Document> documents = textReader.get();

            TokenTextSplitter splitter = new TokenTextSplitter(chunkSize, chunkOverlap, 5, 10000, true);
            List<Document> chunks = splitter.apply(documents);

            vectorStore.add(chunks);

            documentContent = documents.stream()
                    .map(Document::getContent)
                    .collect(Collectors.joining("\n"));

            System.out.println("Document loaded successfully with " + chunks.size() + " chunks");
        } catch (Exception e) {
            System.err.println("Error loading document: " + e.getMessage());
            documentContent = "";
        }
    }

    public String chat(String userQuestion) {
        List<Document> similarDocuments = vectorStore.similaritySearch(userQuestion);

        String context = similarDocuments.stream()
                .map(Document::getContent)
                .collect(Collectors.joining("\n\n"));

        String systemPrompt = """
            You are a helpful assistant. Use the following context to answer the user's question.
            If the answer cannot be found in the context, say so politely.
            
            Context:
            {context}
            """;

        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemPrompt);
        Message systemMessage = systemPromptTemplate.createMessage(Map.of("context", context));

        UserMessage userMessage = new UserMessage(userQuestion);
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        return chatClient.prompt(prompt).call().content();
    }

    public String chatSimple(String userQuestion) {
        String systemPrompt = """
            You are a helpful assistant. Use the following information to answer the user's question.
            If the answer cannot be found in the information provided, say so politely.
            
            Information:
            {document}
            """;

        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemPrompt);
        Message systemMessage = systemPromptTemplate.createMessage(Map.of("document", documentContent));

        UserMessage userMessage = new UserMessage(userQuestion);
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        return chatClient.prompt(prompt).call().content();
    }
}

package com.example.ai.chatbot.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    @Value("${app.documents.folder:classpath:data/}")
    private String documentsFolder;

    @Value("${app.chunk.size:1000}")
    private int chunkSize;

    @Value("${app.chunk.overlap:200}")
    private int chunkOverlap;

    private String allDocumentsContent;
    private int totalDocumentsLoaded = 0;

    public ChatbotService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
    }
//
//    @PostConstruct
//    public void init() {
//        loadAllDocuments();
//    }
//
//    private void loadAllDocuments() {
//        try {
//            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
//            List<Document> allDocuments = new ArrayList<>();
//
//            // Load all .txt files
//            Resource[] textResources = resolver.getResources(documentsFolder + "*.txt");
//            System.out.println("Found " + textResources.length + " text files");
//
//            for (Resource resource : textResources) {
//                try {
//                    System.out.println("Loading: " + resource.getFilename());
//                    TextReader textReader = new TextReader(resource);
//                    List<Document> docs = textReader.get();
//
//                    // Add metadata about the source file
//                    docs.forEach(doc -> doc.getMetadata().put("source", resource.getFilename()));
//                    allDocuments.addAll(docs);
//                    totalDocumentsLoaded++;
//                } catch (Exception e) {
//                    System.err.println("Error loading " + resource.getFilename() + ": " + e.getMessage());
//                }
//            }
//
//            // Load all .pdf files
//            Resource[] pdfResources = resolver.getResources(documentsFolder + "*.pdf");
//            System.out.println("Found " + pdfResources.length + " PDF files");
//
//            for (Resource resource : pdfResources) {
//                try {
//                    System.out.println("Loading: " + resource.getFilename());
//                    PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource);
//                    List<Document> docs = pdfReader.get();
//
//                    // Add metadata about the source file
//                    docs.forEach(doc -> doc.getMetadata().put("source", resource.getFilename()));
//                    allDocuments.addAll(docs);
//                    totalDocumentsLoaded++;
//                } catch (Exception e) {
//                    System.err.println("Error loading " + resource.getFilename() + ": " + e.getMessage());
//                }
//            }
//
//            if (allDocuments.isEmpty()) {
//                System.err.println("WARNING: No documents found in " + documentsFolder);
//                allDocumentsContent = "";
//                return;
//            }
//
//            // Split all documents into chunks
//            TokenTextSplitter splitter = new TokenTextSplitter(chunkSize, chunkOverlap, 5, 10000, true);
//            List<Document> chunks = splitter.apply(allDocuments);
//
//            // Store all chunks in vector store
//            vectorStore.add(chunks);
//
//            // Keep full content for simple mode
//            allDocumentsContent = allDocuments.stream()
//                    .map(doc -> {
//                        String source = (String) doc.getMetadata().get("source");
//                        return "--- From: " + source + " ---\n" + doc.getContent();
//                    })
//                    .collect(Collectors.joining("\n\n"));
//
//            System.out.println("Successfully loaded " + totalDocumentsLoaded + " documents with " + chunks.size() + " chunks");
//
//        } catch (IOException e) {
//            System.err.println("Error loading documents: " + e.getMessage());
//            allDocumentsContent = "";
//        }
//    }

    public String chat(String userQuestion) {
        // Retrieve relevant document chunks from ALL documents
        List<Document> similarDocuments = vectorStore.similaritySearch(userQuestion);

        if (similarDocuments.isEmpty()) {
            return "I couldn't find any relevant information to answer your question.";
        }

        // Build context with source information
        String context = similarDocuments.stream()
                .map(doc -> {
                    String source = (String) doc.getMetadata().get("source");
                    return "[Source: " + source + "]\n" + doc.getContent();
                })
                .collect(Collectors.joining("\n\n"));

        String systemPrompt = """
            You are a helpful assistant. Use the following context from multiple documents to answer the user's question.
            Each piece of context shows which document it comes from.
            If the answer cannot be found in the context, say so politely.
            When answering, you can mention which document the information came from if relevant.
            
            Context:
            {context}
            """;

        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemPrompt);
        Message systemMessage = systemPromptTemplate.createMessage(Map.of("context", context));

        UserMessage userMessage = new UserMessage(userQuestion);
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        return chatClient.prompt(prompt).call().content();
    }

//    public String chatSimple(String userQuestion) {
//        if (allDocumentsContent.isEmpty()) {
//            return "No documents have been loaded. Please add documents to the data folder.";
//        }
//
//        String systemPrompt = """
//            You are a helpful assistant. Use the following information from multiple documents to answer the user's question.
//            Each section shows which document it comes from.
//            If the answer cannot be found in the information provided, say so politely.
//
//            Information:
//            {document}
//            """;
//
//        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemPrompt);
//        Message systemMessage = systemPromptTemplate.createMessage(Map.of("document", allDocumentsContent));
//
//        UserMessage userMessage = new UserMessage(userQuestion);
//        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
//
//        return chatClient.prompt(prompt).call().content();
//    }

//    public String getDocumentsInfo() {
//        return "Loaded " + totalDocumentsLoaded + " documents from " + documentsFolder;
//    }
}
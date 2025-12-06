package com.example.ai.chatbot.controller;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DocumentService {


    private final VectorStore vectorStore;

    @Value("${documents.upload.dir:src/main/resources/documents}")
    private String uploadDir;

    public DocumentService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public String saveAndProcessDocument(MultipartFile file) throws IOException {
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Save file to disk
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Process and add to vector store
        processDocument(filePath.toFile());

        return fileName;
    }

    private void processDocument(File file) {
        try {
            List<Document> documents;

            if (file.getName().toLowerCase().endsWith(".pdf")) {
                // Process PDF
                Resource resource = new FileSystemResource(file);
                PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder()
                        .withPageExtractedTextFormatter(new ExtractedTextFormatter.Builder()
                                .withNumberOfBottomTextLinesToDelete(3)
                                .withNumberOfTopPagesToSkipBeforeDelete(1)
                                .build())
                        .withPagesPerDocument(1)
                        .build();

                PagePdfDocumentReader reader = new PagePdfDocumentReader(resource, config);
                documents = reader.get();
            } else {
                // Process TXT
                String content = Files.readString(file.toPath());
                Document doc = new Document(content);
                doc.getMetadata().put("source", file.getName());
                documents = List.of(doc);
            }

            // Split documents into chunks
            TokenTextSplitter splitter = new TokenTextSplitter();
            List<Document> splitDocs = splitter.apply(documents);

            // Add to vector store
            vectorStore.add(splitDocs);

        } catch (Exception e) {
            throw new RuntimeException("Error processing document: " + e.getMessage(), e);
        }
    }

    public List<String> listDocuments() throws IOException {
        Path uploadPath = Paths.get(uploadDir);

        if (!Files.exists(uploadPath)) {
            return List.of();
        }

        try (Stream<Path> paths = Files.list(uploadPath)) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        }
    }

    public boolean deleteDocument(String fileName) throws IOException {
        Path filePath = Paths.get(uploadDir).resolve(fileName);

        if (Files.exists(filePath)) {
            Files.delete(filePath);
            // Note: This doesn't remove from vector store
            // You may need to rebuild the vector store or implement removal logic
            return true;
        }

        return false;
    }

    public void reloadAllDocuments() throws IOException {
        List<String> files = listDocuments();

        for (String fileName : files) {
            File file = Paths.get(uploadDir).resolve(fileName).toFile();
            processDocument(file);
        }
    }
}

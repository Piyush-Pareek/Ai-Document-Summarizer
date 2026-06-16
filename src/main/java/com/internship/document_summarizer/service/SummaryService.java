package com.internship.document_summarizer.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SummaryService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=";

    // 1. The Main Orchestrator
    public String processAndSummarizeFile(MultipartFile file) throws Exception {
        // Extract raw text based on file type
        String rawText = extractTextFromFile(file);

        if (rawText == null || rawText.trim().isEmpty()) {
            throw new Exception(
                    "Could not extract any text from the document. It might be empty or an image-based PDF.");
        }

        // Chunk the text to simulate pages (roughly 3000 chars)
        List<String> pages = chunkText(rawText, 3000);
        StringBuilder finalSummary = new StringBuilder();

        int pageNum = 1;
        for (String page : pages) {
            finalSummary.append("### Page ").append(pageNum).append(" Summary:\n");
            finalSummary.append(callGemini(page)).append("\n\n");
            pageNum++;
        }

        return finalSummary.toString();
    }

    // 2. The File Parser
    private String extractTextFromFile(MultipartFile file) throws Exception {
        String fileName = file.getOriginalFilename();
        if (fileName == null)
            return "";

        try (InputStream inputStream = file.getInputStream()) {
            if (fileName.endsWith(".pdf")) {
                try (PDDocument document = PDDocument.load(inputStream)) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    return stripper.getText(document);
                }
            } else if (fileName.endsWith(".docx")) {
                try (XWPFDocument document = new XWPFDocument(inputStream);
                        XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                    return extractor.getText();
                }
            } else if (fileName.endsWith(".txt")) {
                return new String(file.getBytes(), StandardCharsets.UTF_8);
            } else {
                throw new Exception("Unsupported file format. Please upload .pdf, .docx, or .txt");
            }
        }
    }

    // 3. The Chunker
    private List<String> chunkText(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < text.length(); i += chunkSize) {
            chunks.add(text.substring(i, Math.min(text.length(), i + chunkSize)));
        }
        return chunks;
    }

    // 4. The AI Caller
    private String callGemini(String textChunk) {
        RestTemplate restTemplate = new RestTemplate();
        String fullUrl = GEMINI_URL + apiKey;

        // Clean the chunk to avoid breaking the JSON payload
        String safeText = textChunk.replace("\"", "\\\"").replace("\n", " ").replace("\r", "");

        String systemPrompt = "You are an expert technical writer and executive assistant. " +
                "Read the following text block and provide a concise summary. " +
                "Follow these strict rules:\\n" +
                "1. Provide exactly 3 to 5 clear bullet points containing the most critical information.\\n" +
                "2. Do not use complex jargon. If a highly technical term or jargon is present, briefly define it in simple, everyday English within the bullet point.\\n"
                +
                "3. Output the response in plain text format only.\\n\\n" +
                "Here is the text to summarize:\\n" + safeText;

        String requestBody = "{\"contents\": [{\"parts\": [{\"text\": \"" + systemPrompt + "\"}]}]}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(fullUrl, entity, Map.class);
            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                    return (String) parts.get(0).get("text");
                }
            }
            return "Error: Could not parse response from AI.";
        } catch (Exception e) {
            return "Error communicating with AI: " + e.getMessage();
        }
    }
}
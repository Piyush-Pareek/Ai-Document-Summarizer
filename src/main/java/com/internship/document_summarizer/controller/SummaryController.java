package com.internship.document_summarizer.controller;


import com.internship.document_summarizer.service.SummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/document")
@CrossOrigin(origins = "*")
public class SummaryController {

    @Autowired
    private SummaryService summaryService;

    @PostMapping("/summarize")
    public ResponseEntity<String> uploadAndSummarize(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Error: Please upload a valid document.");
        }

        try {
            String summary = summaryService.processAndSummarizeFile(file);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to process document: " + e.getMessage());
        }
    }
}

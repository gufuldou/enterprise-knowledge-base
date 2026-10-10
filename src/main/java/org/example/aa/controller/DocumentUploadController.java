package org.example.aa.controller;

import org.example.aa.common.ApiResponse;
import org.example.aa.entity.KnowledgeDocument;
import org.example.aa.service.DocumentService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class DocumentUploadController {

    private final DocumentService documentService;

    public DocumentUploadController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        KnowledgeDocument document = documentService.upload(file);
        Map<String, Object> data = Map.of(
                "documentId", document.getId(),
                "title", document.getTitle(),
                "fileType", document.getFileType(),
                "chunkCount", document.getChunkCount()
        );
        return ApiResponse.ok("文件上传并解析成功", data);
    }
}

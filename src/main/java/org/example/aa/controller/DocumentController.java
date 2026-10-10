package org.example.aa.controller;

import org.example.aa.common.ApiResponse;
import org.example.aa.entity.KnowledgeDocument;
import org.example.aa.mapper.DocumentMapper;
import org.example.aa.service.DocumentService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentMapper documentMapper;
    private final DocumentService documentService;

    public DocumentController(DocumentMapper documentMapper, DocumentService documentService) {
        this.documentMapper = documentMapper;
        this.documentService = documentService;
    }

    @GetMapping
    public ApiResponse<List<KnowledgeDocument>> list() {
        return ApiResponse.ok(documentMapper.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<KnowledgeDocument> getById(@PathVariable Long id) {
        return ApiResponse.ok(documentMapper.findById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return ApiResponse.ok("删除成功", null);
    }
}

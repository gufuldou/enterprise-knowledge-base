package org.example.aa.controller;

import org.example.aa.common.ApiResponse;
import org.example.aa.service.ChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@Validated
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ApiResponse<ChatService.RAGAnswer> chat(
            @RequestBody @Valid ChatRequest request) {
        return ApiResponse.ok(chatService.chat(request.question()));
    }

    public record ChatRequest(
            @NotBlank(message = "问题不能为空")
            @Size(max = 2000, message = "问题不能超过2000个字符")
            String question) {
    }
}

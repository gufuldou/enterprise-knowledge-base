package org.example.aa.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatClient chatClient;
    private final RetrievalService retrievalService;

    public ChatService(ChatClient.Builder builder, RetrievalService retrievalService) {
        this.chatClient = builder.build();
        this.retrievalService = retrievalService;
    }

    public record RAGAnswer(String answer, List<RetrievalService.RetrievedChunk> sources) {
    }

    public RAGAnswer chat(String question) {
        long start = System.currentTimeMillis();
        List<RetrievalService.RetrievedChunk> sources = retrievalService.retrieve(question, 3);

        if (sources.isEmpty()) {
            return new RAGAnswer("知识库中暂无内容，请先上传文档。", sources);
        }

        StringBuilder context = new StringBuilder();
        for (int i = 0; i < sources.size(); i++) {
            context.append("[资料").append(i + 1).append("]\n")
                    .append(sources.get(i).content()).append("\n\n");
        }

        String answer = chatClient.prompt()
                .system("""
                        你是一个企业知识库助手。请严格基于提供的资料回答用户问题。
                        如果资料中没有相关信息，请明确回答"知识库中未找到相关内容"。
                        回答要准确简洁，并在结尾用 [资料N] 标注所引用的资料。
                        """)
                .user("资料内容：\n" + context + "用户问题：" + question)
                .call()
                .content();

        log.info("问答完成: 耗时 {}ms, 引用 {} 个来源", System.currentTimeMillis() - start, sources.size());
        return new RAGAnswer(answer, sources);
    }
}

package eureca.capstone.project.batch.component.tasklet;

import com.fasterxml.jackson.databind.ObjectMapper;
import eureca.capstone.project.batch.common.dto.response.AIKeywordResponseDto;
import eureca.capstone.project.batch.transaction_feed.entity.TransactionFeed;
import eureca.capstone.project.batch.transaction_feed.repository.TransactionFeedRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;


import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class KeywordExtractionTasklet implements Tasklet {
    private final TransactionFeedRepository transactionFeedRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ChatClient chatClient;
    private final String systemPromptTemplate;

    private static final String EXECUTE_URL = "https://visiblego.com/orchestrator/common/execute?keyword={keyword}";

    public KeywordExtractionTasklet(@Qualifier("keywordExtractionClient") ChatClient chatClient,
                                    TransactionFeedRepository transactionFeedRepository,
                                    RestTemplate restTemplate,
                                    ObjectMapper objectMapper,
                                    @Value("classpath:prompts/keyword-extraction-system.txt") Resource systemPromptResource){
        this.transactionFeedRepository=transactionFeedRepository;
        this.restTemplate=restTemplate;
        this.objectMapper=objectMapper;
        this.chatClient=chatClient;
        try {
            this.systemPromptTemplate = systemPromptResource.getContentAsString(StandardCharsets.UTF_8);
            log.info("키워드 추출 프롬프트 로딩 성공");
        } catch (IOException e) {
            log.error("키워드 추출 프롬프트 로딩 실패");
            throw new UncheckedIOException("키워드 추출 프롬프트 로딩 실패", e);
        }
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("[KeywordExtractionTasklet] AI 기반 키워드 추출 Tasklet 시작");
        Pageable pageable = PageRequest.of(0, 30, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<String> recentTitles = transactionFeedRepository.findAll(pageable).getContent()
                .stream()
                .map(feed -> String.format("제목: %s, 내용: %s", feed.getTitle(), feed.getContent()))
                .toList();

        if (recentTitles.isEmpty()) {
            log.info("추출할 최신 게시글이 없어 Tasklet을 종료합니다.");
            return RepeatStatus.FINISHED;
        }

        // 2. AI 호출하여 키워드 추출
        List<String> finalKeywords = callAiForKeywords(recentTitles);
        log.info("AI가 추출한 최종 키워드 ({}개): {}", finalKeywords.size(), finalKeywords);

        if (finalKeywords.isEmpty()) {
            log.warn("AI가 키워드를 추출하지 못했습니다. Tasklet을 종료합니다.");
            return RepeatStatus.FINISHED;
        }

        // 3. 추출된 키워드로 API 호출
        finalKeywords.forEach(keyword -> {
            try {
                restTemplate.getForObject(EXECUTE_URL, Void.class, keyword);
                log.info("✅ '{}' 키워드 랭킹 등록 API 호출 성공", keyword);
                Thread.sleep(100); // API 서버 부하 분산을 위한 짧은 대기
            } catch (Exception e) {
                log.error("❌ '{}' 키워드 랭킹 등록 API 호출 실패: {}", keyword, e.getMessage());
            }
        });

        log.info("[KeywordExtractionTasklet] Tasklet 완료");
        return RepeatStatus.FINISHED;
    }

    private List<String> callAiForKeywords(List<String> posts) {
        var outputConverter = new BeanOutputConverter<>(AIKeywordResponseDto.class);
        String finalSystemPrompt = systemPromptTemplate.replace("{format}", outputConverter.getFormat());

        String postList = posts.stream()
                .map(post -> "- " + post)
                .collect(Collectors.joining("\n"));

        try {
            AIKeywordResponseDto response = chatClient.prompt()
                    .system(finalSystemPrompt)
                    .user(postList)
                    .call()
                    .entity(AIKeywordResponseDto.class);

            return response.getKeywords();
        } catch (Exception e) {
            log.error("AI 키워드 추출 중 오류 발생", e);
            return List.of();
        }
    }
}

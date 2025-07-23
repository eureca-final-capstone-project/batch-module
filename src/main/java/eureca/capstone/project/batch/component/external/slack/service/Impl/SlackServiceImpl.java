package eureca.capstone.project.batch.component.external.slack.service.Impl;

import eureca.capstone.project.batch.component.external.slack.dto.SlackMessageRequestDto;
import eureca.capstone.project.batch.component.external.slack.service.SlackService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SlackServiceImpl implements SlackService {

    @Value("${slack.bot.token}")
    private String slackBotToken;

    @Value("${slack.channel}")
    private String slackChannel;

    private final WebClient webClient;

    @Override
    public void sendMessage(SlackMessageRequestDto message) {
        sendMessageToChannel(slackChannel, message);
    }

    @Override
    public void sendMessageToChannel(String channel, SlackMessageRequestDto message) {

        String markdownMessage = String.format(
                """
                *:rotating_light: [배치 실패] :rotating_light:*
                
                *Job* : `%s`
                *Step* : `%s`
                *Start* : %s
                *End* : %s
                *ReadCount* : %d
                *WriteCount* : %d
                *Exit Status* : %s
                *Error Message* : ```
                %s
                ```
                """,
                message.getJobName(),
                message.getStepName(),
                timeFormat(message.getStartTime()),
                timeFormat(message.getEndTime()),
                message.getReadCount(),
                message.getWriteCount(),
                message.getExitSatus(),
                message.getErrorMessage()
        );
        webClient.post()
                .uri("/chat.postMessage")
                .header("Authorization", "Bearer " + slackBotToken)
                .bodyValue(Map.of(
                        "channel", channel,
                        "text", markdownMessage
                ))
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(error -> System.err.println("Slack error: " + error.getMessage()))
                .subscribe();
    }

    private String timeFormat(LocalDateTime time) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return time.format(formatter);
    }
}

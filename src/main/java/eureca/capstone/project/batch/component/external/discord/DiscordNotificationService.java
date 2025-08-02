//package eureca.capstone.project.batch.component.external.discord;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpEntity;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
//import java.awt.Color;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class DiscordNotificationService {
//
//    private final RestTemplate restTemplate;
//
//    @Value("${discord.webhook.url}")
//    private String discordWebhookUrl;
//
//    public void sendMessage(String title, String description, Color color) {
//        try {
//            Map<String, Object> embed = new HashMap<>();
//            embed.put("title", title);
//            embed.put("description", description);
//            embed.put("color", color.getRGB() & 0xFFFFFF); // 색상 코드 변환
//
//            Map<String, Object> payload = new HashMap<>();
//            payload.put("embeds", List.of(embed));
//
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_JSON);
//
//            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
//
//            restTemplate.postForEntity(discordWebhookUrl, request, String.class);
//            log.info("디스코드 알림 전송 성공");
//        } catch (Exception e) {
//            log.error("디스코드 알림 전송 실패: {}", e.getMessage(), e);
//        }
//    }
//}
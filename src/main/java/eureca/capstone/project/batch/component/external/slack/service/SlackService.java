package eureca.capstone.project.batch.component.external.slack.service;

import eureca.capstone.project.batch.component.external.slack.dto.SlackMessageRequestDto;

public interface SlackService {
    void sendMessageToChannel(String channel, SlackMessageRequestDto message);

    void sendMessage(SlackMessageRequestDto message);
}

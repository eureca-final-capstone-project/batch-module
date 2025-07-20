package eureca.capstone.project.batch.slack.service;

import eureca.capstone.project.batch.slack.dto.SlackMessageRequestDto;

public interface SlackService {
    void sendMessageToChannel(String channel, SlackMessageRequestDto message);

    void sendMessage(SlackMessageRequestDto message);
}

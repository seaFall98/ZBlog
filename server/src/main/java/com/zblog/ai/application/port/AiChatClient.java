package com.zblog.ai.application.port;

import com.zblog.ai.application.AiConfig;

public interface AiChatClient {

  String completeChat(AiConfig config, String systemPrompt, String userContent);
}

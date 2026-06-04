package com.zblog.tools.application.port;

import java.util.Map;

public record RemoteResource(Map<String, String> headers, byte[] body) {}

package com.zblog.ops.application.port;

import java.util.Map;

public interface SystemRuntimeInfoProvider {

  Map<String, Object> staticInfo();

  Map<String, Object> dynamicInfo();
}

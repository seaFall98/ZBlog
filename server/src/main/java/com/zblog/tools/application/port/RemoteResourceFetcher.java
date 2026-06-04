package com.zblog.tools.application.port;

import java.net.URI;

public interface RemoteResourceFetcher {

  RemoteResource fetch(URI uri, int limit, String... allowedContentTypes);
}

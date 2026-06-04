package com.zblog.content.application.port;

import java.util.Optional;

public interface ArticleAssetStore {

  boolean existsUpload(String url);

  Optional<byte[]> readUpload(String url);
}

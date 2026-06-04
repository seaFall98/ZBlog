package com.zblog.guestbook.application.port;

import com.zblog.common.api.PageResponse;
import com.zblog.guestbook.domain.GuestbookMessage;
import com.zblog.guestbook.domain.GuestbookStatus;

public interface GuestbookRepository {

  GuestbookMessage create(String nickname, String email, String content, String ip, String userAgent);

  PageResponse<GuestbookMessage> listPublic(int page, int pageSize);

  PageResponse<GuestbookMessage> listAdmin(
      int page,
      int pageSize,
      String keyword,
      GuestbookStatus status,
      Boolean pinned,
      String startTime,
      String endTime);

  GuestbookMessage updateStatus(long id, GuestbookStatus status, String adminNote);

  GuestbookMessage updatePinned(long id, boolean pinned);

  void delete(long id);

  GuestbookMessage findById(long id);
}

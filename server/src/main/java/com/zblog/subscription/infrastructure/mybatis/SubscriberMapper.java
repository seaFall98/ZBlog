package com.zblog.subscription.infrastructure.mybatis;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SubscriberMapper {

  List<Long> idsByEmail(@Param("email") String email);

  void insertSubscriber(Map<String, Object> params);

  void insertSubscriberV2(Map<String, Object> params);

  void resetPending(
      @Param("id") long id,
      @Param("unsubscribeToken") String unsubscribeToken,
      @Param("confirmationToken") String confirmationToken);

  void activate(@Param("id") long id);

  List<Long> idsByConfirmationToken(@Param("token") String token);

  void reactivate(@Param("id") long id);

  List<Long> activeIdsByToken(@Param("token") String token);

  void deactivate(@Param("id") long id);

  long countActiveRows();

  long countAdminRows(@Param("keyword") String keyword, @Param("status") String status);

  List<Map<String, Object>> listAdminRows(@Param("limit") int limit, @Param("offset") int offset);

  List<Map<String, Object>> listAdminRowsV2(
      @Param("keyword") String keyword,
      @Param("status") String status,
      @Param("limit") int limit,
      @Param("offset") int offset);

  List<Map<String, Object>> listActiveRowsAfterId(@Param("limit") int limit, @Param("afterId") long afterId);

  void recordDeliveryQueued(@Param("id") long id);

  void markBounced(@Param("id") long id, @Param("errorMessage") String errorMessage);

  void delete(@Param("id") long id);

  List<Map<String, Object>> rowsById(@Param("id") long id);
}

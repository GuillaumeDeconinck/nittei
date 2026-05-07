package com.meetsmore.nittei.infra.repos;

import com.meetsmore.nittei.domain.Calendar;
import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.infra.repos.query.MetadataFindQuery;
import java.util.List;
import java.util.Optional;

public interface CalendarRepository {
  void insert(Calendar calendar);

  void save(Calendar calendar);

  Optional<Calendar> find(ID calendarId);

  List<Calendar> findMultiple(List<ID> calendarIds);

  List<Calendar> findByUser(ID userId);

  Optional<Calendar> findByUserAndKey(ID userId, String key);

  List<Calendar> findForUsers(List<ID> userIds);

  void delete(ID calendarId);

  List<Calendar> findByMetadata(MetadataFindQuery query);
}

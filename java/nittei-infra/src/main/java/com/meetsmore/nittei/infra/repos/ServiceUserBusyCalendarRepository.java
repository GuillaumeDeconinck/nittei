package com.meetsmore.nittei.infra.repos;

import com.meetsmore.nittei.domain.BusyCalendarProvider;
import com.meetsmore.nittei.domain.ID;
import java.util.List;

public interface ServiceUserBusyCalendarRepository {
  boolean exists(BusyCalendarIdentifier input);

  boolean existsExternal(ExternalBusyCalendarIdentifier input);

  void insert(BusyCalendarIdentifier input);

  void insertExternal(ExternalBusyCalendarIdentifier input);

  void delete(BusyCalendarIdentifier input);

  void deleteExternal(ExternalBusyCalendarIdentifier input);

  List<BusyCalendarProvider> find(ID serviceId, ID userId);
}

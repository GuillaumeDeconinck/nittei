package com.meetsmore.nittei.infra.repos;

import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.Schedule;
import com.meetsmore.nittei.infra.repos.query.MetadataFindQuery;
import java.util.List;
import java.util.Optional;

public interface ScheduleRepository {
    void insert(Schedule schedule);
    void save(Schedule schedule);
    Optional<Schedule> find(ID scheduleId);
    List<Schedule> findMany(List<ID> scheduleIds);
    List<Schedule> findByUser(ID userId);
    void delete(ID scheduleId);
    List<Schedule> findByMetadata(MetadataFindQuery query);
}

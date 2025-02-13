import { NitteiBaseClient } from './baseClient'
import type { ID } from './gen_types/ID'
import type { ScheduleDTO } from './gen_types/ScheduleDTO'
import type { ScheduleRule } from './gen_types/ScheduleRule'

interface UpdateScheduleRequest {
  rules?: ScheduleRule[]
  timezone?: string
}

interface CreateScheduleRequest {
  timezone: string
  rules?: ScheduleRule[]
}

type ScheduleResponse = {
  schedule: ScheduleDTO
}

/**
 * Client for the schedule endpoints (admin)
 */
export class NitteiScheduleClient extends NitteiBaseClient {
  public async create(userId: ID, req: CreateScheduleRequest) {
    return await this.post<ScheduleResponse>(`/user/${userId}/schedule`, req)
  }

  public async update(scheduleId: ID, update: UpdateScheduleRequest) {
    return await this.put<ScheduleResponse>(
      `/user/schedule/${scheduleId}`,
      update
    )
  }

  public async remove(scheduleId: ID) {
    return await this.delete<ScheduleResponse>(`/user/schedule/${scheduleId}`)
  }

  public async find(scheduleId: ID) {
    return await this.get<ScheduleResponse>(`/user/schedule/${scheduleId}`)
  }
}

/**
 * Client for the schedule endpoints (user)
 */
export class NitteiScheduleUserClient extends NitteiBaseClient {
  public async create(req: CreateScheduleRequest) {
    return await this.post<ScheduleResponse>('/schedule', req)
  }

  public async update(scheduleId: ID, update: UpdateScheduleRequest) {
    return await this.put<ScheduleResponse>(`/schedule/${scheduleId}`, update)
  }

  public async remove(scheduleId: ID) {
    return await this.delete<ScheduleResponse>(`/schedule/${scheduleId}`)
  }

  public async find(scheduleId: ID) {
    return await this.get<ScheduleResponse>(`/schedule/${scheduleId}`)
  }
}

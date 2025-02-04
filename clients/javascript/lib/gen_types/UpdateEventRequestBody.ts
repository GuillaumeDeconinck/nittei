// This file was generated by [ts-rs](https://github.com/Aleph-Alpha/ts-rs). Do not edit this file manually.
import type { CalendarEventReminder } from './CalendarEventReminder'
import type { CalendarEventStatus } from './CalendarEventStatus'
import type { ID } from './ID'
import type { RRuleOptions } from './RRuleOptions'
import type { JsonValue } from './serde_json/JsonValue'

/**
 * Request body for updating an event
 */
export type UpdateEventRequestBody = {
  /**
   * Optional start time of the event (UTC)
   */
  startTime?: Date
  /**
   * Optional title of the event
   */
  title?: string
  /**
   * Optional description of the event
   */
  description?: string
  /**
   * Optional type of the event
   * e.g. "meeting", "reminder", "birthday"
   * Default is None
   */
  eventType?: string
  /**
   * Optional parent event ID
   * This is useful for external applications that need to link Nittei's events to a wider data model (e.g. a project, an order, etc.)
   */
  parentId?: string
  /**
   * Optional external event ID
   * This is useful for external applications that need to link Nittei's events to their own data models
   * Default is None
   */
  externalId?: string
  /**
   * Optional location of the event
   */
  location?: string
  /**
   * Optional status of the event
   * Default is "Tentative"
   */
  status?: CalendarEventStatus | null
  /**
   * Optional flag to indicate if the event is an all day event
   * Default is false
   */
  allDay?: boolean
  /**
   * Optional duration of the event in milliseconds
   */
  duration?: number
  /**
   * Optional busy flag
   */
  busy?: boolean
  /**
   * Optional new recurrence rule
   */
  recurrence?: RRuleOptions
  /**
   * Optional service UUID
   */
  serviceId?: ID
  /**
   * Optional group UUID
   * Allows to group events together (e.g. a project, a team, etc.)
   * Default is None
   */
  groupId?: ID
  /**
   * Optional list of exclusion dates for the recurrence rule
   */
  exdates?: Array<Date>
  /**
   * Optional recurring event ID
   * This is the ID of the recurring event that this event is part of
   * Default is None
   */
  recurringEventId?: ID
  /**
   * Optional original start time of the event
   * This is the original start time of the event before it was moved (only for recurring events)
   * Default is None
   */
  originalStartTime?: Date
  /**
   * Optional list of reminders
   */
  reminders?: Array<CalendarEventReminder>
  /**
   * Optional metadata (e.g. {"key": "value"})
   */
  metadata?: JsonValue
  /**
   * Optional created date to use to replace the current one
   */
  created?: Date
  /**
   * Optional updated date to use to replace the current one
   */
  updated?: Date
}

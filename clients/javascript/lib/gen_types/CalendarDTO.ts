// This file was generated by [ts-rs](https://github.com/Aleph-Alpha/ts-rs). Do not edit this file manually.
import type { CalendarSettingsDTO } from './CalendarSettingsDTO'
import type { ID } from './ID'
import type { JsonValue } from './serde_json/JsonValue'

/**
 * Calendar object
 */
export type CalendarDTO = {
  /**
   * UUID of the calendar
   */
  id: ID
  /**
   * UUID of the user that owns the calendar
   */
  userId: ID
  /**
   * Name of the calendar (optional)
   */
  name?: string
  /**
   * Key of the calendar (optional)
   * When defined, this is unique per user
   */
  key?: string
  /**
   * Calendar settings
   */
  settings: CalendarSettingsDTO
  /**
   * Metadata (e.g. {"key": "value"})
   */
  metadata?: JsonValue
}

use nittei_domain::{CalendarEvent, IntegrationProvider, SyncedCalendarEvent};
use nittei_infra::{
    google_calendar::GoogleCalendarProvider,
    outlook_calendar::OutlookCalendarProvider,
};
use tracing::{error, info};

use super::{
    create_event::CreateEventUseCase,
    sync_event_reminders::{EventOperation, SyncEventRemindersTrigger, SyncEventRemindersUseCase},
    update_event::UpdateEventUseCase,
};
use crate::{
    event::create_batch_events::CreateBatchEventsUseCase,
    shared::usecase::{Subscriber, execute},
};

pub struct CreateRemindersOnEventCreated;

#[async_trait::async_trait]
impl Subscriber<CreateEventUseCase> for CreateRemindersOnEventCreated {
    async fn notify(&self, e: &CalendarEvent, ctx: &nittei_infra::NitteiContext) {
        let sync_event_reminders = SyncEventRemindersUseCase {
            request: SyncEventRemindersTrigger::EventModified(e, EventOperation::Created),
        };

        // Sideeffect, ignore result
        let _ = execute(sync_event_reminders, ctx).await;
    }
}

#[async_trait::async_trait]
impl Subscriber<CreateBatchEventsUseCase> for CreateRemindersOnEventCreated {
    async fn notify(&self, events: &Vec<CalendarEvent>, ctx: &nittei_infra::NitteiContext) {
        for event in events {
            let sync_event_reminders = SyncEventRemindersUseCase {
                request: SyncEventRemindersTrigger::EventModified(event, EventOperation::Created),
            };

            // Sideeffect, ignore result
            let _ = execute(sync_event_reminders, ctx).await;
        }
    }
}
pub struct SyncRemindersOnEventUpdated;

#[async_trait::async_trait]
impl Subscriber<UpdateEventUseCase> for SyncRemindersOnEventUpdated {
    async fn notify(&self, e: &CalendarEvent, ctx: &nittei_infra::NitteiContext) {
        let sync_event_reminders = SyncEventRemindersUseCase {
            request: SyncEventRemindersTrigger::EventModified(e, EventOperation::Updated),
        };

        // Sideeffect, ignore result
        let _ = execute(sync_event_reminders, ctx).await;
    }
}

pub struct CreateSyncedEventsOnEventCreated;

#[async_trait::async_trait]
impl Subscriber<CreateEventUseCase> for CreateSyncedEventsOnEventCreated {
    async fn notify(&self, e: &CalendarEvent, ctx: &nittei_infra::NitteiContext) {
        let synced_calendars = match ctx
            .repos
            .calendar_synced
            .find_by_calendar(&e.calendar_id)
            .await
        {
            Ok(synced_calendars) => synced_calendars,
            Err(e) => {
                error!("Unable to query synced calendars from repo: {:?}", e);
                return;
            }
        };

        let synced_outlook_calendars = synced_calendars
            .iter()
            .filter(|cal| cal.provider == IntegrationProvider::Outlook)
            .collect::<Vec<_>>();
        let synced_google_calendars = synced_calendars
            .iter()
            .filter(|cal| cal.provider == IntegrationProvider::Google)
            .collect::<Vec<_>>();

        if synced_google_calendars.is_empty() && synced_outlook_calendars.is_empty() {
            return;
        }
        let user = ctx.repos.users.find(&e.user_id).await;
        let user = match user {
            Ok(Some(u)) => u,
            Ok(None) => {
                error!("Unable to find user when creating sync events");
                return;
            }
            Err(e) => {
                error!("Unable to find user when creating sync events {:?}", e);
                return;
            }
        };

        if !synced_outlook_calendars.is_empty() {
            let provider = match OutlookCalendarProvider::new(&user, ctx).await {
                Ok(p) => p,
                Err(_) => {
                    error!("Unable to create outlook calendar provider");
                    return;
                }
            };
            for synced_o_cal in synced_outlook_calendars {
                let ext_event = match provider
                    .create_event(synced_o_cal.ext_calendar_id.clone(), e.clone())
                    .await
                {
                    Ok(e) => e,
                    Err(_) => {
                        error!("Unable to create external outlook calendar event");
                        continue;
                    }
                };

                let synced_event = SyncedCalendarEvent {
                    calendar_id: e.calendar_id.clone(),
                    event_id: e.id.clone(),
                    ext_calendar_id: synced_o_cal.ext_calendar_id.clone(),
                    ext_event_id: ext_event.id,
                    provider: synced_o_cal.provider.clone(),
                    user_id: user.id.clone(),
                };
                if ctx.repos.event_synced.insert(&synced_event).await.is_err() {
                    error!("Unable to insert outlook synced calendar event into repo");
                }
            }
        }

        if !synced_google_calendars.is_empty() {
            let provider = match GoogleCalendarProvider::new(&user, ctx).await {
                Ok(p) => p,
                Err(_) => {
                    error!("Unable to create google calendar provider");
                    return;
                }
            };
            for synced_g_cal in synced_google_calendars {
                let ext_event = match provider
                    .create_event(synced_g_cal.ext_calendar_id.clone(), e.clone())
                    .await
                {
                    Ok(e) => e,
                    Err(_) => {
                        error!("Unable to create google external calendar event");
                        continue;
                    }
                };

                let synced_event = SyncedCalendarEvent {
                    calendar_id: e.calendar_id.clone(),
                    event_id: e.id.clone(),
                    ext_calendar_id: synced_g_cal.ext_calendar_id.clone(),
                    ext_event_id: ext_event.id,
                    provider: synced_g_cal.provider.clone(),
                    user_id: user.id.clone(),
                };
                if ctx.repos.event_synced.insert(&synced_event).await.is_err() {
                    error!("Unable to insert google synced calendar event into repo");
                } else {
                    info!("Inserted google synced events ");
                }
            }
        }
    }
}

#[async_trait::async_trait]
impl Subscriber<CreateBatchEventsUseCase> for CreateSyncedEventsOnEventCreated {
    async fn notify(&self, events: &Vec<CalendarEvent>, ctx: &nittei_infra::NitteiContext) {
        for event in events {
            // Re-use the same subscriber for single events and many events
            <Self as Subscriber<CreateEventUseCase>>::notify(self, event, ctx).await;
        }
    }
}
pub struct UpdateSyncedEventsOnEventUpdated;

#[async_trait::async_trait]
impl Subscriber<UpdateEventUseCase> for UpdateSyncedEventsOnEventUpdated {
    async fn notify(&self, e: &CalendarEvent, ctx: &nittei_infra::NitteiContext) {
        let synced_events = match ctx.repos.event_synced.find_by_event(&e.id).await {
            Ok(synced_calendars) => synced_calendars,
            Err(e) => {
                error!("Unable to query synced events from repo: {:?}", e);
                return;
            }
        };

        let synced_outlook_events = synced_events
            .iter()
            .filter(|synced_o_event| synced_o_event.provider == IntegrationProvider::Outlook)
            .collect::<Vec<_>>();
        let synced_google_events = synced_events
            .iter()
            .filter(|synced_g_event| synced_g_event.provider == IntegrationProvider::Google)
            .collect::<Vec<_>>();

        if synced_google_events.is_empty() && synced_outlook_events.is_empty() {
            return;
        }
        let user = match ctx.repos.users.find(&e.user_id).await {
            Ok(Some(u)) => u,
            Ok(None) => {
                error!("Unable to find user when updating sync events");
                return;
            }
            Err(e) => {
                error!("Unable to find user when updating sync events {:?}", e);
                return;
            }
        };

        if !synced_outlook_events.is_empty() {
            let provider = match OutlookCalendarProvider::new(&user, ctx).await {
                Ok(p) => p,
                Err(_) => {
                    error!("Unable to create outlook calendar provider");
                    return;
                }
            };
            for synced_o_event in synced_outlook_events {
                if provider
                    .update_event(
                        synced_o_event.ext_calendar_id.clone(),
                        synced_o_event.ext_event_id.clone(),
                        e.clone(),
                    )
                    .await
                    .is_err()
                {
                    error!("Unable to update external outlook calendar event");
                };
            }
        }

        if !synced_google_events.is_empty() {
            let provider = match GoogleCalendarProvider::new(&user, ctx).await {
                Ok(p) => p,
                Err(_) => {
                    error!("Unable to create google calendar provider");
                    return;
                }
            };
            for synced_g_event in synced_google_events {
                if provider
                    .update_event(
                        synced_g_event.ext_calendar_id.clone(),
                        synced_g_event.ext_event_id.clone(),
                        e.clone(),
                    )
                    .await
                    .is_err()
                {
                    error!("Unable to update google external calendar event");
                };
            }
        }
    }
}

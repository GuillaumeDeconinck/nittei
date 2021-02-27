use crate::shared::usecase::{execute, UseCase};
use crate::{
    error::NettuError,
    shared::auth::{ensure_nettu_acct_header, protect_account_route},
};
use actix_web::{web, HttpRequest, HttpResponse};
use futures::future::join_all;
use nettu_scheduler_core::{
    booking_slots::{
        get_service_bookingslots, validate_bookingslots_query, validate_slots_interval,
        BookingQueryError, BookingSlotsOptions, BookingSlotsQuery, ServiceBookingSlot,
        ServiceBookingSlotDTO, UserFreeEvents,
    },
    get_free_busy, Calendar, CalendarView, EventInstance, ServiceResource, TimePlan,
};
use nettu_scheduler_infra::NettuContext;
use serde::{Deserialize, Serialize};

#[derive(Debug, Deserialize)]
pub struct PathParams {
    service_id: String,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QueryParams {
    iana_tz: Option<String>,
    duration: i64,
    interval: i64,
    date: String,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct APIRes {
    booking_slots: Vec<ServiceBookingSlotDTO>,
}

pub async fn get_service_bookingslots_controller(
    http_req: HttpRequest,
    query_params: web::Query<QueryParams>,
    path_params: web::Path<PathParams>,
    ctx: web::Data<NettuContext>,
) -> Result<HttpResponse, NettuError> {
    match ensure_nettu_acct_header(&http_req) {
        Ok(_) => (),
        Err(e) => match protect_account_route(&http_req, &ctx).await {
            Ok(_) => (),
            Err(_) => return Err(e),
        },
    };

    let usecase = GetServiceBookingSlotsUseCase {
        service_id: path_params.service_id.clone(),
        iana_tz: query_params.iana_tz.clone(),
        date: query_params.date.clone(),
        duration: query_params.duration,
        interval: query_params.interval,
    };

    execute(usecase, &ctx).await
        .map(|usecase_res| {
            let res = APIRes {
                booking_slots: usecase_res
                    .booking_slots
                    .iter()
                    .map(|slot| ServiceBookingSlotDTO::new(slot))
                    .collect(),
            };
            HttpResponse::Ok().json(res)
        })
        .map_err(|e| match e {
            UseCaseErrors::InvalidDate(msg) => {
                NettuError::BadClientData(format!(
                    "Invalid datetime: {}. Should be YYYY-MM-DD, e.g. January 1. 2020 => 2020-1-1",
                    msg
                ))
            }
            UseCaseErrors::InvalidTimezone(msg) => {
                NettuError::BadClientData(format!(
                    "Invalid timezone: {}. It should be a valid IANA TimeZone.",
                    msg
                ))
            }
            UseCaseErrors::InvalidInterval => {
                NettuError::BadClientData(
                    "Invalid interval specified. It should be between 10 - 60 minutes inclusively and be specified as milliseconds.".into()
                )
            }
            UseCaseErrors::InvalidTimespan => {
                NettuError::BadClientData("The provided start_ts and end_ts is invalid".into())
            }
            UseCaseErrors::ServiceNotFound => NettuError::NotFound(format!("Service with id: {}, was not found.", path_params.service_id)),
        })
}

struct GetServiceBookingSlotsUseCase {
    pub service_id: String,
    pub date: String,
    pub iana_tz: Option<String>,
    pub duration: i64,
    pub interval: i64,
}

struct UseCaseRes {
    booking_slots: Vec<ServiceBookingSlot>,
}

#[derive(Debug)]
enum UseCaseErrors {
    ServiceNotFound,
    InvalidInterval,
    InvalidTimespan,
    InvalidDate(String),
    InvalidTimezone(String),
}

#[async_trait::async_trait(?Send)]
impl UseCase for GetServiceBookingSlotsUseCase {
    type Response = UseCaseRes;

    type Errors = UseCaseErrors;

    type Context = NettuContext;

    async fn execute(&mut self, ctx: &Self::Context) -> Result<Self::Response, Self::Errors> {
        if !validate_slots_interval(self.interval) {
            return Err(UseCaseErrors::InvalidInterval);
        }

        let query = BookingSlotsQuery {
            date: self.date.clone(),
            iana_tz: self.iana_tz.clone(),
            interval: self.interval,
            duration: self.duration,
        };
        let booking_timespan = match validate_bookingslots_query(&query) {
            Ok(t) => t,
            Err(e) => match e {
                BookingQueryError::InvalidIntervalError => {
                    return Err(UseCaseErrors::InvalidInterval)
                }
                BookingQueryError::InvalidDateError(d) => {
                    return Err(UseCaseErrors::InvalidDate(d))
                }
                BookingQueryError::InvalidTimezoneError(d) => {
                    return Err(UseCaseErrors::InvalidTimezone(d))
                }
            },
        };

        let service = match ctx.repos.service_repo.find(&self.service_id).await {
            Some(s) => s,
            None => return Err(UseCaseErrors::ServiceNotFound),
        };

        let mut usecase_futures: Vec<_> = Vec::with_capacity(service.users.len());

        let view = match CalendarView::create(booking_timespan.start_ts, booking_timespan.end_ts) {
            Ok(view) => view,
            Err(_) => return Err(UseCaseErrors::InvalidTimespan),
        };

        for user in &service.users {
            let view = view.clone();
            usecase_futures.push(self.get_user_freebusy(user, view, ctx));
        }

        let users_free_events = join_all(usecase_futures).await;

        let booking_slots = get_service_bookingslots(
            users_free_events,
            &BookingSlotsOptions {
                interval: self.interval,
                duration: self.duration,
                end_ts: booking_timespan.end_ts,
                start_ts: booking_timespan.start_ts,
            },
        );

        Ok(UseCaseRes { booking_slots })
    }
}

impl GetServiceBookingSlotsUseCase {
    async fn get_user_availibility(
        &self,
        user: &ServiceResource,
        user_calendars: &Vec<Calendar>,
        view: &CalendarView,
        ctx: &NettuContext,
    ) -> Vec<EventInstance> {
        match &user.availibility {
            TimePlan::Calendar(id) => {
                let calendar = match user_calendars.iter().find(|cal| cal.id == *id) {
                    Some(cal) => cal,
                    None => {
                        return vec![];
                    }
                };
                let all_calendar_events = ctx
                    .repos
                    .event_repo
                    .find_by_calendar(&id, Some(&view))
                    .await
                    .unwrap_or(vec![]);

                let mut all_event_instances = all_calendar_events
                    .iter()
                    .map(|e| e.expand(Some(&view), &calendar.settings))
                    .flatten()
                    .collect::<Vec<_>>();

                get_free_busy(&mut all_event_instances).free
            }
            TimePlan::Schedule(id) => match ctx.repos.schedule_repo.find(&id).await {
                Some(schedule) if schedule.user_id == user.id => schedule.freebusy(&view),
                _ => vec![],
            },
            TimePlan::Empty => vec![],
        }
    }

    async fn get_user_busy(
        &self,
        user: &ServiceResource,
        busy_calendars: &Vec<&Calendar>,
        view: &CalendarView,
        ctx: &NettuContext,
    ) -> Vec<EventInstance> {
        let mut busy_events: Vec<EventInstance> = vec![];
        for cal in busy_calendars {
            match ctx
                .repos
                .event_repo
                .find_by_calendar(&cal.id, Some(&view))
                .await
            {
                Ok(calendar_events) => {
                    let mut calendar_busy_events = calendar_events
                        .into_iter()
                        .filter(|e| e.busy)
                        .map(|e| {
                            let mut instances = e.expand(Some(&view), &cal.settings);
                            let is_service_event = e.services.contains(&String::from("*"))
                                || e.services.contains(&self.service_id);

                            // Add buffer to instances if event is a service event
                            if user.buffer > 0 && is_service_event {
                                let buffer_in_millis = user.buffer * 60 * 1000;
                                for instance in instances.iter_mut() {
                                    instance.end_ts += buffer_in_millis;
                                }
                            }

                            instances
                        })
                        .flatten()
                        .collect::<Vec<_>>();

                    busy_events.append(&mut calendar_busy_events);
                }
                Err(_) => {}
            }
        }

        busy_events
    }

    /// Ensure that calendar view fits within user settings for when
    /// it should be bookable
    fn parse_calendar_view(
        user: &ServiceResource,
        mut view: CalendarView,
        ctx: &NettuContext,
    ) -> Result<CalendarView, ()> {
        let first_available =
            ctx.sys.get_timestamp_millis() + user.closest_booking_time * 60 * 1000;
        if view.get_start() < first_available {
            view = match CalendarView::create(first_available, view.get_end()) {
                Ok(view) => view,
                Err(_) => return Err(()),
            }
        }
        if let Some(furthest_booking_time) = user.furthest_booking_time {
            let last_available = furthest_booking_time * 60 * 1000 + ctx.sys.get_timestamp_millis();
            if last_available < view.get_end() {
                view = match CalendarView::create(view.get_start(), last_available) {
                    Ok(view) => view,
                    Err(_) => return Err(()),
                }
            }
        }

        Ok(view)
    }

    async fn get_user_freebusy(
        &self,
        user: &ServiceResource,
        mut view: CalendarView,
        ctx: &NettuContext,
    ) -> UserFreeEvents {
        let empty = UserFreeEvents {
            free_events: vec![],
            user_id: user.id.clone(),
        };

        match Self::parse_calendar_view(user, view, ctx) {
            Ok(parsed_view) => view = parsed_view,
            Err(_) => return empty,
        }

        let user_calendars = ctx.repos.calendar_repo.find_by_user(&user.user_id).await;
        let busy_calendars = user_calendars
            .iter()
            .filter(|cal| user.busy.contains(&cal.id))
            .collect::<Vec<_>>();

        let mut free_events = self
            .get_user_availibility(user, &user_calendars, &view, ctx)
            .await;

        let mut busy_events = self.get_user_busy(user, &busy_calendars, &view, ctx).await;

        let mut all_events = Vec::with_capacity(free_events.len() + busy_events.len());
        all_events.append(&mut free_events);
        free_events.append(&mut busy_events);

        UserFreeEvents {
            free_events: get_free_busy(&mut all_events).free,
            user_id: user.id.clone(),
        }
    }
}

#[cfg(test)]
mod test {
    use std::sync::Arc;

    use super::*;
    use chrono::prelude::*;
    use chrono::Utc;
    use nettu_scheduler_core::{Calendar, CalendarEvent, RRuleOptions, Service, ServiceResource};
    use nettu_scheduler_infra::{setup_context, ISys};

    struct TestContext {
        ctx: NettuContext,
        service: Service,
    }

    struct DummySys {}

    impl ISys for DummySys {
        fn get_timestamp_millis(&self) -> i64 {
            0
        }
    }

    async fn setup() -> TestContext {
        let mut ctx = setup_context().await;
        ctx.sys = Arc::new(DummySys {});

        let service = Service::new("123".into());
        ctx.repos.service_repo.insert(&service).await.unwrap();

        TestContext { ctx, service }
    }

    async fn setup_service_users(ctx: &NettuContext, service: &mut Service) {
        let mut resource1 = ServiceResource {
            id: "1".into(),
            user_id: "1".into(),
            buffer: 0,
            availibility: TimePlan::Empty,
            busy: vec![],
            closest_booking_time: 0,
            furthest_booking_time: None,
        };
        let mut resource2 = ServiceResource {
            id: "2".into(),
            user_id: "2".into(),
            buffer: 0,
            availibility: TimePlan::Empty,
            busy: vec![],
            closest_booking_time: 0,
            furthest_booking_time: None,
        };

        let calendar_user_1 = Calendar::new(&resource1.user_id);
        resource1.availibility = TimePlan::Calendar(calendar_user_1.id.clone());
        let calendar_user_2 = Calendar::new(&resource2.user_id);
        resource2.availibility = TimePlan::Calendar(calendar_user_2.id.clone());

        ctx.repos
            .calendar_repo
            .insert(&calendar_user_1)
            .await
            .unwrap();
        ctx.repos
            .calendar_repo
            .insert(&calendar_user_2)
            .await
            .unwrap();

        let availibility_event1 = CalendarEvent {
            busy: false,
            calendar_id: calendar_user_1.id,
            duration: 1000 * 60 * 60,
            end_ts: 0,
            exdates: vec![],
            id: "1".into(),
            recurrence: None,
            start_ts: 1000 * 60 * 60,
            account_id: "1".into(),
            user_id: resource1.user_id.to_owned(),
            reminder: None,
            services: vec![],
        };
        let availibility_event2 = CalendarEvent {
            busy: false,
            calendar_id: calendar_user_2.id.clone(),
            duration: 1000 * 60 * 60,
            end_ts: 0,
            exdates: vec![],
            id: "2".into(),
            recurrence: None,
            start_ts: 1000 * 60 * 60,
            account_id: "1".into(),
            user_id: resource2.user_id.to_owned(),
            reminder: None,
            services: vec![],
        };
        let mut availibility_event3 = CalendarEvent {
            busy: false,
            calendar_id: calendar_user_2.id,
            duration: 1000 * 60 * 105,
            end_ts: 0,
            exdates: vec![],
            id: "3".into(),
            recurrence: None,
            start_ts: 1000 * 60 * 60 * 4,
            user_id: resource1.user_id.to_owned(),
            account_id: "1".into(),
            reminder: None,
            services: vec![],
        };
        let recurrence = RRuleOptions {
            ..Default::default()
        };
        availibility_event3.set_recurrence(recurrence, &calendar_user_2.settings, true);

        ctx.repos
            .event_repo
            .insert(&availibility_event1)
            .await
            .unwrap();
        ctx.repos
            .event_repo
            .insert(&availibility_event2)
            .await
            .unwrap();
        ctx.repos
            .event_repo
            .insert(&availibility_event3)
            .await
            .unwrap();

        service.add_user(resource1);
        service.add_user(resource2);
        ctx.repos.service_repo.save(&service).await.unwrap();
    }

    #[actix_web::main]
    #[test]
    async fn get_service_bookingslots() {
        let TestContext { ctx, service } = setup().await;

        let mut usecase = GetServiceBookingSlotsUseCase {
            date: "2010-1-1".into(),
            duration: 1000 * 60 * 60,
            iana_tz: Utc.to_string().into(),
            interval: 1000 * 60 * 15,
            service_id: service.id,
        };

        let res = usecase.execute(&ctx).await;
        assert!(res.is_ok());
        assert!(res.unwrap().booking_slots.is_empty());
    }

    #[actix_web::main]
    #[test]
    async fn get_bookingslots_with_multiple_users_in_service() {
        let TestContext { ctx, mut service } = setup().await;
        setup_service_users(&ctx, &mut service).await;

        let mut usecase = GetServiceBookingSlotsUseCase {
            date: "2010-1-1".into(),
            duration: 1000 * 60 * 60,
            iana_tz: Utc.to_string().into(),
            interval: 1000 * 60 * 15,
            service_id: service.id.clone(),
        };

        let res = usecase.execute(&ctx).await;
        assert!(res.is_ok());
        let booking_slots = res.unwrap().booking_slots;
        assert_eq!(booking_slots.len(), 4);
        for i in 0..4 {
            assert_eq!(booking_slots[i].duration, usecase.duration);
            assert_eq!(booking_slots[i].user_ids, vec!["2"]);
            assert_eq!(
                booking_slots[i].start,
                Utc.ymd(2010, 1, 1)
                    .and_hms(4, 15 * i as u32, 0)
                    .timestamp_millis()
            );
        }

        let mut usecase = GetServiceBookingSlotsUseCase {
            date: "1970-1-1".into(),
            duration: 1000 * 60 * 60,
            iana_tz: Utc.to_string().into(),
            interval: 1000 * 60 * 15,
            service_id: service.id,
        };

        let res = usecase.execute(&ctx).await;
        assert!(res.is_ok());
        let booking_slots = res.unwrap().booking_slots;
        assert_eq!(booking_slots.len(), 5);
        assert_eq!(booking_slots[0].user_ids, vec!["1", "2"]);
        for i in 0..5 {
            assert_eq!(booking_slots[i].duration, usecase.duration);
            if i > 0 {
                assert_eq!(booking_slots[i].user_ids, vec!["2"]);
                assert_eq!(
                    booking_slots[i].start,
                    Utc.ymd(1970, 1, 1)
                        .and_hms(4, 15 * (i - 1) as u32, 0)
                        .timestamp_millis()
                );
            }
        }
    }
}

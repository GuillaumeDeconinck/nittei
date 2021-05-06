use crate::{
    shared::entity::{Entity, ID},
    Meta, Metadata,
};
use serde::{Deserialize, Serialize};

/// A type that describes a time plan and is either a `Calendar` or a `Schedule`
#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
#[serde(tag = "variant", content = "id")]
pub enum TimePlan {
    /// Calendar id
    Calendar(ID),
    /// Schedule id
    Schedule(ID),
    // No plan
    Empty,
}

/// A bookable `User` registered on a `Service`
#[derive(Clone, Debug, Serialize)]
pub struct ServiceResource {
    pub id: ID,
    /// Id of the `User` registered on this `Service`
    pub user_id: ID,
    /// Every available event in a `Calendar` or a `Shedule` in this field
    /// describes the time when this `ServiceResource` will be bookable.
    /// Note: If there are busy `CalendarEvent`s in the `Calendar` then the user
    /// will not be bookable during that time.
    pub availibility: TimePlan,
    /// List of `Calendar` ids that should be subtracted from the availibility
    /// time plan.
    pub busy: Vec<BusyCalendar>,
    /// This `ServiceResource` will not be bookable this amount of *minutes*
    /// after a meeting. A `CalendarEvent` will be interpreted as a meeting
    /// if the attribute `services` on the `CalendarEvent` includes this
    /// `Service` id or "*".
    pub buffer: i64,
    /// Minimum amount of time in minutes before this user could receive any
    /// booking requests. That means that if a bookingslots query is made at
    /// time T then this `ServiceResource` will not have any availaible
    /// bookingslots before at least T + `closest_booking_time`
    pub closest_booking_time: i64,
    /// Amount of time in minutes into the future after which the user can not receive any
    /// booking requests. This is useful to ensure that booking requests are not made multiple
    /// years into the future. That means that if a bookingslots query is made at
    /// time T then this `ServiceResource` will not have any availaible
    /// bookingslots after T + `furthest_booking_time`
    pub furthest_booking_time: Option<i64>,
}

impl ServiceResource {
    pub fn new(user_id: ID, availibility: TimePlan, busy: Vec<BusyCalendar>) -> Self {
        Self {
            id: Default::default(),
            user_id,
            availibility,
            busy,
            buffer: 0,
            closest_booking_time: 0,
            furthest_booking_time: None,
        }
    }

    pub fn set_availibility(&mut self, availibility: TimePlan) {
        self.availibility = availibility;
    }

    pub fn set_busy(&mut self, busy: Vec<BusyCalendar>) {
        self.busy = busy;
    }

    pub fn set_buffer(&mut self, buffer: i64) -> bool {
        let min_buffer = 0;
        let max_buffer = 60 * 12; // 12 Hours
        if buffer < min_buffer || buffer > max_buffer {
            return false;
        }
        self.buffer = buffer;
        true
    }

    // pub fn get_calendar_ids(&self) -> Vec<ID> {
    //     let mut calendar_ids = self.busy.clone();

    //     if let TimePlan::Calendar(id) = &self.availibility {
    //         calendar_ids.push(id.clone());
    //     }

    //     calendar_ids
    // }

    pub fn get_schedule_id(&self) -> Option<ID> {
        match &self.availibility {
            TimePlan::Schedule(id) => Some(id.clone()),
            _ => None,
        }
    }

    pub fn contains_calendar(&self, calendar_id: &str) -> bool {
        match &self.availibility {
            TimePlan::Calendar(id) if id.to_string() == calendar_id => {
                return true;
            }
            _ => (),
        }

        for busy in &self.busy {
            match busy {
                BusyCalendar::Nettu(id) if id.to_string() == calendar_id => return true,
                BusyCalendar::Google(id) if id == calendar_id => return true,
                _ => (),
            }
        }

        false
    }

    pub fn remove_calendar(&mut self, calendar_id: &str) {
        match &self.availibility {
            TimePlan::Calendar(id) if id.to_string() == calendar_id => {
                self.availibility = TimePlan::Empty;
            }
            _ => (),
        }

        self.busy.retain(|busy_cal| match busy_cal {
            BusyCalendar::Nettu(cal_id) => cal_id.to_string() != calendar_id,
            BusyCalendar::Google(cal_id) => cal_id != calendar_id,
            _ => true,
        });
    }

    pub fn contains_schedule(&self, schedule_id: &ID) -> bool {
        matches!(&self.availibility, TimePlan::Schedule(id) if id == schedule_id)
    }

    pub fn remove_schedule(&mut self, schedule_id: &ID) {
        match &self.availibility {
            TimePlan::Schedule(id) if id == schedule_id => {
                self.availibility = TimePlan::Empty;
            }
            _ => (),
        }
    }
}

#[derive(Clone, Debug)]
pub struct Service {
    pub id: ID,
    pub account_id: ID,
    // interval: usize,
    // allow_more_booking_requests_in_queue_than_resources
    pub users: Vec<ServiceResource>,
    pub metadata: Metadata,
}

impl Entity for Service {
    fn id(&self) -> &ID {
        &self.id
    }
}

impl Meta for Service {
    fn metadata(&self) -> &Metadata {
        &self.metadata
    }
    fn account_id(&self) -> &ID {
        &self.account_id
    }
}

impl Service {
    pub fn new(account_id: ID) -> Self {
        Self {
            id: Default::default(),
            account_id,
            users: Default::default(),
            metadata: Default::default(),
        }
    }

    pub fn add_user(&mut self, user: ServiceResource) {
        self.users.push(user);
    }

    pub fn remove_user(&mut self, user_id: &ID) -> Option<ServiceResource> {
        for (pos, user) in self.users.iter().enumerate() {
            if user.user_id == *user_id {
                return Some(self.users.remove(pos));
            }
        }
        None
    }

    pub fn find_user(&self, user_id: &ID) -> Option<&ServiceResource> {
        self.users.iter().find(|u| u.user_id == *user_id)
    }

    pub fn find_user_mut(&mut self, user_id: &ID) -> Option<&mut ServiceResource> {
        self.users.iter_mut().find(|u| u.user_id == *user_id)
    }
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(tag = "provider", content = "id")]
pub enum BusyCalendar {
    Google(String),
    Nettu(ID),
}

use super::ICalendarRepo;
use crate::repos::shared::{
    inmemory_repo::*, query_structs::MetadataFindQuery, repo::DeleteResult,
};
use nettu_scheduler_domain::{Calendar, ID};

pub struct InMemoryCalendarRepo {
    calendars: std::sync::Mutex<Vec<Calendar>>,
}

impl InMemoryCalendarRepo {
    pub fn new() -> Self {
        Self {
            calendars: std::sync::Mutex::new(Vec::new()),
        }
    }
}

#[async_trait::async_trait]
impl ICalendarRepo for InMemoryCalendarRepo {
    async fn insert(&self, calendar: &Calendar) -> anyhow::Result<()> {
        insert(calendar, &self.calendars);
        Ok(())
    }

    async fn save(&self, calendar: &Calendar) -> anyhow::Result<()> {
        save(calendar, &self.calendars);
        Ok(())
    }

    async fn find(&self, calendar_id: &ID) -> Option<Calendar> {
        find(calendar_id, &self.calendars)
    }

    async fn find_by_user(&self, user_id: &ID) -> Vec<Calendar> {
        find_by(&self.calendars, |cal| cal.user_id == *user_id)
    }

    async fn delete(&self, calendar_id: &ID) -> Option<Calendar> {
        delete(calendar_id, &self.calendars)
    }

    async fn delete_by_user(&self, user_id: &ID) -> anyhow::Result<DeleteResult> {
        let res = delete_by(&self.calendars, |cal| cal.user_id == *user_id);
        Ok(res)
    }

    async fn find_by_metadata(&self, query: MetadataFindQuery) -> Vec<Calendar> {
        find_by_metadata(&self.calendars, query)
    }
}

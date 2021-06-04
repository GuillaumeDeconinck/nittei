mod account;
mod calendar;
mod event;
mod schedule;
mod service;
mod shared;
mod user;

use account::{IAccountRepo, InMemoryAccountRepo, MongoAccountRepo, PostgresAccountRepo};
use calendar::{ICalendarRepo, InMemoryCalendarRepo, MongoCalendarRepo, PostgresCalendarRepo};
use event::{
    IEventRemindersExpansionJobsRepo, IEventRepo, IReminderRepo,
    InMemoryEventRemindersExpansionJobsRepo, InMemoryEventRepo, InMemoryReminderRepo,
    MongoEventRemindersExpansionsJobRepo, MongoEventRepo, MongoReminderRepo,
};
use mongodb::{options::ClientOptions, Client};
use schedule::{IScheduleRepo, InMemoryScheduleRepo, MongoScheduleRepo};
use service::{IServiceRepo, InMemoryServiceRepo, MongoServiceRepo};
use sqlx::postgres::PgPoolOptions;
use std::sync::Arc;
use tracing::info;
use user::{IUserRepo, InMemoryUserRepo, MongoUserRepo};

pub use mongodb::bson::oid::ObjectId;
pub use shared::query_structs::*;

#[derive(Clone)]
pub struct Repos {
    pub event_repo: Arc<dyn IEventRepo>,
    pub calendar_repo: Arc<dyn ICalendarRepo>,
    pub account_repo: Arc<dyn IAccountRepo>,
    pub user_repo: Arc<dyn IUserRepo>,
    pub service_repo: Arc<dyn IServiceRepo>,
    pub schedule_repo: Arc<dyn IScheduleRepo>,
    pub reminder_repo: Arc<dyn IReminderRepo>,
    pub event_reminders_expansion_jobs_repo: Arc<dyn IEventRemindersExpansionJobsRepo>,
}

impl Repos {
    pub async fn create_mongodb(
        connection_string: &str,
        db_name: &str,
    ) -> Result<Self, Box<dyn std::error::Error>> {
        let client_options = ClientOptions::parse(connection_string).await?;
        let client = Client::with_options(client_options)?;
        let db = client.database(db_name);

        // This is needed to make sure that db is ready when opening server
        info!("DB CHECKING CONNECTION ...");
        db.collection("server-start")
            .insert_one(
                mongodb::bson::doc! {
                "server-start": 1
                },
                None,
            )
            .await?;
        info!("DB CHECKING CONNECTION ... [done]");
        Ok(Self {
            event_repo: Arc::new(MongoEventRepo::new(&db)),
            calendar_repo: Arc::new(MongoCalendarRepo::new(&db)),
            account_repo: Arc::new(MongoAccountRepo::new(&db)),
            user_repo: Arc::new(MongoUserRepo::new(&db)),
            service_repo: Arc::new(MongoServiceRepo::new(&db)),
            schedule_repo: Arc::new(MongoScheduleRepo::new(&db)),
            reminder_repo: Arc::new(MongoReminderRepo::new(&db)),
            event_reminders_expansion_jobs_repo: Arc::new(
                MongoEventRemindersExpansionsJobRepo::new(&db),
            ),
        })
    }

    pub async fn create_inmemory() -> Self {
        let pool = PgPoolOptions::new()
            .max_connections(5)
            .connect("postgresql://localhost:5432/tester")
            .await
            .expect("TO CONNECT TO POSTGRES");

        Self {
            event_repo: Arc::new(InMemoryEventRepo::new()),
            // calendar_repo: Arc::new(InMemoryCalendarRepo::new()),
            calendar_repo: Arc::new(PostgresCalendarRepo::new(pool.clone())),
            // account_repo: Arc::new(InMemoryAccountRepo::new()),
            account_repo: Arc::new(PostgresAccountRepo::new(pool)),
            user_repo: Arc::new(InMemoryUserRepo::new()),
            service_repo: Arc::new(InMemoryServiceRepo::new()),
            schedule_repo: Arc::new(InMemoryScheduleRepo::new()),
            reminder_repo: Arc::new(InMemoryReminderRepo::new()),
            event_reminders_expansion_jobs_repo: Arc::new(
                InMemoryEventRemindersExpansionJobsRepo::new(),
            ),
        }
    }
}

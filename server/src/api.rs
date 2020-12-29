use crate::{
    calendar::repos::{CalendarRepo, ICalendarRepo},
    event::repos::{EventRepo, IEventRepo},
};
use actix_web::{web, HttpResponse};
use mongodb::{options::ClientOptions, Client};

use std::sync::Arc;

pub struct Repos {
    pub event_repo: Arc<dyn IEventRepo>,
    pub calendar_repo: Arc<dyn ICalendarRepo>,
}

impl Repos {
    pub async fn create() -> Result<Self, Box<dyn std::error::Error>> {
        let client_options =
            ClientOptions::parse(&std::env::var("MONGODB_CONNECTION_STRING").unwrap()).await?;
        let client = Client::with_options(client_options)?;
        let db = client.database(&std::env::var("MONGODB_NAME").unwrap());

        // This is needed to make sure that db is ready when opening server
        println!("DB CHECKING CONNECTION ...");
        db.collection("server-start")
            .insert_one(
                mongodb::bson::doc! {
                "server-start": 1
                },
                None,
            )
            .await?;
        println!("DB CHECKING CONNECTION ... [done]");
        Ok(Self {
            event_repo: Arc::new(EventRepo::new(&db)),
            calendar_repo: Arc::new(CalendarRepo::new(&db)),
        })
    }
}

impl Clone for Repos {
    fn clone(&self) -> Self {
        Self {
            event_repo: Arc::clone(&self.event_repo),
            calendar_repo: Arc::clone(&self.calendar_repo),
        }
    }
}

pub struct Context {
    pub repos: Repos,
}

#[async_trait::async_trait(?Send)]
pub trait Perform {
    // type Response: serde::ser::Serialize + Send;

    async fn perform(&self, context: &web::Data<Context>) -> HttpResponse;
}

#[derive(Debug)]
pub struct NettuError {
    pub inner: anyhow::Error,
}

impl<T> From<T> for NettuError
where
    T: Into<anyhow::Error>,
{
    fn from(t: T) -> Self {
        NettuError { inner: t.into() }
    }
}

impl std::fmt::Display for NettuError {
    fn fmt(&self, f: &mut std::fmt::Formatter) -> std::fmt::Result {
        self.inner.fmt(f)
    }
}

impl actix_web::error::ResponseError for NettuError {}

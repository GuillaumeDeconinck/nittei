use super::IServiceRepo;
use crate::shared::mongo_repo;
use mongo_repo::MongoDocument;
use mongodb::{
    bson::{doc, oid::ObjectId, Document},
    Collection, Database,
};
use nettu_scheduler_core::{Service, ServiceResource};
use serde::{Deserialize, Serialize};
use std::error::Error;

pub struct ServiceRepo {
    collection: Collection,
}

impl ServiceRepo {
    pub fn new(db: &Database) -> Self {
        Self {
            collection: db.collection("services"),
        }
    }
}

#[async_trait::async_trait]
impl IServiceRepo for ServiceRepo {
    async fn insert(&self, service: &Service) -> Result<(), Box<dyn Error>> {
        match mongo_repo::insert::<_, ServiceMongo>(&self.collection, service).await {
            Ok(_) => Ok(()),
            Err(_) => Ok(()), // fix this
        }
    }

    async fn save(&self, service: &Service) -> Result<(), Box<dyn Error>> {
        match mongo_repo::save::<_, ServiceMongo>(&self.collection, service).await {
            Ok(_) => Ok(()),
            Err(_) => Ok(()), // fix this
        }
    }

    async fn find(&self, service_id: &str) -> Option<Service> {
        let id = match ObjectId::with_string(service_id) {
            Ok(oid) => mongo_repo::MongoPersistenceID::ObjectId(oid),
            Err(_) => return None,
        };
        mongo_repo::find::<_, ServiceMongo>(&self.collection, &id).await
    }

    async fn delete(&self, service_id: &str) -> Option<Service> {
        let id = match ObjectId::with_string(service_id) {
            Ok(oid) => mongo_repo::MongoPersistenceID::ObjectId(oid),
            Err(_) => return None,
        };
        mongo_repo::delete::<_, ServiceMongo>(&self.collection, &id).await
    }

    async fn remove_calendar_from_services(&self, calendar_id: &str) -> Result<(), Box<dyn Error>> {
        let filter = doc! {
            "attributes": {
                "key": "calendars",
                "value": calendar_id
            }
        };
        let update = doc! {
            "attributes.value": {
                "$pull": calendar_id
            },
            "users.calendar_ids": {
                "$pull": calendar_id
            }
        };
        mongo_repo::update_many::<_, ServiceMongo>(&self.collection, filter, update).await
    }

    async fn remove_schedule_from_services(&self, schedule_id: &str) -> Result<(), Box<dyn Error>> {
        let filter = doc! {
            "attributes": {
                "key": "schedules",
                "value": schedule_id
            }
        };
        let update = doc! {
            "attributes.value": {
                "$pull": schedule_id
            },
            "users.schedule_ids": {
                "$pull": schedule_id
            }
        };
        mongo_repo::update_many::<_, ServiceMongo>(&self.collection, filter, update).await
    }
}

#[derive(Debug, Serialize, Deserialize)]
struct ServiceResourceMongo {
    pub _id: ObjectId,
    pub user_id: String,
    pub calendar_ids: Vec<String>,
    pub schedule_ids: Vec<String>,
}

#[derive(Debug, Serialize, Deserialize)]
struct DocumentAttribute {
    pub key: String,
    pub value: Vec<String>,
}

#[derive(Debug, Serialize, Deserialize)]
struct ServiceMongo {
    pub _id: ObjectId,
    pub account_id: String,
    pub users: Vec<ServiceResourceMongo>,
    pub attributes: Vec<DocumentAttribute>,
}

impl MongoDocument<Service> for ServiceMongo {
    fn to_domain(&self) -> Service {
        Service {
            id: self._id.to_string(),
            account_id: self.account_id.clone(),
            users: self
                .users
                .iter()
                .map(|user| ServiceResource {
                    id: user._id.to_string(),
                    user_id: user.user_id.clone(),
                    calendar_ids: user.calendar_ids.clone(),
                    schedule_ids: user.schedule_ids.clone(),
                })
                .collect(),
        }
    }

    fn from_domain(service: &Service) -> Self {
        Self {
            _id: ObjectId::with_string(&service.id).unwrap(),
            account_id: service.account_id.clone(),
            users: service
                .users
                .iter()
                .map(|user| ServiceResourceMongo {
                    _id: ObjectId::with_string(&user.id).unwrap(),
                    user_id: user.user_id.clone(),
                    calendar_ids: user.calendar_ids.clone(),
                    schedule_ids: user.schedule_ids.clone(),
                })
                .collect(),
            attributes: vec![
                DocumentAttribute {
                    key: "calendars".into(),
                    value: service
                        .users
                        .iter()
                        .map(|u| u.calendar_ids.clone())
                        .flatten()
                        .collect(),
                },
                DocumentAttribute {
                    key: "schedules".into(),
                    value: service
                        .users
                        .iter()
                        .map(|u| u.schedule_ids.clone())
                        .flatten()
                        .collect(),
                },
            ],
        }
    }

    fn get_id_filter(&self) -> Document {
        doc! {
            "_id": self._id.clone()
        }
    }
}
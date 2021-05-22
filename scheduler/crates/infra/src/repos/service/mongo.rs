use super::IServiceRepo;
use crate::{
    repos::shared::{
        mongo_repo::{self},
        query_structs::MetadataFindQuery,
    },
    KVMetadata,
};
use mongo_repo::MongoDocument;
use mongodb::{
    bson::{doc, oid::ObjectId, Document},
    Collection, Database,
};
use nettu_scheduler_domain::{BusyCalendar, Service, ServiceResource, TimePlan, ID};
use serde::{Deserialize, Serialize};

pub struct MongoServiceRepo {
    collection: Collection,
}

impl MongoServiceRepo {
    pub fn new(db: &Database) -> Self {
        Self {
            collection: db.collection("services"),
        }
    }
}

#[async_trait::async_trait]
impl IServiceRepo for MongoServiceRepo {
    async fn insert(&self, service: &Service) -> anyhow::Result<()> {
        mongo_repo::insert::<_, ServiceMongo>(&self.collection, service).await
    }

    async fn save(&self, service: &Service) -> anyhow::Result<()> {
        mongo_repo::save::<_, ServiceMongo>(&self.collection, service).await
    }

    async fn find(&self, service_id: &ID) -> Option<Service> {
        let oid = service_id.inner_ref();
        mongo_repo::find::<_, ServiceMongo>(&self.collection, &oid).await
    }

    async fn delete(&self, service_id: &ID) -> Option<Service> {
        let oid = service_id.inner_ref();
        mongo_repo::delete::<_, ServiceMongo>(&self.collection, &oid).await
    }

    async fn remove_calendar_from_services(&self, calendar_id: &str) -> anyhow::Result<()> {
        let filter = doc! {
            "ids": calendar_id
        };

        let mut services =
            mongo_repo::find_many_by::<_, ServiceMongo>(&self.collection, filter).await?;
        for service in &mut services {
            for user in &mut service.users {
                if let TimePlan::Calendar(id) = &user.availibility {
                    if id.to_string() == calendar_id {
                        user.availibility = TimePlan::Empty;
                    }
                }
                user.busy.retain(|busy_calendar_id| match busy_calendar_id {
                    BusyCalendar::Google(id) => *id != calendar_id,
                    BusyCalendar::Nettu(id) => id.to_string() != calendar_id,
                });
            }
            mongo_repo::save::<_, ServiceMongo>(&self.collection, service).await?;
        }
        Ok(())
    }

    async fn remove_schedule_from_services(&self, schedule_id: &ID) -> anyhow::Result<()> {
        let filter = doc! {
            "ids": &schedule_id.inner_ref()
        };
        let mut services =
            mongo_repo::find_many_by::<_, ServiceMongo>(&self.collection, filter).await?;
        for service in &mut services {
            for user in &mut service.users {
                if let TimePlan::Schedule(id) = &user.availibility {
                    if id == schedule_id {
                        user.availibility = TimePlan::Empty;
                    }
                }
            }
            mongo_repo::save::<_, ServiceMongo>(&self.collection, service).await?;
        }
        Ok(())
    }

    async fn remove_user_from_services(&self, user_id: &ID) -> anyhow::Result<()> {
        let user_id = user_id.inner_ref();
        let filter = doc! {
            "ids": user_id.to_string()
        };
        let update = doc! {
            "$pull": {
                "ids": user_id.to_string(),
                "users": {
                    "user_id": user_id
                }
            }
        };

        mongo_repo::update_many::<_, ServiceMongo>(&self.collection, filter, update).await
    }

    async fn find_by_metadata(&self, query: MetadataFindQuery) -> Vec<Service> {
        mongo_repo::find_by_metadata::<_, ServiceMongo>(&self.collection, query).await
    }
}

#[derive(Debug, Serialize, Deserialize)]
struct ServiceResourceMongo {
    pub _id: ObjectId,
    pub user_id: ObjectId,
    pub availibility: TimePlan,
    pub busy: Vec<BusyCalendar>,
    pub buffer: i64,
    pub closest_booking_time: i64,
    pub furthest_booking_time: Option<i64>,
}

#[derive(Debug, Serialize, Deserialize)]
struct ServiceMongo {
    pub _id: ObjectId,
    pub account_id: ObjectId,
    pub users: Vec<ServiceResourceMongo>,
    pub ids: Vec<String>,
    pub metadata: Vec<KVMetadata>,
}

impl MongoDocument<Service> for ServiceMongo {
    fn to_domain(self) -> Service {
        Service {
            id: ID::from(self._id),
            account_id: ID::from(self.account_id),
            users: self
                .users
                .into_iter()
                .map(|user| ServiceResource {
                    id: ID::from(user._id),
                    user_id: ID::from(user.user_id),
                    availibility: user.availibility,
                    busy: user.busy,
                    buffer: user.buffer,
                    closest_booking_time: user.closest_booking_time,
                    furthest_booking_time: user.furthest_booking_time,
                })
                .collect(),
            metadata: KVMetadata::to_metadata(self.metadata),
        }
    }

    fn from_domain(service: &Service) -> Self {
        Self {
            _id: service.id.inner_ref().clone(),
            account_id: service.account_id.inner_ref().clone(),
            users: service
                .users
                .iter()
                .map(|user| ServiceResourceMongo {
                    _id: user.id.inner_ref().clone(),
                    user_id: user.user_id.inner_ref().clone(),
                    availibility: user.availibility.clone(),
                    busy: user.busy.clone(),
                    buffer: user.buffer,
                    closest_booking_time: user.closest_booking_time,
                    furthest_booking_time: user.furthest_booking_time,
                })
                .collect(),
            metadata: KVMetadata::new(service.metadata.clone()),
            ids: service
                .users
                .iter()
                .map(|u| {
                    let mut ids = u
                        .busy
                        .iter()
                        .map(|busy| match busy {
                            BusyCalendar::Google(id) => id.clone(),
                            BusyCalendar::Nettu(id) => id.to_string(),
                        })
                        .collect::<Vec<_>>();
                    ids.push(u.user_id.to_string());
                    match &u.availibility {
                        TimePlan::Calendar(id) | TimePlan::Schedule(id) => {
                            ids.push(id.to_string());
                        }
                        _ => (),
                    };
                    ids
                })
                .flatten()
                .collect(),
        }
    }

    fn get_id_filter(&self) -> Document {
        doc! {
            "_id": &self._id
        }
    }
}

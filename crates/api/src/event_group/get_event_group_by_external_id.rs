use actix_web::{web, HttpRequest, HttpResponse};
use nittei_api_structs::get_event_group_by_external_id::*;
use nittei_domain::{event_group::EventGroup, ID};
use nittei_infra::NitteiContext;

use crate::{
    error::NitteiError,
    shared::{
        auth::protect_account_route,
        usecase::{execute, UseCase},
    },
};

pub async fn get_event_group_by_external_id_admin_controller(
    http_req: HttpRequest,
    path_params: web::Path<PathParams>,
    ctx: web::Data<NitteiContext>,
) -> Result<HttpResponse, NitteiError> {
    let account = protect_account_route(&http_req, &ctx).await?;

    let usecase = GetEventGroupByExternalIdUseCase {
        account_id: account.id,
        external_id: path_params.external_id.clone(),
    };

    execute(usecase, &ctx)
        .await
        .map(|event| HttpResponse::Ok().json(APIResponse::new(event)))
        .map_err(NitteiError::from)
}

#[derive(Debug)]
pub struct GetEventGroupByExternalIdUseCase {
    pub external_id: String,
    pub account_id: ID,
}

#[derive(Debug)]
pub enum UseCaseError {
    InternalError,
    NotFound(String),
}

impl From<UseCaseError> for NitteiError {
    fn from(e: UseCaseError) -> Self {
        match e {
            UseCaseError::InternalError => Self::InternalError,
            UseCaseError::NotFound(external_id) => Self::NotFound(format!(
                "The event group with external_id: {}, was not found.",
                external_id
            )),
        }
    }
}

#[async_trait::async_trait(?Send)]
impl UseCase for GetEventGroupByExternalIdUseCase {
    type Response = EventGroup;

    type Error = UseCaseError;

    const NAME: &'static str = "GetEvent";

    async fn execute(&mut self, ctx: &NitteiContext) -> Result<Self::Response, Self::Error> {
        let g = ctx
            .repos
            .event_groups
            .get_by_external_id(&self.external_id)
            .await
            .map_err(|_| UseCaseError::InternalError)?;
        match g {
            Some(event_group) if event_group.account_id == self.account_id => Ok(event_group),
            _ => Err(UseCaseError::NotFound(self.external_id.clone())),
        }
    }
}

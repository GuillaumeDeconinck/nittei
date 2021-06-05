use crate::{
    error::NettuError,
    shared::{
        auth::protect_account_route,
        usecase::{execute, UseCase},
    },
};
use actix_web::{web, HttpRequest, HttpResponse};

use nettu_scheduler_api_structs::remove_user_from_service::*;
use nettu_scheduler_domain::{Account, Service, ID};
use nettu_scheduler_infra::NettuContext;

pub async fn remove_user_from_service_controller(
    http_req: HttpRequest,
    path_params: web::Path<PathParams>,
    ctx: web::Data<NettuContext>,
) -> Result<HttpResponse, NettuError> {
    let account = protect_account_route(&http_req, &ctx).await?;

    let usecase = RemoveUserFromServiceUseCase {
        account,
        service_id: path_params.service_id.to_owned(),
        user_id: path_params.user_id.to_owned(),
    };

    execute(usecase, &ctx)
        .await
        .map(|usecase_res| HttpResponse::Ok().json(APIResponse::new(usecase_res.service)))
        .map_err(|e| match e {
            UseCaseErrors::StorageError => NettuError::InternalError,
            UseCaseErrors::ServiceNotFound => {
                NettuError::NotFound("The requested service was not found".to_string())
            }
            UseCaseErrors::UserNotFound => {
                NettuError::NotFound("The specified user was not found in the service".to_string())
            }
        })
}

#[derive(Debug)]
struct RemoveUserFromServiceUseCase {
    pub account: Account,
    pub service_id: ID,
    pub user_id: ID,
}

#[derive(Debug)]
struct UseCaseRes {
    pub service: Service,
}

#[derive(Debug)]
enum UseCaseErrors {
    StorageError,
    ServiceNotFound,
    UserNotFound,
}

#[async_trait::async_trait(?Send)]
impl UseCase for RemoveUserFromServiceUseCase {
    type Response = UseCaseRes;

    type Errors = UseCaseErrors;

    const NAME: &'static str = "RemoveUserFromService";

    async fn execute(&mut self, ctx: &NettuContext) -> Result<Self::Response, Self::Errors> {
        let mut service = match ctx.repos.services.find(&self.service_id).await {
            Some(service) if service.account_id == self.account.id => service,
            _ => return Err(UseCaseErrors::ServiceNotFound),
        };

        match service.remove_user(&self.user_id) {
            Some(_) => match ctx.repos.services.save(&service).await {
                Ok(_) => Ok(UseCaseRes { service }),
                Err(_) => Err(UseCaseErrors::StorageError),
            },
            None => Err(UseCaseErrors::UserNotFound),
        }
    }
}

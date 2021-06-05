use crate::shared::usecase::{execute, UseCase};
use crate::{error::NettuError, shared::auth::protect_account_route};
use actix_web::{web, HttpRequest, HttpResponse};
use nettu_scheduler_api_structs::create_user::*;
use nettu_scheduler_domain::{Metadata, User, ID};
use nettu_scheduler_infra::NettuContext;

pub async fn create_user_controller(
    http_req: HttpRequest,
    body: web::Json<RequestBody>,
    ctx: web::Data<NettuContext>,
) -> Result<HttpResponse, NettuError> {
    let account = protect_account_route(&http_req, &ctx).await?;

    let usecase = CreateUserUseCase {
        account_id: account.id,
        metadata: body.0.metadata.unwrap_or_default(),
    };

    execute(usecase, &ctx)
        .await
        .map(|usecase_res| HttpResponse::Created().json(APIResponse::new(usecase_res.user)))
        .map_err(|e| match e {
            UseCaseErrors::StorageError => NettuError::InternalError,
            UseCaseErrors::UserAlreadyExists => NettuError::Conflict(
                "A user with that userId already exist. UserIds need to be unique.".into(),
            ),
        })
}

#[derive(Debug)]
pub struct CreateUserUseCase {
    pub account_id: ID,
    pub metadata: Metadata,
}

#[derive(Debug)]
pub struct UseCaseRes {
    pub user: User,
}

#[derive(Debug)]
pub enum UseCaseErrors {
    StorageError,
    UserAlreadyExists,
}

#[async_trait::async_trait(?Send)]
impl UseCase for CreateUserUseCase {
    type Response = UseCaseRes;
    type Errors = UseCaseErrors;

    const NAME: &'static str = "CreateUser";

    async fn execute(&mut self, ctx: &NettuContext) -> Result<Self::Response, Self::Errors> {
        let mut user = User::new(self.account_id.clone());
        user.metadata = self.metadata.clone();

        if let Some(_existing_user) = ctx.repos.users.find(&user.id).await {
            return Err(UseCaseErrors::UserAlreadyExists);
        }

        let res = ctx.repos.users.insert(&user).await;
        match res {
            Ok(_) => Ok(UseCaseRes { user }),
            Err(_) => Err(UseCaseErrors::StorageError),
        }
    }
}

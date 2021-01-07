use crate::shared::auth::protect_account_route;
use crate::shared::usecase::{execute, Usecase};
use crate::{
    api::Context,
    user::domain::{User, UserDTO},
};
use actix_web::{web, HttpRequest, HttpResponse};

use serde::Deserialize;

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct BodyParams {
    pub user_id: String,
}

pub async fn create_user_controller(
    http_req: HttpRequest,
    body: web::Json<BodyParams>,
    ctx: web::Data<Context>,
) -> HttpResponse {
    let account = match protect_account_route(&http_req, &ctx).await {
        Ok(a) => a,
        Err(res) => return res,
    };

    let usecase = CreateUserUseCase {
        account_id: account.id.clone(),
        external_user_id: body.user_id.clone(),
    };
    let res = execute(usecase, &ctx.into_inner()).await;

    match res {
        Ok(usecase_res) => {
            let res = UserDTO::new(&usecase_res.user);
            HttpResponse::Created().json(res)
        }
        Err(e) => match e {
            UseCaseErrors::StorageError => HttpResponse::InternalServerError().finish(),
            UseCaseErrors::UserAlreadyExists => HttpResponse::Conflict()
                .body("A user with that userId already exist. UserIds need to be unique."),
        },
    }
}

pub struct CreateUserUseCase {
    pub account_id: String,
    pub external_user_id: String,
}
pub struct UseCaseRes {
    pub user: User,
}

#[derive(Debug)]
pub enum UseCaseErrors {
    StorageError,
    UserAlreadyExists,
}

#[async_trait::async_trait(?Send)]
impl Usecase for CreateUserUseCase {
    type Response = UseCaseRes;
    type Errors = UseCaseErrors;
    type Context = Context;

    async fn execute(&mut self, ctx: &Self::Context) -> Result<Self::Response, Self::Errors> {
        let user = User::new(&self.account_id, &self.external_user_id);

        if let Some(_existing_user) = ctx.repos.user_repo.find(&user.id).await {
            return Err(UseCaseErrors::UserAlreadyExists);
        }

        let res = ctx.repos.user_repo.insert(&user).await;
        match res {
            Ok(_) => Ok(UseCaseRes { user }),
            Err(_) => Err(UseCaseErrors::StorageError),
        }
    }
}

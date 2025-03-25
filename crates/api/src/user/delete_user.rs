use axum::{Extension, Json, extract::Path, http::HeaderMap};
use nittei_api_structs::delete_user::*;
use nittei_domain::{Account, ID, User};
use nittei_infra::NitteiContext;

use crate::{
    error::NitteiError,
    shared::{
        auth::protect_admin_route,
        usecase::{UseCase, execute},
    },
};

pub async fn delete_user_controller(
    headers: HeaderMap,
    path_params: Path<PathParams>,
    Extension(ctx): Extension<NitteiContext>,
) -> Result<Json<APIResponse>, NitteiError> {
    let account = protect_admin_route(&headers, &ctx).await?;

    let usecase = DeleteUserUseCase {
        account,
        user_id: path_params.user_id.clone(),
    };
    execute(usecase, &ctx)
        .await
        .map(|usecase_res| Json(APIResponse::new(usecase_res.user)))
        .map_err(NitteiError::from)
}

#[derive(Debug)]
struct DeleteUserUseCase {
    account: Account,
    user_id: ID,
}

#[derive(Debug)]
struct UseCaseRes {
    pub user: User,
}

#[derive(Debug)]
enum UseCaseError {
    StorageError,
    UserNotFound(ID),
}

impl From<UseCaseError> for NitteiError {
    fn from(e: UseCaseError) -> Self {
        match e {
            UseCaseError::StorageError => Self::InternalError,
            UseCaseError::UserNotFound(id) => {
                Self::NotFound(format!("A user with id: {}, was not found.", id))
            }
        }
    }
}

#[async_trait::async_trait]
impl UseCase for DeleteUserUseCase {
    type Response = UseCaseRes;

    type Error = UseCaseError;

    const NAME: &'static str = "DeleteUser";

    async fn execute(&mut self, ctx: &NitteiContext) -> Result<Self::Response, Self::Error> {
        let user = match ctx.repos.users.find(&self.user_id).await {
            Ok(Some(u)) if u.account_id == self.account.id => {
                match ctx.repos.users.delete(&self.user_id).await {
                    Ok(Some(u)) => u,
                    Ok(None) => return Err(UseCaseError::StorageError),
                    Err(_) => return Err(UseCaseError::StorageError),
                }
            }
            Ok(_) => return Err(UseCaseError::UserNotFound(self.user_id.clone())),
            Err(_) => return Err(UseCaseError::StorageError),
        };

        Ok(UseCaseRes { user })
    }
}

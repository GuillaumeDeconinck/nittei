use actix_web::{web, HttpRequest, HttpResponse};
use nittei_api_structs::get_outlook_calendars::{APIResponse, PathParams, QueryParams};
use nittei_domain::{
    providers::outlook::{OutlookCalendar, OutlookCalendarAccessRole},
    User,
};
use nittei_infra::{outlook_calendar::OutlookCalendarProvider, NitteiContext};

use crate::{
    error::NitteiError,
    shared::{
        auth::{account_can_modify_user, protect_account_route, protect_route},
        usecase::{execute, UseCase},
    },
};

pub async fn get_outlook_calendars_admin_controller(
    http_req: HttpRequest,
    path: web::Path<PathParams>,
    query: web::Query<QueryParams>,
    ctx: web::Data<NitteiContext>,
) -> Result<HttpResponse, NitteiError> {
    let account = protect_account_route(&http_req, &ctx).await?;
    let user = account_can_modify_user(&account, &path.user_id, &ctx).await?;

    let usecase = GetOutlookCalendarsUseCase {
        user,
        min_access_role: query.0.min_access_role,
    };

    execute(usecase, &ctx)
        .await
        .map(|calendars| HttpResponse::Ok().json(APIResponse::new(calendars)))
        .map_err(NitteiError::from)
}

pub async fn get_outlook_calendars_controller(
    http_req: HttpRequest,
    query: web::Query<QueryParams>,
    ctx: web::Data<NitteiContext>,
) -> Result<HttpResponse, NitteiError> {
    let (user, _policy) = protect_route(&http_req, &ctx).await?;

    let usecase = GetOutlookCalendarsUseCase {
        user,
        min_access_role: query.0.min_access_role,
    };

    execute(usecase, &ctx)
        .await
        .map(|calendars| HttpResponse::Ok().json(APIResponse::new(calendars)))
        .map_err(NitteiError::from)
}

#[derive(Debug)]
struct GetOutlookCalendarsUseCase {
    pub user: User,
    pub min_access_role: OutlookCalendarAccessRole,
}

#[derive(Debug)]
enum UseCaseError {
    UserNotConnectedToOutlook,
    OutlookQuery,
}

impl From<UseCaseError> for NitteiError {
    fn from(e: UseCaseError) -> Self {
        match e {
            UseCaseError::UserNotConnectedToOutlook => {
                Self::BadClientData("The user is not connected to outlook.".into())
            }
            UseCaseError::OutlookQuery => Self::InternalError,
        }
    }
}

#[async_trait::async_trait(?Send)]
impl UseCase for GetOutlookCalendarsUseCase {
    type Response = Vec<OutlookCalendar>;

    type Error = UseCaseError;

    const NAME: &'static str = "GetOutlookCalendars";

    async fn execute(&mut self, ctx: &NitteiContext) -> Result<Self::Response, Self::Error> {
        let provider = OutlookCalendarProvider::new(&self.user, ctx)
            .await
            .map_err(|_| UseCaseError::UserNotConnectedToOutlook)?;

        provider
            .list(self.min_access_role.clone())
            .await
            .map_err(|_| UseCaseError::OutlookQuery)
    }
}

use actix_web::{web, HttpRequest, HttpResponse};
use chrono::Weekday;
use chrono_tz::Tz;
use nittei_api_structs::update_calendar::{APIResponse, PathParams, RequestBody};
use nittei_domain::{Calendar, Metadata, User, ID};
use nittei_infra::NitteiContext;

use crate::{
    error::NitteiError,
    shared::{
        auth::{
            account_can_modify_calendar,
            account_can_modify_user,
            protect_account_route,
            protect_route,
            Permission,
        },
        usecase::{execute, execute_with_policy, PermissionBoundary, UseCase},
    },
};

pub async fn update_calendar_admin_controller(
    http_req: HttpRequest,
    ctx: web::Data<NitteiContext>,
    path: web::Path<PathParams>,
    body: web::Json<RequestBody>,
) -> Result<HttpResponse, NitteiError> {
    let account = protect_account_route(&http_req, &ctx).await?;
    let cal = account_can_modify_calendar(&account, &path.calendar_id, &ctx).await?;
    let user = account_can_modify_user(&account, &cal.user_id, &ctx).await?;

    let usecase = UpdateCalendarUseCase {
        user,
        calendar_id: cal.id,
        week_start: body.0.settings.week_start,
        timezone: body.0.settings.timezone,
        metadata: body.0.metadata,
    };

    execute(usecase, &ctx)
        .await
        .map(|calendar| HttpResponse::Ok().json(APIResponse::new(calendar)))
        .map_err(NitteiError::from)
}

pub async fn update_calendar_controller(
    http_req: HttpRequest,
    ctx: web::Data<NitteiContext>,
    mut path: web::Path<PathParams>,
    body: web::Json<RequestBody>,
) -> Result<HttpResponse, NitteiError> {
    let (user, policy) = protect_route(&http_req, &ctx).await?;

    let usecase = UpdateCalendarUseCase {
        user,
        calendar_id: std::mem::take(&mut path.calendar_id),
        week_start: body.0.settings.week_start,
        timezone: body.0.settings.timezone,
        metadata: body.0.metadata,
    };

    execute_with_policy(usecase, &policy, &ctx)
        .await
        .map(|calendar| HttpResponse::Ok().json(APIResponse::new(calendar)))
        .map_err(NitteiError::from)
}

#[derive(Debug)]
struct UpdateCalendarUseCase {
    pub user: User,
    pub calendar_id: ID,
    pub week_start: Option<Weekday>,
    pub timezone: Option<Tz>,
    pub metadata: Option<Metadata>,
}

#[derive(Debug)]
enum UseCaseError {
    CalendarNotFound,
    StorageError,
}

impl From<UseCaseError> for NitteiError {
    fn from(e: UseCaseError) -> Self {
        match e {
            UseCaseError::StorageError => Self::InternalError,
            UseCaseError::CalendarNotFound => Self::NotFound("The calendar was not found.".into()),
        }
    }
}

#[async_trait::async_trait(?Send)]
impl UseCase for UpdateCalendarUseCase {
    type Response = Calendar;

    type Error = UseCaseError;

    const NAME: &'static str = "UpdateCalendar";

    async fn execute(&mut self, ctx: &NitteiContext) -> Result<Self::Response, Self::Error> {
        let calendar = ctx
            .repos
            .calendars
            .find(&self.calendar_id)
            .await
            .map_err(|_| UseCaseError::StorageError)?;
        let mut calendar = match calendar {
            Some(cal) if cal.user_id == self.user.id => cal,
            _ => return Err(UseCaseError::CalendarNotFound),
        };

        if let Some(wkst) = self.week_start {
            calendar.settings.week_start = wkst;
        }

        if let Some(timezone) = self.timezone {
            calendar.settings.timezone = timezone;
        }

        if let Some(metadata) = &self.metadata {
            calendar.metadata = metadata.clone();
        }

        ctx.repos
            .calendars
            .save(&calendar)
            .await
            .map(|_| calendar)
            .map_err(|_| UseCaseError::StorageError)
    }
}

impl PermissionBoundary for UpdateCalendarUseCase {
    fn permissions(&self) -> Vec<Permission> {
        vec![Permission::UpdateCalendar]
    }
}

#[cfg(test)]
mod test {
    use nittei_domain::{Account, Calendar, User};
    use nittei_infra::setup_context;

    use super::*;

    #[actix_web::main]
    #[test]
    async fn it_update_settings_with_valid_wkst() {
        let ctx = setup_context().await.unwrap();
        let account = Account::default();
        ctx.repos.accounts.insert(&account).await.unwrap();
        let user = User::new(account.id.clone(), None);
        ctx.repos.users.insert(&user).await.unwrap();
        let calendar = Calendar::new(&user.id, &account.id);
        ctx.repos.calendars.insert(&calendar).await.unwrap();

        assert_eq!(calendar.settings.week_start, Weekday::Mon);
        let new_wkst = Weekday::Thu;
        let mut usecase = UpdateCalendarUseCase {
            user,
            calendar_id: calendar.id.clone(),
            week_start: Some(new_wkst),
            timezone: None,
            metadata: Some(Metadata::new()),
        };
        let res = usecase.execute(&ctx).await;
        assert!(res.is_ok());

        // Check that calendar settings have been updated
        let calendar = ctx
            .repos
            .calendars
            .find(&calendar.id)
            .await
            .unwrap()
            .unwrap();
        assert_eq!(calendar.settings.week_start, new_wkst);
    }
}

use std::convert::{TryFrom, TryInto};

use nittei_domain::{BusyCalendar, ID};
use sqlx::{FromRow, PgPool};
use tracing::{error, instrument};

use super::{BusyCalendarIdentifier, ExternalBusyCalendarIdentifier, IServiceUserBusyCalendarRepo};

#[derive(Debug)]
pub struct PostgresServiceUseBusyCalendarRepo {
    pool: PgPool,
}

impl PostgresServiceUseBusyCalendarRepo {
    pub fn new(pool: PgPool) -> Self {
        Self { pool }
    }
}

#[derive(Debug, FromRow)]
struct BusyCalendarRaw {
    provider: String,
    calendar_id: String,
}

impl TryFrom<BusyCalendarRaw> for BusyCalendar {
    type Error = anyhow::Error;
    fn try_from(e: BusyCalendarRaw) -> anyhow::Result<Self> {
        Ok(match &e.provider[..] {
            "google" => BusyCalendar::Google(e.calendar_id),
            "outlook" => BusyCalendar::Outlook(e.calendar_id),
            "nittei" => BusyCalendar::Nittei(e.calendar_id.parse()?),
            _ => unreachable!("Invalid provider"),
        })
    }
}

#[async_trait::async_trait]
impl IServiceUserBusyCalendarRepo for PostgresServiceUseBusyCalendarRepo {
    #[instrument]
    async fn exists(&self, input: BusyCalendarIdentifier) -> anyhow::Result<bool> {
        let res = sqlx::query!(
            r#"
            SELECT FROM service_user_busy_calendars WHERE
            service_uid = $1 AND
            user_uid = $2 AND
            calendar_uid = $3
            "#,
            input.service_id.as_ref(),
            input.user_id.as_ref(),
            input.calendar_id.as_ref(),
        )
        .execute(&self.pool)
        .await
        .inspect_err(|e| {
            error!(
                "Unable to check if nittei busy calendar: {:?} exists. DB returned error: {:?}",
                input, e
            );
        })?;

        Ok(res.rows_affected() == 1)
    }

    #[instrument]
    async fn exists_ext(&self, input: ExternalBusyCalendarIdentifier) -> anyhow::Result<bool> {
        let res = sqlx::query!(
            r#"
            SELECT FROM service_user_external_busy_calendars WHERE
            service_uid = $1 AND
            user_uid = $2 AND
            ext_calendar_id = $3
            "#,
            input.service_id.as_ref(),
            input.user_id.as_ref(),
            input.ext_calendar_id,
        )
        .execute(&self.pool)
        .await
        .inspect_err(|e| {
            error!(
                "Unable to check if external busy calendar: {:?} exists. DB returned error: {:?}",
                input, e
            );
        })?;

        Ok(res.rows_affected() == 1)
    }

    #[instrument]
    async fn insert(&self, input: BusyCalendarIdentifier) -> anyhow::Result<()> {
        sqlx::query!(
            r#"
            INSERT INTO service_user_busy_calendars(service_uid, user_uid, calendar_uid)
            VALUES($1, $2, $3)
            "#,
            input.service_id.as_ref(),
            input.user_id.as_ref(),
            input.calendar_id.as_ref(),
        )
        .execute(&self.pool)
        .await
        .inspect_err(|e| {
            error!(
                "Unable to insert nittei busy calendar: {:?}. DB returned error: {:?}",
                input, e
            );
        })?;

        Ok(())
    }

    #[instrument]
    async fn insert_ext(&self, input: ExternalBusyCalendarIdentifier) -> anyhow::Result<()> {
        let provider: String = input.provider.clone().into();
        sqlx::query!(
                r#"
            INSERT INTO service_user_external_busy_calendars(service_uid, user_uid, ext_calendar_id, provider)
            VALUES($1, $2, $3, $4)
            "#,
                input.service_id.as_ref(),
                input.user_id.as_ref(),
                &input.ext_calendar_id,
                provider as _
            )
            .execute(&self.pool)
            .await
            .inspect_err(|e| {
                error!(
                    "Unable to insert external busy calendar: {:?}. DB returned error: {:?}",
                    input, e
                );
            })?;

        Ok(())
    }

    #[instrument]
    async fn delete(&self, input: BusyCalendarIdentifier) -> anyhow::Result<()> {
        sqlx::query!(
            r#"
            DELETE FROM service_user_busy_calendars AS busy
            WHERE busy.service_uid = $1 AND
            busy.user_uid = $2 AND
            busy.calendar_uid = $3
            "#,
            input.service_id.as_ref(),
            input.user_id.as_ref(),
            input.calendar_id.as_ref(),
        )
        .execute(&self.pool)
        .await
        .inspect_err(|e| {
            error!(
                "Delete nittei busy calendar: {:?} failed. DB returned error: {:?}",
                input, e
            );
        })?;

        Ok(())
    }

    #[instrument]
    async fn delete_ext(&self, input: ExternalBusyCalendarIdentifier) -> anyhow::Result<()> {
        let provider: String = input.provider.clone().into();
        sqlx::query!(
            r#"
            DELETE FROM service_user_external_busy_calendars AS busy
            WHERE busy.service_uid = $1 AND
            busy.user_uid = $2 AND
            busy.ext_calendar_id = $3 AND
            busy.provider = $4
            "#,
            input.service_id.as_ref(),
            input.user_id.as_ref(),
            input.ext_calendar_id,
            provider as _,
        )
        .execute(&self.pool)
        .await
        .inspect_err(|e| {
            error!(
                "Delete external busy calendar: {:?} failed. DB returned error: {:?}",
                input, e
            );
        })?;

        Ok(())
    }

    #[instrument]
    async fn find(&self, service_id: &ID, user_id: &ID) -> anyhow::Result<Vec<BusyCalendar>> {
        let busy_calendars: Vec<BusyCalendarRaw> = sqlx::query_as(
            r#"
            SELECT ext_c.provider, ext_c.ext_calendar_id as calendar_id
            FROM service_user_external_busy_calendars AS ext_c
            WHERE ext_c.service_uid = $1 AND ext_c.user_uid = $2
            UNION ALL
            SELECT 'nittei' as provider, bc.calendar_uid::text as calendar_id
            FROM service_user_busy_calendars AS bc
            WHERE bc.service_uid = $1 AND bc.user_uid = $2
            "#,
        )
        .bind(service_id.as_ref())
        .bind(user_id.as_ref())
        .fetch_all(&self.pool)
        .await
        .inspect_err(|e| {
            error!(
                "Find busy calendars for service user in service_id: {} and user_id: {} failed. DB returned error: {:?}",
                service_id, user_id, e
            );
        })?;

        busy_calendars.into_iter().map(|bc| bc.try_into()).collect()
    }
}

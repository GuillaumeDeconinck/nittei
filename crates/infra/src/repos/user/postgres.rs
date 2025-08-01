use std::convert::{TryFrom, TryInto};

use nittei_domain::{ID, User};
use serde_json::Value;
use sqlx::{
    FromRow,
    PgPool,
    types::{Json, Uuid},
};
use tracing::{error, instrument};

use super::IUserRepo;
use crate::repos::shared::query_structs::MetadataFindQuery;

#[derive(Debug)]
pub struct PostgresUserRepo {
    pool: PgPool,
}

impl PostgresUserRepo {
    pub fn new(pool: PgPool) -> Self {
        Self { pool }
    }
}

#[derive(Debug, FromRow)]
struct UserRaw {
    user_uid: Uuid,
    account_uid: Uuid,
    external_id: Option<String>,
    metadata: Value,
}

impl TryFrom<UserRaw> for User {
    type Error = anyhow::Error;
    fn try_from(e: UserRaw) -> anyhow::Result<Self> {
        Ok(Self {
            id: e.user_uid.into(),
            account_id: e.account_uid.into(),
            external_id: e.external_id,
            metadata: serde_json::from_value(e.metadata)?,
        })
    }
}

#[async_trait::async_trait]
impl IUserRepo for PostgresUserRepo {
    #[instrument(name = "user::insert")]
    async fn insert(&self, user: &User) -> anyhow::Result<()> {
        sqlx::query!(
            r#"
            INSERT INTO users(user_uid, account_uid, external_id, metadata)
            VALUES($1, $2, $3, $4)
            "#,
            user.id.as_ref(),
            user.account_id.as_ref(),
            user.external_id.as_ref(),
            Json(&user.metadata) as _,
        )
        .execute(&self.pool)
        .await
        .inspect_err(|e| {
            error!(
                user = ?user,
                error = ?e,
                "Failed to insert user"
            );
        })?;

        Ok(())
    }

    #[instrument(name = "user::save")]
    async fn save(&self, user: &User) -> anyhow::Result<()> {
        sqlx::query!(
            r#"
            UPDATE users
            SET account_uid = $2,
            external_id = $3,
            metadata = $4
            WHERE user_uid = $1
            "#,
            user.id.as_ref(),
            user.account_id.as_ref(),
            user.external_id.as_ref(),
            Json(&user.metadata) as _,
        )
        .execute(&self.pool)
        .await
        .inspect_err(|e| {
            error!(
                user = ?user,
                error = ?e,
                "Failed to save user"
            );
        })?;
        Ok(())
    }

    #[instrument(name = "user::delete")]
    async fn delete(&self, user_id: &ID) -> anyhow::Result<Option<User>> {
        let res = sqlx::query_as!(
            UserRaw,
            r#"
            DELETE FROM users AS u
            WHERE u.user_uid = $1
            RETURNING *
            "#,
            user_id.as_ref(),
        )
        .fetch_optional(&self.pool)
        .await
        .inspect_err(|e| {
            error!(
                user_id = %user_id,
                error = ?e,
                "Failed to delete user"
            );
        })?;

        res.map(|user| user.try_into()).transpose()
    }

    #[instrument(name = "user::find")]
    async fn find(&self, user_id: &ID) -> anyhow::Result<Option<User>> {
        let res = sqlx::query_as!(
            UserRaw,
            r#"
            SELECT * FROM users AS u
            WHERE u.user_uid = $1
            "#,
            user_id.as_ref(),
        )
        .fetch_optional(&self.pool)
        .await
        .inspect_err(|e| {
            error!(
                user_id = %user_id,
                error = ?e,
                "Failed to find user"
            );
        })?;

        res.map(|user| user.try_into()).transpose()
    }

    #[instrument(name = "user::find_many")]
    async fn find_many(&self, user_ids: &[ID]) -> anyhow::Result<Vec<User>> {
        let user_ids = user_ids.iter().map(|id| *id.as_ref()).collect::<Vec<_>>();

        let users: Vec<UserRaw> = sqlx::query_as!(
            UserRaw,
            r#"
            SELECT * FROM users AS u
            WHERE u.user_uid = ANY($1)
            "#,
            &user_ids
        )
        .fetch_all(&self.pool)
        .await
        .inspect_err(|e| {
            error!(
                user_ids = ?user_ids,
                error = ?e,
                "Failed to find users"
            );
        })?;

        users.into_iter().map(|u| u.try_into()).collect()
    }

    #[instrument(name = "user::find_by_account_id")]
    async fn find_by_account_id(
        &self,
        user_id: &ID,
        account_id: &ID,
    ) -> anyhow::Result<Option<User>> {
        let res = sqlx::query_as!(
            UserRaw,
            r#"
            SELECT * FROM users AS u
            WHERE u.user_uid = $1 AND
            u.account_uid = $2
            "#,
            user_id.as_ref(),
            account_id.as_ref()
        )
        .fetch_optional(&self.pool)
        .await
        .inspect_err(|e| {
            error!(
                user_id = %user_id,
                error = ?e,
                "Failed to find user"
            );
        })?;

        res.map(|user| user.try_into()).transpose()
    }

    #[instrument(name = "user::get_by_external_id")]
    async fn get_by_external_id(&self, external_id: &str) -> anyhow::Result<Option<User>> {
        let res = sqlx::query_as!(
            UserRaw,
            r#"
            SELECT * FROM users AS u
            WHERE u.external_id = $1
            "#,
            external_id,
        )
        .fetch_optional(&self.pool)
        .await
        .inspect_err(|e| {
            error!(
                external_id = %external_id,
                error = ?e,
                "Failed to find user"
            );
        })?;

        res.map(|user| user.try_into()).transpose()
    }

    #[instrument(name = "user::find_by_metadata")]
    async fn find_by_metadata(&self, query: MetadataFindQuery) -> anyhow::Result<Vec<User>> {
        let users: Vec<UserRaw> = sqlx::query_as!(
            UserRaw,
            r#"
            SELECT * FROM users AS u
            WHERE u.account_uid = $1 AND metadata @> $2
            LIMIT $3
            OFFSET $4
            "#,
            query.account_id.as_ref(),
            Json(&query.metadata) as _,
            query.limit as i64,
            query.skip as i64,
        )
        .fetch_all(&self.pool)
        .await
        .inspect_err(|e| {
            error!(
                query = ?query,
                error = ?e,
                "Failed to find users"
            );
        })?;

        users.into_iter().map(|u| u.try_into()).collect()
    }
}

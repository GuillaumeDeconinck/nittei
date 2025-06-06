mod postgres;

use nittei_domain::{ID, User};
pub use postgres::PostgresUserRepo;

use super::shared::query_structs::MetadataFindQuery;

#[async_trait::async_trait]
pub trait IUserRepo: Send + Sync {
    async fn insert(&self, user: &User) -> anyhow::Result<()>;
    async fn save(&self, user: &User) -> anyhow::Result<()>;
    async fn delete(&self, user_id: &ID) -> anyhow::Result<Option<User>>;
    async fn find(&self, user_id: &ID) -> anyhow::Result<Option<User>>;
    async fn find_many(&self, user_ids: &[ID]) -> anyhow::Result<Vec<User>>;
    async fn find_by_account_id(
        &self,
        user_id: &ID,
        account_id: &ID,
    ) -> anyhow::Result<Option<User>>;
    async fn get_by_external_id(&self, external_id: &str) -> anyhow::Result<Option<User>>;
    async fn find_by_metadata(&self, query: MetadataFindQuery) -> anyhow::Result<Vec<User>>;
}

#[cfg(test)]
mod tests {
    use nittei_domain::{Account, Metadata};

    use super::*;
    use crate::setup_context;

    #[tokio::test]
    async fn test_metadata_query() {
        let ctx = setup_context().await.unwrap();

        let account = Account::new();
        ctx.repos
            .accounts
            .insert(&account)
            .await
            .expect("To insert account");
        let mut user = User::new(account.id.clone(), None);
        ctx.repos.users.insert(&user).await.expect("To insert user");

        let mut query = MetadataFindQuery {
            account_id: account.id.clone(),
            limit: 100,
            metadata: Metadata::new_kv("group_id".to_string(), "123".to_string()),
            skip: 0,
        };

        assert!(
            ctx.repos
                .users
                .find_by_metadata(query.clone())
                .await
                .unwrap()
                .is_empty()
        );

        // Now add metadata
        let metadata = serde_json::json! ({ "group_id": "123" });

        user.metadata = Some(metadata);
        ctx.repos.users.save(&user).await.expect("To save user");

        let res = ctx
            .repos
            .users
            .find_by_metadata(query.clone())
            .await
            .unwrap();
        assert_eq!(res.len(), 1);
        assert_eq!(res[0].id, user.id);

        // Different account id should give no results
        query.account_id = ID::default();
        assert!(
            ctx.repos
                .users
                .find_by_metadata(query)
                .await
                .unwrap()
                .is_empty()
        );
    }

    // #[tokio::test]
    // async fn test_google_integration_revoke() {
    //     let ctx = setup_context().await.unwrap();
    //     let account_id = ID::default();
    //     let mut user = User::new(account_id.clone());
    //     user.integrations = vec![IntegrationProvider::Google(UserGoogleIntegrationData {
    //         access_token: "1".into(),
    //         refresh_token: "1".into(),
    //         access_token_expires_ts: 1,
    //     })];
    //     ctx.repos
    //         .users
    //         .insert(&user)
    //         .await
    //         .expect("To save user");

    //     let user = ctx
    //         .repos
    //         .users
    //         .find(&user.id)
    //         .await
    //         .expect("To find user just inserted");

    //     // Check that integration is there before deleting it
    //     assert_eq!(user.integrations.len(), 1);

    //     assert!(ctx
    //         .repos
    //         .users
    //         .revoke_google_integration(&user.account_id)
    //         .await
    //         .is_ok());

    //     let user = ctx
    //         .repos
    //         .users
    //         .find(&user.id)
    //         .await
    //         .expect("To find user just inserted");

    //     assert!(user.integrations.is_empty());
    // }
}

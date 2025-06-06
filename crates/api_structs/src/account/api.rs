use nittei_domain::Account;
use serde::{Deserialize, Serialize};
use ts_rs::TS;
use utoipa::ToSchema;
use validator::Validate;

use crate::dtos::AccountDTO;

/// Account response object
#[derive(Deserialize, Serialize, TS, ToSchema)]
#[serde(rename_all = "camelCase")]
#[ts(export)]
pub struct AccountResponse {
    /// Account retrieved
    pub account: AccountDTO,
}

impl AccountResponse {
    pub fn new(account: Account) -> Self {
        Self {
            account: AccountDTO::new(&account),
        }
    }
}

pub mod create_account {
    use super::*;

    /// Request body for creating an account
    #[derive(Deserialize, Serialize, Validate, TS, ToSchema)]
    #[serde(rename_all = "camelCase")]
    #[ts(export)]
    pub struct CreateAccountRequestBody {
        /// Code used for authentifying the request
        /// Creating accounts is an admin operation, so it requires a specific code
        #[validate(length(min = 1))]
        pub code: String,
    }

    /// Response body for creating an account
    #[derive(Serialize, Deserialize, TS, ToSchema)]
    #[serde(rename_all = "camelCase")]
    #[ts(export)]
    pub struct CreateAccountResponseBody {
        /// Account created
        pub account: AccountDTO,
        /// API Key that can be used for doing requests for this account
        pub secret_api_key: String,
    }

    impl CreateAccountResponseBody {
        pub fn new(account: Account) -> Self {
            Self {
                account: AccountDTO::new(&account),
                secret_api_key: account.secret_api_key,
            }
        }
    }
}

pub mod get_account {
    use super::*;

    pub type APIResponse = AccountResponse;
}

pub mod set_account_pub_key {
    use super::*;

    /// Request body for setting the public JWT key of an account
    #[derive(Debug, Deserialize, Serialize, Validate, TS, ToSchema)]
    #[serde(rename_all = "camelCase")]
    #[ts(export)]
    pub struct SetAccountPubKeyRequestBody {
        /// Public JWT key
        #[validate(length(min = 1))]
        pub public_jwt_key: Option<String>,
    }

    pub type APIResponse = AccountResponse;
}

pub mod set_account_webhook {

    use super::*;

    /// Request body for setting the webhook of an account
    #[derive(Debug, Deserialize, Serialize, Validate, TS, ToSchema)]
    #[serde(rename_all = "camelCase")]
    #[ts(export)]
    pub struct SetAccountWebhookRequestBody {
        /// Webhook URL
        #[validate(url)]
        pub webhook_url: String,
    }

    pub type APIResponse = AccountResponse;
}

pub mod delete_account_webhook {
    use super::*;

    pub type APIResponse = AccountResponse;
}

pub mod add_account_integration {
    use nittei_domain::IntegrationProvider;

    use super::*;

    /// Request body for adding an integration to an account
    #[derive(Debug, Deserialize, Serialize, Validate, TS, ToSchema)]
    #[serde(rename_all = "camelCase")]
    #[ts(export)]
    pub struct AddAccountIntegrationRequestBody {
        /// Client ID of the integration
        #[validate(length(min = 1))]
        pub client_id: String,

        // Client secret of the integration
        #[validate(length(min = 1))]
        pub client_secret: String,

        // Redirect URI of the integration
        #[validate(url)]
        pub redirect_uri: String,

        /// Provider of the integration
        /// This is used to know which integration to use
        /// E.g. Google, Outlook, etc.
        pub provider: IntegrationProvider,
    }

    pub type APIResponse = String;
}

pub mod remove_account_integration {
    use nittei_domain::IntegrationProvider;

    use super::*;

    #[derive(Debug, Deserialize, Serialize)]
    pub struct PathParams {
        pub provider: IntegrationProvider,
    }

    pub type APIResponse = String;
}

/// Request body for searching events for a whole account (across all users)
pub mod account_search_events {
    use nittei_domain::{CalendarEventSort, DateTimeQuery, IDQuery, RecurrenceQuery, StringQuery};

    use super::*;
    use crate::dtos::CalendarEventDTO;

    /// Request body for searching events for a whole account (across all users)
    #[derive(Deserialize, Serialize, Validate, TS, ToSchema)]
    #[serde(rename_all = "camelCase", deny_unknown_fields)]
    #[ts(export, rename_all = "camelCase")]
    pub struct AccountSearchEventsRequestBody {
        /// Filter to use for searching events
        pub filter: AccountSearchEventsRequestBodyFilter,

        /// Optional sort to use when searching events
        #[ts(optional)]
        pub sort: Option<CalendarEventSort>,

        /// Optional limit to use when searching events (u16)
        /// Defaults to 200
        #[ts(optional)]
        pub limit: Option<u16>,
    }

    /// Request body for searching events for a whole account (across all users)
    #[derive(Deserialize, Serialize, Validate, TS, ToSchema)]
    #[serde(rename_all = "camelCase", deny_unknown_fields)]
    #[ts(export, rename_all = "camelCase")]
    pub struct AccountSearchEventsRequestBodyFilter {
        /// Optional query on event ID
        #[ts(optional)]
        pub event_uid: Option<IDQuery>,

        /// Optional query on user ID, or list of user IDs
        #[ts(optional)]
        pub user_id: Option<IDQuery>,

        /// Optional query on external ID (which is a string as it's an ID from an external system)
        #[ts(optional)]
        pub external_id: Option<StringQuery>,

        /// Optional query on external parent ID (which is a string as it's an ID from an external system)
        #[ts(optional)]
        pub external_parent_id: Option<StringQuery>,

        /// Optional query on the group ID
        #[ts(optional)]
        pub group_id: Option<IDQuery>,

        /// Optional query on start time - e.g. "lower than or equal", or "great than or equal" (UTC)
        #[ts(optional)]
        pub start_time: Option<DateTimeQuery>,

        /// Optional query on end time - e.g. "lower than or equal", or "great than or equal" (UTC)
        #[ts(optional)]
        pub end_time: Option<DateTimeQuery>,

        /// Optional query on event type
        #[ts(optional)]
        pub event_type: Option<StringQuery>,

        /// Optional query on event status
        #[ts(optional)]
        pub status: Option<StringQuery>,

        /// Optional query on the recurring event UID
        #[ts(optional)]
        pub recurring_event_uid: Option<IDQuery>,

        /// Optional query on original start time - "lower than or equal", or "great than or equal" (UTC)
        #[ts(optional)]
        pub original_start_time: Option<DateTimeQuery>,

        /// Optional filter on the recurrence
        /// This allows to filter on the existence or not of a recurrence, or the existence of a recurrence at a specific date
        #[ts(optional)]
        pub recurrence: Option<RecurrenceQuery>,

        /// Optional query on metadata
        #[ts(optional)]
        pub metadata: Option<serde_json::Value>,

        /// Optional query on created at - e.g. "lower than or equal", or "great than or equal" (UTC)
        #[ts(optional)]
        pub created_at: Option<DateTimeQuery>,

        /// Optional query on updated at - e.g. "lower than or equal", or "great than or equal" (UTC)
        #[ts(optional)]
        pub updated_at: Option<DateTimeQuery>,
    }

    /// API response for getting events by calendars
    #[derive(Serialize, TS, ToSchema)]
    #[serde(rename_all = "camelCase")]
    #[ts(export)]
    pub struct SearchEventsAPIResponse {
        /// List of calendar events retrieved
        pub events: Vec<CalendarEventDTO>,
    }

    impl SearchEventsAPIResponse {
        pub fn new(events: Vec<CalendarEventDTO>) -> Self {
            Self { events }
        }
    }
}

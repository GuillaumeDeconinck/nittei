pub mod create_user;
mod delete_user;
mod get_me;
mod get_multiple_users_freebusy;
mod get_user;
mod get_user_freebusy;
mod get_users_by_meta;
mod oauth_integration;
mod remove_integration;
mod update_user;

use actix_web::web;
use create_user::create_user_controller;
use delete_user::delete_user_controller;
use get_me::get_me_controller;
use get_multiple_users_freebusy::get_multiple_freebusy_controller;
use get_user::get_user_controller;
use get_user_freebusy::get_freebusy_controller;
pub use get_user_freebusy::parse_vec_query_value;
use get_users_by_meta::get_users_by_meta_controller;
use oauth_integration::*;
use remove_integration::{remove_integration_admin_controller, remove_integration_controller};
use update_user::update_user_controller;

pub fn configure_routes(cfg: &mut web::ServiceConfig) {
    cfg.route("/user", web::post().to(create_user_controller));
    cfg.route("/me", web::get().to(get_me_controller));
    cfg.route("/user/meta", web::get().to(get_users_by_meta_controller));
    // This is a POST route !
    cfg.route(
        "/user/freebusy",
        web::post().to(get_multiple_freebusy_controller),
    );
    cfg.route("/user/{user_id}", web::get().to(get_user_controller));
    cfg.route("/user/{user_id}", web::put().to(update_user_controller));
    cfg.route("/user/{user_id}", web::delete().to(delete_user_controller));
    cfg.route(
        "/user/{user_id}/freebusy",
        web::get().to(get_freebusy_controller),
    );

    // Oauth
    cfg.route("/me/oauth", web::post().to(oauth_integration_controller));
    cfg.route(
        "/me/oauth/{provider}",
        web::delete().to(remove_integration_controller),
    );
    cfg.route(
        "/user/{user_id}/oauth",
        web::post().to(oauth_integration_admin_controller),
    );
    cfg.route(
        "/user/{user_id}/oauth/{provider}",
        web::delete().to(remove_integration_admin_controller),
    );
}

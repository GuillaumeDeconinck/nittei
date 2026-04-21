package com.meetsmore.nittei.domain;

public record Account(ID id, String secretApiKey, PEMKey publicJwtKey, AccountSettings settings) {}

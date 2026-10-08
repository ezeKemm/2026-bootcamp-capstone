package com.northstar.crm.auth.dto;

/** Returned by a successful login. Never log accessToken. role is AGENT or ADMIN. */
public record LoginResponse(String accessToken, String tokenType, String username, String role) {}

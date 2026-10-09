package com.northstar.crm.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Body of POST /api/v1/auth/login. */
public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

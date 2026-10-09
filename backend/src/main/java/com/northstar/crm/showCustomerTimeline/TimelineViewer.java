package com.northstar.crm.showCustomerTimeline;

import org.springframework.security.core.Authentication;

/** The caller, reduced to what the visibility rule needs. */
record TimelineViewer(String username, boolean seesEveryInteraction) {

    static TimelineViewer from(Authentication authentication) {
        boolean admin = authentication.getAuthorities().stream()
            .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        return new TimelineViewer(authentication.getName(), admin);
    }
}
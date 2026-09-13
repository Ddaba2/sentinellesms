package com.sentinellesms.security;

public final class Roles {

    public static final String SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    public static final String ADMIN = "ROLE_ADMIN";
    public static final String MODERATOR = "ROLE_MODERATOR";
    public static final String ANALYST = "ROLE_ANALYST";
    public static final String USER = "ROLE_USER";

    /** Lecture back-office (stats, listes, consultation). */
    public static final String BO_READ = "hasAnyRole('SUPER_ADMIN','ADMIN','MODERATOR','ANALYST')";

    /** Modération des signalements et réputations. */
    public static final String BO_MODERATE = "hasAnyRole('SUPER_ADMIN','ADMIN','MODERATOR')";

    /** Administration (règles, contenus, utilisateurs). */
    public static final String BO_ADMIN = "hasAnyRole('SUPER_ADMIN','ADMIN')";

    /** Super-administrateur uniquement. */
    public static final String BO_SUPER = "hasRole('SUPER_ADMIN')";

    private Roles() {
    }
}

package com.meetsmore.nittei.api.shared.auth;

import java.util.List;

public record Policy(List<Permission> allow, List<Permission> reject) {

    public boolean authorize(List<Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return true;
        }

        if (reject != null) {
            for (Permission denied : reject) {
                if (denied == Permission.ALL || permissions.contains(denied)) {
                    return false;
                }
            }
        }

        if (allow == null) {
            return false;
        }

        if (allow.contains(Permission.ALL)) {
            return true;
        }

        return allow.containsAll(permissions);
    }

    public static Policy empty() {
        return new Policy(null, null);
    }
}

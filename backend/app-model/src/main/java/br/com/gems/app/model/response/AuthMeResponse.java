package br.com.gems.app.model.response;

import java.util.List;
import java.util.Set;
import java.util.UUID;
public record AuthMeResponse(User user, List<Tenant> tenants, Set<String> globalActions) {
    public record User(UUID id, String name, String email) {
    }
    public record Tenant(String alias, String name) {
    }
}

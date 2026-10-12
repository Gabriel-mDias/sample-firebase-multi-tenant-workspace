package br.com.gems.app.model.response;

import java.util.List;
import java.util.Set;
public record AuthContextResponse(Tenant tenant, List<Container> profiles, List<Container> groups,
        Set<String> actions) {
    public record Tenant(String alias, String name) {
    }
    public record Container(String code, String name) {
    }
}

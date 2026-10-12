package br.com.gems.app.model.dto;

public record UsuarioCadastroDTO(String name, String email, Method method) {

    public enum Method {

        GOOGLE, PASSWORD

    }

}

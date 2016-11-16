package com.example.fellipe.trackme.dto;

/**
 * Created by Fellipe on 16/11/2016.
 */

public class ContatoDTO {

    private String id;
    private String email;

    public ContatoDTO(String id, String email) {
        this.id = id;
        this.email = email;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}

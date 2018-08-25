package br.com.fellipe.naocheguei.util;

import br.com.fellipe.naocheguei.dto.ContatoDTO;
import br.com.fellipe.naocheguei.dto.TripInfo;

import java.util.ArrayList;
import java.util.List;

import br.com.fellipe.naocheguei.dto.TripInfo;

/**
 * Created by Fellipe on 28/07/2016.
 */
public class Session {
    private static Session ourInstance = new Session();

    public static Session getInstance() {
        if (ourInstance == null){
            ourInstance = new Session();
        }
        return ourInstance;
    }

    private Session() {
        contacts = new ArrayList<>();
    }

    private String userId;
    private TripInfo trip;
    private ArrayList<ContatoDTO> contacts;

    public TripInfo getTrip() {
        return trip;
    }

    public void setTrip(TripInfo trip) {
        this.trip = trip;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public ArrayList<ContatoDTO> getContacts() {
        return contacts;
    }

    public void setContacts(ArrayList<ContatoDTO> contacts) {
        this.contacts = contacts;
    }
}

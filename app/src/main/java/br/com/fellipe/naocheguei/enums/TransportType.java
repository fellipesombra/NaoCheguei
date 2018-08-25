package br.com.fellipe.naocheguei.enums;

/**
 * Created by Fellipe on 16/10/2016.
 */

public enum TransportType {

    BICYCLING("bicycling"),
    DRIVING("driving"),
    WALKING("walking"),
    PUBLIC_TRANSPORT("transit");

    private String name;

    TransportType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static TransportType getByName(String name){
        for (TransportType t: values()) {
            if(t.getName().equalsIgnoreCase(name)){
                return t;
            }
        }
        return null;
    }

}

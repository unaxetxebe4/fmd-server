package de.nulide.findmydevice.data;

import androidx.annotation.Nullable;

public class Contact {

    private String name;
    private String number;

    public Contact() {
    }

    public Contact(String name, String number) {
        this.name = name;
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public boolean equals(@Nullable Contact toCheck) {
        if(name.equals(toCheck.name) && number.equals(toCheck.number)){
            return true;
        }
        return false;
    }
}

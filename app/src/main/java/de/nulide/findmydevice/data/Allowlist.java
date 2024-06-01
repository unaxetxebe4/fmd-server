package de.nulide.findmydevice.data;

import java.util.LinkedList;

import de.nulide.findmydevice.data.io.IO;

public class Allowlist extends LinkedList<Contact> {

    public Allowlist() {

    }

    public boolean superAdd(Contact c) {
        return super.add(c);
    }

    @Override
    public Contact remove(int index) {
        Contact c = get(index);
        super.remove(index);
        IO.write(this, IO.whiteListFileName);
        return c;
    }

    @Override
    public boolean add(Contact c) {
        if (!checkForDuplicates(c)) {
            super.add(c);
            IO.write(this, IO.whiteListFileName);
        }
        return true;
    }

    public boolean checkForDuplicates(Contact toCheck) {
        for (Contact contact : this) {
            if (contact.equals(toCheck)) {
                return true;
            }
        }
        return false;
    }
}

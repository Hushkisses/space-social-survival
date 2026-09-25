package com.hushkisses.spacesurvival.social.sanction;

public final class PlayerSanctionState {
    private boolean medicalCheckOrdered;
    private boolean disarmed;
    private boolean detained;
    private boolean accessRestricted;
    private boolean ejected;

    public boolean medicalCheckOrdered() { return medicalCheckOrdered; }
    public boolean disarmed() { return disarmed; }
    public boolean detained() { return detained; }
    public boolean accessRestricted() { return accessRestricted; }
    public boolean ejected() { return ejected; }

    void orderMedicalCheck() { medicalCheckOrdered = true; }
    void disarm() { disarmed = true; }
    void detain() { detained = true; }
    void restrictAccess() { accessRestricted = true; }
    void eject() { ejected = true; }
}

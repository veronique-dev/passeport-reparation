package com.passeportreparation.common.enums;

public enum IssueCode {
    // Lave-linge
    WM_DRAIN_PUMP,
    WM_DOOR_LOCK,
    WM_NO_SPIN,
    WM_ELECTRONIC_BOARD,
    WM_UNKNOWN,

    // Lave-vaisselle
    DW_HEATING,
    DW_DRAIN,
    DW_SPRAY_ARM,
    DW_ELECTRONIC_BOARD,
    DW_UNKNOWN,

    // Four
    OV_THERMOSTAT,
    OV_HEATING_ELEMENT,
    OV_DOOR_SEAL,
    OV_ELECTRONIC_BOARD,
    OV_UNKNOWN,

    // Hors périmètre / non précisé
    UNSUPPORTED_OTHER
}

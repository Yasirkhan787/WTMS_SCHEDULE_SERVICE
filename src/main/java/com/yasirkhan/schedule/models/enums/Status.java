package com.yasirkhan.schedule.models.enums;

public enum Status {

    IN_YARD,    // The driver arrived at 6:00 AM and is waiting.

    QUEUED,     //The supervisor puts the truck in line for the excavator.

    LOADING,    // The truck is under the excavator.

    IN_TRANSIT, // The truck is full and driving to the dump.

    DUMPING,    // The truck is at the weighbridge/landfill.

    RETURNING,  //The truck is empty and driving back to the excavator for Trip 2.

    ACTIVE,
    ASSIGNED,
    DELAYED,
    CANCELLED,
    COMPLETED,
    BLOCKED;
}

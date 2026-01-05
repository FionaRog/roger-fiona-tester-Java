package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;


public class FareCalculatorService {

       public void calculateFare(Ticket ticket) {
        calculateFare(ticket, false);
    }

    public void calculateFare(Ticket ticket, boolean discount){
        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime().toString());
        }

        long inTime = ticket.getInTime().getTime();
        long outTime = ticket.getOutTime().getTime();

                long durationInMillis = outTime - inTime;
                double duration = durationInMillis / (1000.0 * 60 * 60);
                duration = Math.round(duration * 100.0) / 100.0;
                if (durationInMillis < 30 * 60 * 1000 ) {
                    ticket.setPrice(0);
                    return;
                }

        switch (ticket.getParkingSpot().getParkingType()){
            case CAR: {
                double price = discount ? (duration * Fare.CAR_RATE_PER_HOUR * 0.95) : (duration * Fare.CAR_RATE_PER_HOUR);
                ticket.setPrice(price);
                break;
            }
            case BIKE: {
                double price = discount ? (duration * Fare.BIKE_RATE_PER_HOUR * 0.95) : (duration * Fare.BIKE_RATE_PER_HOUR);
                ticket.setPrice(price);
                break;
            }
            default: throw new IllegalArgumentException("Unkown Parking Type");
        }
    }
}
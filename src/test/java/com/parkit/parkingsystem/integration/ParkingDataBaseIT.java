package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    private static void setUp() throws Exception{
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    private void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    private static void tearDown(){
    // Nothing to clean up
    }

    @Test
    public void testParkingACar() throws Exception {
        int nextAvailableParkingSpot = parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR);
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        parkingService.processIncomingVehicle();
        Ticket savedTicket = ticketDAO.getTicket("ABCDEF");

        assertNotNull(savedTicket,"the ticket must exist in database after its entrance");
        assertNotNull(savedTicket.getParkingSpot(),"the ticket must be associated with a parking spot");

        int parkingNumber = savedTicket.getParkingSpot().getId();
        assertTrue(parkingNumber > 0);

        int finalNextAvailableParkingSpot = parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR);
        assertTrue(finalNextAvailableParkingSpot > nextAvailableParkingSpot, "the parking spot used by the vehicle should be unavailable");
    }

    @Test
    public void testParkingLotExit() throws Exception {
        int nextAvailableParkingSpot = parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR);
        testParkingACar();
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        Ticket savedTicket = ticketDAO.getTicket("ABCDEF");
        setInTime(savedTicket);
        ticketDAO.updateTicket(savedTicket);

        parkingService.processExitingVehicle();
        savedTicket = ticketDAO.getTicket("ABCDEF");


        assertNotNull(savedTicket.getOutTime(), "out time must be saved into the ticket");

        assertTrue(savedTicket.getPrice() > 0, "the price must be more than 0 because the parking duration is more than 30 minutes");

        int finalNextAvailableParkingSpot = parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR);
        assertEquals(finalNextAvailableParkingSpot , nextAvailableParkingSpot, "the parking spot must be available as before the entrance of the vehicle");
    }

    private static void setInTime(Ticket savedTicket) {
        Date dateMinusOneHour = Date.from(
                LocalDateTime.now()
                        .minusHours(1)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
        );
        savedTicket.setInTime(dateMinusOneHour);
    }

    @Test
    public void testParkingLotExitRecurringUser() throws Exception {
        testParkingLotExit();

        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        parkingService.processIncomingVehicle();

        assertTrue(ticketDAO.getNbTicket("ABCDEF") > 1, "Must be a recurring user with at least 1 ticket already registered");

        Ticket secondTicket = ticketDAO.getTicket("ABCDEF");

       setInTime(secondTicket);

        ticketDAO.updateTicket(secondTicket);

        parkingService.processExitingVehicle();


        Ticket savedTicket = ticketDAO.getTicket("ABCDEF");

        double expectedDiscountedPrice = 1.4249999999999998;
        assertEquals(expectedDiscountedPrice, savedTicket.getPrice(), 0.01,"The price must include a 5% discount for recurring user");
    }


    }


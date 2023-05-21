package uk.gov.dwp.uc.pairtest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketPurchaseRequest;
import uk.gov.dwp.uc.pairtest.domain.TicketRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class TicketServiceImplTest {

    private static final TicketRequest CHILD_TICKET_REQUEST = new TicketRequest(TicketRequest.Type.CHILD, 1);
    private static final TicketRequest INFANT_TICKET_REQUEST = new TicketRequest(TicketRequest.Type.INFANT, 1);
    private static final TicketRequest ADULT_TICKET_REQUEST = new TicketRequest(TicketRequest.Type.ADULT, 1);
    private final TicketPaymentService ticketPaymentService = mock(TicketPaymentService.class);
    private final SeatReservationService seatReservationService = mock(SeatReservationService.class);
    private final TicketServiceImpl underTest = new TicketServiceImpl(ticketPaymentService, seatReservationService);

    @Test
    public void shouldDoNothingWhenTicketPurchaseRequestIsNull() {
        underTest.purchaseTickets(null);
        verifyNoInteractions(seatReservationService);
        verifyNoInteractions(ticketPaymentService);
    }

    @Test
    public void shouldDoNothingWhenTicketRequestIsEmpty() {
        underTest.purchaseTickets(new TicketPurchaseRequest(1, new TicketRequest[]{}));
        verifyNoInteractions(seatReservationService);
        verifyNoInteractions(ticketPaymentService);
    }

    @Test
    public void shouldThrowExceptionWhenTicketTypeIsNull() {
        TicketRequest ticketRequest = new TicketRequest(null, 1);
        TicketPurchaseRequest ticketPurchaseRequest = new TicketPurchaseRequest(1, new TicketRequest[]{ticketRequest});
        InvalidPurchaseException exception = Assertions.assertThrows(InvalidPurchaseException.class,
                () -> underTest.purchaseTickets(ticketPurchaseRequest));
        assertEquals("Invalid ticket type", exception.getMessage());
    }

    @Test
    public void shouldThrowExceptionWhenMoreThan20Tickets() {
        TicketRequest adultTicketRequest = new TicketRequest(TicketRequest.Type.ADULT, 19);
        TicketPurchaseRequest ticketPurchaseRequest = new TicketPurchaseRequest(1, new TicketRequest[]{adultTicketRequest, CHILD_TICKET_REQUEST, INFANT_TICKET_REQUEST});

        InvalidPurchaseException exception = Assertions.assertThrows(InvalidPurchaseException.class,
                () -> underTest.purchaseTickets(ticketPurchaseRequest));
        assertEquals("A maximum of 20 tickets can be purchased at a time", exception.getMessage());
    }
    @Test
    public void shouldThrowExceptionWhenLessThan1Ticket() {
        TicketRequest adultTickerRequest = new TicketRequest(TicketRequest.Type.ADULT, 0);
        TicketPurchaseRequest ticketPurchaseRequest = new TicketPurchaseRequest(1, new TicketRequest[]{adultTickerRequest});

        InvalidPurchaseException exception = Assertions.assertThrows(InvalidPurchaseException.class,
                () -> underTest.purchaseTickets(ticketPurchaseRequest));
        assertEquals("No ticket selected", exception.getMessage());
    }

    @Test
    public void shouldThrowExceptionWhenAdultTicketNotPresent() {
        TicketPurchaseRequest ticketPurchaseRequest = new TicketPurchaseRequest(1, new TicketRequest[]{CHILD_TICKET_REQUEST, INFANT_TICKET_REQUEST});

        InvalidPurchaseException exception = Assertions.assertThrows(InvalidPurchaseException.class,
                () -> underTest.purchaseTickets(ticketPurchaseRequest));
        assertEquals("Child and Infant tickets cannot be purchased without purchasing an Adult ticket", exception.getMessage());
    }

    @Test
    public void shouldThrowExceptionWhenInvalidAccountId() {
        TicketPurchaseRequest ticketPurchaseRequest = new TicketPurchaseRequest(0, new TicketRequest[]{ADULT_TICKET_REQUEST});

        InvalidPurchaseException exception = Assertions.assertThrows(InvalidPurchaseException.class,
                () -> underTest.purchaseTickets(ticketPurchaseRequest));
        assertEquals("Invalid account ID", exception.getMessage());
    }

    @Test
    public void shouldCallMakePaymentWithCorrectAmountWhenOneTicketPerType() {
        TicketPurchaseRequest ticketPurchaseRequest = new TicketPurchaseRequest(1, new TicketRequest[]{ADULT_TICKET_REQUEST, CHILD_TICKET_REQUEST, INFANT_TICKET_REQUEST, });
        underTest.purchaseTickets(ticketPurchaseRequest);
        Mockito.verify(ticketPaymentService, times(1)).makePayment(1, 30);
    }
    @Test
    public void shouldCallMakePaymentWithCorrectAmountWhenMultipleTicketPerType() {
        TicketPurchaseRequest ticketPurchaseRequest = new TicketPurchaseRequest(1, new TicketRequest[]{
                new TicketRequest(TicketRequest.Type.ADULT, 5),
                new TicketRequest(TicketRequest.Type.CHILD, 5),
                new TicketRequest(TicketRequest.Type.INFANT, 5)});
        underTest.purchaseTickets(ticketPurchaseRequest);
        Mockito.verify(ticketPaymentService, times(1)).makePayment(1, 150);
    }

    @Test
    public void shouldCallReserveSeatWithCorrectNumberWhenOneTicketPerType() {
        TicketPurchaseRequest ticketPurchaseRequest = new TicketPurchaseRequest(1, new TicketRequest[]{ADULT_TICKET_REQUEST, CHILD_TICKET_REQUEST, INFANT_TICKET_REQUEST});
        underTest.purchaseTickets(ticketPurchaseRequest);
        Mockito.verify(seatReservationService, times(1)).reserveSeat(1, 2);
    }
    @Test
    public void shouldCallReserveSeatWithCorrectNumberWhenMultipleTicketPerType() {
        TicketPurchaseRequest ticketPurchaseRequest = new TicketPurchaseRequest(1, new TicketRequest[]{
                new TicketRequest(TicketRequest.Type.ADULT, 5),
                new TicketRequest(TicketRequest.Type.CHILD, 5),
                new TicketRequest(TicketRequest.Type.INFANT, 5)});
        underTest.purchaseTickets(ticketPurchaseRequest);
        Mockito.verify(seatReservationService, times(1)).reserveSeat(1, 10);
    }
    @Test
    public void shouldMakePaymentAndReserveSeatsWhenInfant() {
        TicketPurchaseRequest ticketPurchaseRequest = new TicketPurchaseRequest(1, new TicketRequest[]{ADULT_TICKET_REQUEST, INFANT_TICKET_REQUEST});
        underTest.purchaseTickets(ticketPurchaseRequest);
        Mockito.verify(ticketPaymentService, times(1)).makePayment(1, 20);
        Mockito.verify(seatReservationService, times(1)).reserveSeat(1, 1);
    }

    @Test
    public void shouldMakePaymentAndReserveSeatsWhenMultipleRequestsOfTheSameType() {
        TicketPurchaseRequest ticketPurchaseRequest = new TicketPurchaseRequest(1, new TicketRequest[]{ADULT_TICKET_REQUEST, ADULT_TICKET_REQUEST});
        underTest.purchaseTickets(ticketPurchaseRequest);
        Mockito.verify(ticketPaymentService, times(1)).makePayment(1, 40);
        Mockito.verify(seatReservationService, times(1)).reserveSeat(1, 2);
    }
}
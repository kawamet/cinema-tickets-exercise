package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketPurchaseRequest;
import uk.gov.dwp.uc.pairtest.domain.TicketRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import java.util.Arrays;
import java.util.Map;

import static java.util.stream.Collectors.toMap;
import static uk.gov.dwp.uc.pairtest.domain.TicketRequest.Type;
import static uk.gov.dwp.uc.pairtest.domain.TicketRequest.Type.ADULT;
import static uk.gov.dwp.uc.pairtest.domain.TicketRequest.Type.CHILD;


public class TicketServiceImpl implements TicketService {

    private static final int MAX_TICKET_NO = 20;

    private final TicketPaymentService ticketPaymentService;
    private final SeatReservationService seatReservationService;


    public TicketServiceImpl(TicketPaymentService ticketPaymentService, SeatReservationService seatReservationService) {
        this.ticketPaymentService = ticketPaymentService;
        this.seatReservationService = seatReservationService;
    }

    /**
     * Should only have private methods other than the one below.
     */
    @Override
    public void purchaseTickets(TicketPurchaseRequest ticketPurchaseRequest) throws InvalidPurchaseException {
        if (ticketPurchaseRequest == null || ticketPurchaseRequest.getTicketTypeRequests().length == 0) {
            return;
        }
        validateRequest(ticketPurchaseRequest);
        long accountId = ticketPurchaseRequest.getAccountId();
        TicketRequest[] ticketTypeRequests = ticketPurchaseRequest.getTicketTypeRequests();
        ticketPaymentService.makePayment(accountId, getTotalAmountToPay(ticketTypeRequests));
        seatReservationService.reserveSeat(accountId, getNumberOfSeats(ticketTypeRequests));
    }

    private int getTotalAmountToPay(TicketRequest[] ticketTypeRequests) {
        return getTotalPricePerTicketType(ticketTypeRequests).values().stream().mapToInt(Integer::intValue).sum();
    }

    private Map<Type, Integer> getTotalPricePerTicketType(TicketRequest[] ticketTypeRequests) {
        return Arrays.stream(ticketTypeRequests)
                .collect(toMap(TicketRequest::getTicketType, this::calculateTotalPrice, Integer::sum));
    }

    private int calculateTotalPrice(TicketRequest ticketTypeRequest) {
        return getTicketPrice(ticketTypeRequest.getTicketType()) * ticketTypeRequest.getNoOfTickets();
    }

    private int getTicketPrice(Type ticketType) {
        return switch (ticketType) {
            case ADULT -> 20;
            case CHILD -> 10;
            default -> 0;
        };
    }

    private int getNumberOfSeats(TicketRequest[] ticketPurchaseRequest) {
        return Arrays.stream(ticketPurchaseRequest)
                .filter(e -> e.getTicketType().equals(ADULT) || e.getTicketType().equals(CHILD))
                .mapToInt(TicketRequest::getNoOfTickets)
                .sum();
    }

    private void validateRequest(TicketPurchaseRequest ticketPurchaseRequest) {
        if (ticketPurchaseRequest.getAccountId() < 1) {
            throw new InvalidPurchaseException("Invalid account ID");
        }
        TicketRequest[] ticketTypeRequests = ticketPurchaseRequest.getTicketTypeRequests();
        if (Arrays.stream(ticketTypeRequests).anyMatch(e -> e.getTicketType() == null)) {
            throw new InvalidPurchaseException("Invalid ticket type");
        }
        if (!containsAdult(ticketTypeRequests)) {
            throw new InvalidPurchaseException("Child and Infant tickets cannot be purchased without purchasing an Adult ticket");
        }
        int ticketNumber = getTicketNumber(ticketTypeRequests);
        if (ticketNumber > MAX_TICKET_NO) {
            throw new InvalidPurchaseException("A maximum of 20 tickets can be purchased at a time");
        }
        if (ticketNumber < 1) {
            throw new InvalidPurchaseException("No ticket selected");
        }
    }

    private boolean containsAdult(TicketRequest[] ticketRequests) {
        return Arrays.stream(ticketRequests)
                .anyMatch(e -> ADULT.equals(e.getTicketType()));
    }

    private int getTicketNumber(TicketRequest[] ticketRequests) {
        return Arrays.stream(ticketRequests)
                .mapToInt(TicketRequest::getNoOfTickets)
                .sum();
    }

}


package com.monkeypenthouse.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monkeypenthouse.core.component.OrderIdGenerator;
import com.monkeypenthouse.core.constant.ResponseCode;
import com.monkeypenthouse.core.dto.tossPayments.ApprovePaymentResponseDto;
import com.monkeypenthouse.core.entity.*;
import com.monkeypenthouse.core.exception.CommonException;
import com.monkeypenthouse.core.repository.*;
import com.monkeypenthouse.core.vo.*;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseServiceImpl implements PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final PurchaseTicketMappingRepository purchaseTicketMappingRepository;
    private final TicketRepository ticketRepository;
    private final AmenityRepository amenityRepository;
    private final TicketStockRepository ticketStockRepository;
    private final OrderIdGenerator orderIdGenerator;
    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final RedissonClient redissonClient;
    private final RedisTemplate redisTemplate;

    @Value("${toss-payments.api-key}")
    private String tossPaymentsApiKey;

    @Override
    @Transactional
    public CreateOrderResponseVo createPurchase(final UserDetails userDetails, final CreatePurchaseRequestVo requestVo) {
        /**
         * Step 1. ?????? ????????? DB ??????
         */

        // ?????? ????????? ????????? ?????? ??? ??????
        final List<Ticket> ticketList = (List<Ticket>) ticketRepository.findAllById(
                requestVo.getPurchaseTicketMappingVoList().stream()
                        .map(purchaseTicketMappingVo -> purchaseTicketMappingVo.getTicketId())
                        .collect(Collectors.toList()));

        if (ticketList.size() != requestVo.getPurchaseTicketMappingVoList().size()) {
            throw new CommonException(ResponseCode.TICKET_NOT_FOUND);
        }

        /**
         * Step 2. ?????? ?????? ?????? ??????
         */

        // ?????? ?????? ?????? ??????
        for (final PurchaseTicketMappingVo vo : requestVo.getPurchaseTicketMappingVoList()) {
            if (
                    (Integer) redissonClient.getBucket(vo.getTicketId() + ":totalQuantity").get() <
                            (Integer) redissonClient.getBucket(vo.getTicketId() + ":purchasedQuantity").get() +
                                    vo.getQuantity()
            ) {
                throw new CommonException(ResponseCode.NOT_ENOUGH_TICKETS);
            }
        }

        /**
         * Step 3. ??? ?????? ??????, ?????? ??????, ?????? ?????? ??????
         */

        // ?????? ID : ?????? ?????? HashMap ??????
        final HashMap<Long, Integer> quantityMap = new HashMap<>();

        requestVo.getPurchaseTicketMappingVoList().stream().map(
                purchaseTicketMappingVo -> quantityMap.put(purchaseTicketMappingVo.getTicketId(), purchaseTicketMappingVo.getQuantity()));

        // amount ??????
        final int amount = ticketList.stream()
                .mapToInt(t -> quantityMap.get(t.getId()) * t.getPrice())
                .sum();

        // orderId ??????
        final String orderId = orderIdGenerator.generate();

        // Redis??? orderId:ticketIdSet, orderId: ticketQuantity ??????
        ListOperations<String, Long> listLongOperations = redisTemplate.opsForList();
        ListOperations<String, Integer> listIntegerOperations = redisTemplate.opsForList();

        for (PurchaseTicketMappingVo vo : requestVo.getPurchaseTicketMappingVoList()) {
            listLongOperations.rightPush(orderId + ":ticketIds", vo.getTicketId());
            listIntegerOperations.rightPush(orderId + ":ticketQuantity", vo.getQuantity());
        }

        // Redis??? ticketId:amenityId ??????
        ValueOperations<String, Long> valueOperations = redisTemplate.opsForValue();
        requestVo.getPurchaseTicketMappingVoList()
                .forEach(vo -> valueOperations.set(vo.getTicketId() + ":amenityId", vo.getAmenityId()));

        // orderName ??????
        String orderName = ticketList.get(0).getName();
        if (ticketList.size() > 1) {
            orderName += " ??? " + (ticketList.size() - 1) + "???";
        }

        /**
         * Step 4. Purchase ??? PurchaseTicketMapping ????????? ?????? ??? return
         */

        // Purchase ????????? ??????
        final User user = userService.getUserByEmail(userDetails.getUsername());

        final Purchase purchase = new Purchase(user, orderId, orderName, amount, OrderStatus.IN_PROGRESS);
        purchaseRepository.save(purchase);

        // PurchaseTicketMapping ????????? ??????
        ticketList.stream().map(ticket ->
                purchaseTicketMappingRepository.save(new com.monkeypenthouse.core.entity.PurchaseTicketMapping(purchase, ticket, quantityMap.get(ticket.getId())))
        );

        return CreateOrderResponseVo.builder()
                .amount(amount)
                .orderId(orderId)
                .orderName(orderName)
                .build();
    }

    @Override
    @Transactional
    public void approvePurchase(final ApproveOrderRequestVo requestVo) throws IOException, InterruptedException {

        /**
         * Step 1. orderId ????????? ?????? ?????? ????????????
         */

        ListOperations<String, Long> listLongOperations = redisTemplate.opsForList();
        ListOperations<String, Integer> listIntegerOperations = redisTemplate.opsForList();

        final List<Long> ticketIds = listLongOperations.range(requestVo.getOrderId() + ":ticketIds", 0, -1);
        final List<Integer> ticketQuantities = listIntegerOperations.range(requestVo.getOrderId() + ":ticketQuantity", 0, -1);

        /**
         * Step 2. Multi Lock ??????
         */
        // key {ticketId}:purchasedQuantity ??? ?????? ?????? ??? ?????? ??????
        final RLock multiLock = redissonClient.getMultiLock(
                ticketIds.stream().map(
                                ticketId -> redissonClient.getLock(ticketId + ":purchasedQuantity"))
                        .collect(Collectors.toList())
                        .toArray(RLock[]::new));

        try {
            // ?????? ??? ??????
            final boolean isLocked = multiLock.tryLock(2, 1, TimeUnit.SECONDS);

            if (!isLocked) {
                throw new CommonException(ResponseCode.TICKET_LOCK_FAILED);
            }

            /**
             * Step 3. ?????? ?????? ?????? ??????
             */

            // ?????? ?????? ?????? ??????
            for (int i = 0; i < ticketIds.size(); i++) {
                Long ticketId = ticketIds.get(i);
                Integer ticketQuantity = ticketQuantities.get(i);

                if (
                        (Integer) redissonClient.getBucket(ticketId + ":totalQuantity").get() <
                                (Integer) redissonClient.getBucket(ticketId + ":purchasedQuantity").get() +
                                        ticketQuantity
                ) {
                    throw new CommonException(ResponseCode.NOT_ENOUGH_TICKETS);
                }
            }

            /**
             * Step 4. orderId????????? Purchase ????????? ??????
             */

            final Purchase purchase = purchaseRepository.findByOrderId(requestVo.getOrderId())
                    .orElseThrow(() -> new CommonException(ResponseCode.ORDER_NOT_FOUND));

            /**
             * Step 5. tossPayments API ??????
             */

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.tosspayments.com/v1/payments/" + requestVo.getPaymentKey()))
                    .header("Authorization", tossPaymentsApiKey)
                    .header("Content-Type", "application/json")
                    .method("POST", HttpRequest.BodyPublishers.ofString(
                            "{\"amount\":" + requestVo.getAmount() +
                                    ",\"orderId\":\"" + requestVo.getOrderId() + "\"}"))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            final ApprovePaymentResponseDto responseDto = objectMapper.readValue(response.body(), ApprovePaymentResponseDto.class);

            ValueOperations<String, Long> valueLongOperations = redisTemplate.opsForValue();
            ValueOperations<String, Integer> valueIntegerOperations = redisTemplate.opsForValue();

            if (response.statusCode() == 200) {
                purchase.changeOrderStatus(OrderStatus.COMPLETED);

                /**
                 * Step 6. tossPayments API ?????? ??? Redis / DB ????????????
                 */

                for (int i = 0; i < ticketIds.size(); i++) {
                    Long ticketId = ticketIds.get(i);
                    Integer ticketQuantity = ticketQuantities.get(i);
                    Long amenityId = valueLongOperations.get(ticketId + ":amenityId");

                    // Redis ????????????
                    Integer newPurchasedQuantity =
                            (Integer) redissonClient.getBucket(ticketId + ":purchasedQuantity").get() + ticketQuantity;
                    redissonClient.getBucket(ticketId + ":purchasedQuantity").set(newPurchasedQuantity);

                    Integer newAmenityQuantity = valueIntegerOperations.get(amenityId + ":purchasedQuantityOfTickets") + ticketQuantity;
                    valueIntegerOperations.set(amenityId + ":purchasedQuantityOfTickets", newAmenityQuantity);

                    // DB ????????????
                    TicketStock ticketStock = ticketStockRepository.findById(ticketId)
                            .orElseThrow(() -> new CommonException(ResponseCode.TICKET_NOT_FOUND));

                    ticketStock.reduce(ticketQuantity);
                }

            } else {
                throw new CommonException(ResponseCode.ORDER_PAYMENT_NOT_APPROVED);
            }

        } catch (Exception e) {
            // ???????????? Interrupt ????????? ??? ?????? ??????

        } finally {
            /**
             * Step 7. Multi Lock ??????
             */

            multiLock.unlock();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void loadPurchaseDataOnRedis() {

        final List<Amenity> amenityList = amenityRepository.findAllWithTicketsUsingFetchJoin();

        for (Amenity amenity : amenityList) {
            List<Long> ticketIds = amenity.getTickets().stream().map(t -> t.getId()).collect(Collectors.toList());
            List<TicketStock> ticketStocks = ticketStockRepository.findAllById(ticketIds);
            amenity.getId();

            ValueOperations<String, Integer> valueOperations = redisTemplate.opsForValue();
            valueOperations.set(amenity.getId() + ":totalQuantityOfTickets", ticketStocks.stream().mapToInt(t-> t.getTotalQuantity()).sum());
            valueOperations.set(amenity.getId() + ":purchasedQuantityOfTickets", ticketStocks.stream().mapToInt(t-> t.getPurchasedQuantity()).sum());

            for (TicketStock ticketStock : ticketStocks) {
                valueOperations.set(ticketStock.getTicketId()+":totalQuantity", ticketStock.getTotalQuantity());
                valueOperations.set(ticketStock.getTicketId()+":purchasedQuantity", ticketStock.getPurchasedQuantity());
            }
        }
    }

    @Override
    @Transactional
    public void cancelPurchase(final CancelPurchaseRequestVo requestVo) {
        final Purchase purchase =
                purchaseRepository.findByOrderId(requestVo.getOrderId()).orElseThrow(() -> new CommonException(ResponseCode.ORDER_NOT_FOUND));

        if (purchase.getOrderStatus()!=OrderStatus.IN_PROGRESS) {
            throw new CommonException(ResponseCode.CANCEL_NOT_ENABLE);
        }

        purchase.changeOrderStatus(OrderStatus.CANCELLED);
    }
}

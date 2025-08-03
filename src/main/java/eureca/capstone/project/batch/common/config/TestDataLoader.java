package eureca.capstone.project.batch.common.config;

import eureca.capstone.project.batch.common.entity.Status;
import eureca.capstone.project.batch.common.entity.TelecomCompany;
import eureca.capstone.project.batch.common.repository.StatusRepository;
import eureca.capstone.project.batch.common.repository.TelecomCompanyRepository;
import eureca.capstone.project.batch.transaction_feed.entity.*;
import eureca.capstone.project.batch.transaction_feed.repository.*;
import eureca.capstone.project.batch.user.entity.User;
import eureca.capstone.project.batch.user.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Profile("test")
@RequiredArgsConstructor
public class TestDataLoader {

    private final UserRepository userRepository;
    private final DataCouponRepository dataCouponRepository;
    private final UserDataCouponRepository userDataCouponRepository;
    private final StatusRepository statusRepository;
    private final TelecomCompanyRepository telecomCompanyRepository;
    private final DataTransactionHistoryRepository dataTransactionHistoryRepository;
    private final TransactionFeedRepository transactionFeedRepository;
    private final SalesTypeRepository salesTypeRepository;
    private final EntityManager entityManager;

    @PostConstruct
    public void loadData() {
        if (userDataCouponRepository.count() > 0) return;

        TelecomCompany skt = telecomCompanyRepository.save(TelecomCompany.builder().name("SKT").build());

        Status issuedStatus = Status.builder().statusId(4L).domain("COUPON").code("ISSUED").description("발급됨").build();

//        statusRepository.saveAll(List.of(
//                Status.builder().statusId(1L).domain("FEED").code("ON_SALE").description("판매중"
//                ).build(),
//                Status.builder().statusId(2L).domain("FEED").code("COMPLETED").description(
//                        "거래완료").build(),
//                Status.builder().statusId(3L).domain("FEED").code("EXPIRED").description(
//                        "기간만료").build(),
//
//                 // UserDataCoupon Status
//                issuedStatus,
//                Status.builder().statusId(5L).domain("COUPON").code("USED").description("사용됨").build(),
//                Status.builder().statusId(6L).domain("COUPON").code("EXPIRED").description("기간 만료").build()
//        ));


//        User testUser = User.builder()
//                .email("testUser@eureca.com")
//                .password("password")
//                .nickname("PerfTestUser")
//                .phoneNumber("010-1234-5678")
//                .telecomCompany(skt)
//                .build();
//        userRepository.save(testUser);
        User testUser = userRepository.findById(1L).orElseThrow(RuntimeException::new);

        DataCoupon sampleCoupon = dataCouponRepository.save(DataCoupon.builder()
                .couponNumber(UUID.randomUUID().toString())
                .dataAmount(1000L)
                .telecomCompany(skt)
                .build());

        int totalCoupons = 1_000_000;
        int expiredCount = 100_000;
        int batchSize = 1000;

        List<UserDataCoupon> couponList = new ArrayList<>();
        LocalDateTime today = LocalDateTime.now();

        for (int i = 1; i <= totalCoupons; i++) {
            LocalDateTime expiredAt = (i <= expiredCount)
                    ? today.minusDays(1)
                    : today.plusDays(2);

            UserDataCoupon userDataCoupon = UserDataCoupon.builder()
                    .user(testUser)
                    .dataCoupon(sampleCoupon)
                    .expiresAt(expiredAt)
                    .status(issuedStatus)
                    .build();

            couponList.add(userDataCoupon);

            if (i % batchSize == 0) {
                userDataCouponRepository.saveAll(couponList);
                couponList.clear();
                if (i % 100000 == 0) System.out.println("Saved " + i + "user data coupons...");
            }
        }

        if (!couponList.isEmpty()) userDataCouponRepository.saveAll(couponList);

        System.out.println("==================================================");
        System.out.println("Test data loading finished. Total " + userDataCouponRepository.count() + " coupons are loaded.");
        System.out.println("-> Expired Coupons: " + expiredCount);
        System.out.println("-> Valid Coupons: " + (totalCoupons - expiredCount));
        System.out.println("==================================================");

    }
}
//    @PostConstruct
//    public void init() {
//        statusRepository.saveAll(List.of(
//
//                Status.builder().statusId(2L).domain("FEED").code("COMPLETED").description(
//                        "거래완료").build(),
//                Status.builder().statusId(3L).domain("FEED").code("EXPIRED").description(
//                        "기간만료").build(),
//
//                Status.builder().statusId(5L).domain("COUPON").code("USED").description("사용됨").build(),
//                Status.builder().statusId(6L).domain("COUPON").code("EXPIRED").description("기간 만료").build()
//        ));
//
//    }
//    @Transactional
//    public void loadData() {
//        if (transactionFeedRepository.count() > 0 || dataTransactionHistoryRepository.count() > 0) return;
//
//        LocalDateTime now = LocalDateTime.now();
//        LocalDateTime oneHourAgo = now.minusHours(1);
//        LocalDateTime threeHoursAgo = now.minusHours(3);
//
//        TelecomCompany skt = telecomCompanyRepository.save(TelecomCompany.builder().name("SKT").build());
//        TelecomCompany kt = telecomCompanyRepository.save(TelecomCompany.builder().name("KT").build());
//        TelecomCompany lgu = telecomCompanyRepository.save(TelecomCompany.builder().name("LG U+").build());
//        TelecomCompany[] carriers = new TelecomCompany[]{skt, kt, lgu};
//
//        Status onSale = statusRepository.save(Status.builder().statusId(1L).domain("FEED").code("ON_SALE").description("판매중"
//        ).build());
//        SalesType normal = salesTypeRepository.save(SalesType.builder().salesTypeId(1L).name("일반 판매").build());
//
////        statusRepository.saveAll(List.of(
////
////                Status.builder().statusId(2L).domain("FEED").code("COMPLETED").description(
////                        "거래완료").build(),
////                Status.builder().statusId(3L).domain("FEED").code("EXPIRED").description(
////                        "기간만료").build(),
////
////                Status.builder().statusId(5L).domain("COUPON").code("USED").description("사용됨").build(),
////                Status.builder().statusId(6L).domain("COUPON").code("EXPIRED").description("기간 만료").build()
////        ));
//
//
//        User buyer = User.builder()
//                .email("testUser@eureca.com")
//                .password("password")
//                .nickname("PerfTestUser")
//                .phoneNumber("010-1234-5678")
//                .telecomCompany(skt)
//                .build();
//        userRepository.save(buyer);
//
//        User seller = User.builder()
//                .email("testUser2@eureca.com")
//                .password("password2")
//                .nickname("PerfTestUser2")
//                .phoneNumber("010-1234-5678")
//                .telecomCompany(skt)
//                .build();
//        userRepository.save(seller);
//
//
//        // 1. TransactionFeed 1,000,000개 생성 (telecomCompany 순환, salesDataAmount 순환)
////        int totalFeeds = 1_000_000;
//        int totalFeeds = 100_000;
//        int feedBatchSize = 1_000;
//        List<TransactionFeed> feedBatch = new ArrayList<>(feedBatchSize);
//        List<TransactionFeed> savedFeeds = new ArrayList<>(totalFeeds);
//        long[] salesDataAmounts = new long[]{100L, 200L, 300L, 400L, 500L, 600L, 700L, 800L, 900L, 1000L};
//
//        for (int i = 0; i < totalFeeds; i++) {
//            TransactionFeed feed = TransactionFeed.builder()
//                    .user(seller)
//                    .title("Perf Test Feed " + i)
//                    .content("Content for feed " + i)
//                    .telecomCompany(carriers[i % carriers.length])
//                    .salesType(normal)
//                    .salesPrice(salesDataAmounts[i % salesDataAmounts.length] * 10)
//                    .salesDataAmount(salesDataAmounts[i % salesDataAmounts.length])
//                    .defaultImageNumber(1L)
//                    .expiresAt(now.plusDays(1))
//                    .status(onSale)
//                    .isDeleted(false)
//                    .build();
//            feedBatch.add(feed);
//
//            if (feedBatch.size() == feedBatchSize) {
//                saveFeedBatch(feedBatch, savedFeeds);
//                feedBatch.clear();
//            }
//        }
//        if (!feedBatch.isEmpty()) {
//            saveFeedBatch(feedBatch, savedFeeds);
//            feedBatch.clear();
//        }
//
//        // 2. 통신사별로 feed 그룹핑
//        Map<Long, List<TransactionFeed>> feedsByCarrier = new HashMap<>();
//        for (TransactionFeed f : savedFeeds) {
//            Long carrierId = f.getTelecomCompany().getTelecomCompanyId();
//            feedsByCarrier.computeIfAbsent(carrierId, k -> new ArrayList<>()).add(f);
//        }
//
//        // 3. hotFeeds: 각 통신사별로 100,000개씩 (중복 없음)
//        int totalHotFeeds = 100_000;
//        List<TransactionFeed> hotFeeds = new ArrayList<>(totalHotFeeds);
//        int basePerCarrier = totalHotFeeds / carriers.length; // 33333
//        int remainderHot = totalHotFeeds % carriers.length;    // 1
//        for (TelecomCompany carrier : List.of(skt, kt, lgu)) {
//            List<TransactionFeed> carrierFeeds = feedsByCarrier.getOrDefault(carrier.getTelecomCompanyId(), Collections.emptyList());
//            int numForCarrier = basePerCarrier + (remainderHot > 0 ? 1 : 0);
//            if (remainderHot > 0) remainderHot--;
//            if (carrierFeeds.size() < numForCarrier) {
//                throw new IllegalStateException("통신사별 feed 수가 부족합니다: " + carrier.getName()
//                        + " 필요: " + numForCarrier + " 실제: " + carrierFeeds.size());
//            }
//            hotFeeds.addAll(new ArrayList<>(carrierFeeds.subList(0, numForCarrier)));
//        }
//
//        // 4. remainingFeeds: hotFeeds 제외한 나머지 700,000개
////        Set<Long> hotFeedIds = hotFeeds.stream()
////                .map(TransactionFeed::getTransactionFeedId)
////                .collect(Collectors.toSet());
////        List<TransactionFeed> remainingFeeds = savedFeeds.stream()
////                .filter(f -> !hotFeedIds.contains(f.getTransactionFeedId()))
////                .toList(); // 예상 700,000개
//
//        // 5. DataTransactionHistory 생성 (각 Feed 당 하나씩)
//        int historyBatchSize = 1_000;
//        List<DataTransactionHistory> historyBatch = new ArrayList<>(historyBatchSize);
//
//        // a) hotFeeds -> createdAt = now -1h
//        for (TransactionFeed feed : hotFeeds) {
//            DataTransactionHistory history = DataTransactionHistory.builder()
//                    .transactionFeed(feed)
//                    .user(buyer)
//                    .transactionFinalPrice(100L)
//                    .isDeleted(false)
//                    .createdAt(oneHourAgo)
//                    .build();
//            historyBatch.add(history);
//            if (historyBatch.size() == historyBatchSize) {
//                saveHistoryBatch(historyBatch);
//                historyBatch.clear();
//            }
//        }
//
////        // b) remainingFeeds -> createdAt = now -3h
////        for (TransactionFeed feed : remainingFeeds) {
////            DataTransactionHistory history = DataTransactionHistory.builder()
////                    .transactionFeed(feed)
////                    .user(buyer)
////                    .transactionFinalPrice(100L)
////                    .isDeleted(false)
////                    .createdAt(threeHoursAgo)
////                    .build();
////            historyBatch.add(history);
////            if (historyBatch.size() == historyBatchSize) {
////                saveHistoryBatch(historyBatch);
////                historyBatch.clear();
////            }
////        }
//
//        if (!historyBatch.isEmpty()) {
//            saveHistoryBatch(historyBatch);
//            historyBatch.clear();
//        }
//
//        System.out.println("==================================================");
//        System.out.println("-> TransactionFeed count: " + savedFeeds.size());
//        System.out.println("-> DataTransactionHistory expected total: " + (hotFeeds.size()));
//        System.out.println("   * Hot total " + hotFeeds.size() + " createdAt = now -1h (통신사별 거의 균등 분배)");
//        System.out.println("   * Remaining createdAt = now -3h");
//        System.out.println("==================================================");
//    }
//
//    private void saveFeedBatch(List<TransactionFeed> feedBatch, List<TransactionFeed> savedFeeds) {
//        List<TransactionFeed> persisted = transactionFeedRepository.saveAll(feedBatch);
//        savedFeeds.addAll(persisted);
//        entityManager.flush();
//        entityManager.clear();
//    }
//
//    private void saveHistoryBatch(List<DataTransactionHistory> historyBatch) {
//        dataTransactionHistoryRepository.saveAll(historyBatch);
//        entityManager.flush();
//        entityManager.clear();
//    }
//}
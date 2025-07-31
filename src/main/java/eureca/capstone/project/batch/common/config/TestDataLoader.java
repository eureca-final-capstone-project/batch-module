package eureca.capstone.project.batch.common.config;

import eureca.capstone.project.batch.common.entity.Status;
import eureca.capstone.project.batch.common.entity.TelecomCompany;
import eureca.capstone.project.batch.common.repository.StatusRepository;
import eureca.capstone.project.batch.common.repository.TelecomCompanyRepository;
import eureca.capstone.project.batch.transaction_feed.entity.DataCoupon;
import eureca.capstone.project.batch.transaction_feed.entity.UserDataCoupon;
import eureca.capstone.project.batch.transaction_feed.repository.DataCouponRepository;
import eureca.capstone.project.batch.transaction_feed.repository.UserDataCouponRepository;
import eureca.capstone.project.batch.user.entity.User;
import eureca.capstone.project.batch.user.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@Profile("test")
@RequiredArgsConstructor
public class TestDataLoader {

    private final UserRepository userRepository;
    private final DataCouponRepository dataCouponRepository;
    private final UserDataCouponRepository userDataCouponRepository;
    private final StatusRepository statusRepository;
    private final TelecomCompanyRepository telecomCompanyRepository;

    @PostConstruct
    public void loadData() {
        if (userDataCouponRepository.count() > 0) return;

        TelecomCompany skt = telecomCompanyRepository.save(TelecomCompany.builder().name("SKT").build());

        Status issuedStatus = Status.builder().statusId(4L).domain("COUPON").code("ISSUED").description("발급됨").build();

        statusRepository.saveAll(List.of(
                Status.builder().statusId(1L).domain("FEED").code("ON_SALE").description("판매중"
                ).build(),
                Status.builder().statusId(2L).domain("FEED").code("COMPLETED").description(
                        "거래완료").build(),
                Status.builder().statusId(3L).domain("FEED").code("EXPIRED").description(
                        "기간만료").build(),

                // UserDataCoupon Status
                issuedStatus,
                Status.builder().statusId(5L).domain("COUPON").code("USED").description("사용됨").build(),
                Status.builder().statusId(6L).domain("COUPON").code("EXPIRED").description("기간 만료").build()
        ));


        User testUser = User.builder()
                .email("testUser@eureca.com")
                .password("password")
                .nickname("PerfTestUser")
                .phoneNumber("010-1234-5678")
                .telecomCompany(skt)
                .build();
        userRepository.save(testUser);

        DataCoupon sampleCoupon = dataCouponRepository.save(DataCoupon.builder()
                .couponNumber(UUID.randomUUID().toString())
                .dataAmount(1000L)
                .telecomCompany(skt)
                .build());

        int totalCoupons = 1_000_000;
        int expiredCount = 200_000;
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

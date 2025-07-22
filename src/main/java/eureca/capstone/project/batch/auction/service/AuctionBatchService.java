package eureca.capstone.project.batch.auction.service;

import eureca.capstone.project.batch.common.entity.Status;
import eureca.capstone.project.batch.common.entity.TelecomCompany;
import eureca.capstone.project.batch.common.repository.TelecomCompanyRepository;
import eureca.capstone.project.batch.common.util.ChangeTypeManager;
import eureca.capstone.project.batch.common.util.StatusManager;
import eureca.capstone.project.batch.pay.entity.PayHistory;
import eureca.capstone.project.batch.pay.entity.UserPay;
import eureca.capstone.project.batch.pay.repository.PayHistoryRepository;
import eureca.capstone.project.batch.pay.repository.UserPayRepository;
import eureca.capstone.project.batch.transaction_feed.entity.DataCoupon;
import eureca.capstone.project.batch.transaction_feed.entity.DataTransactionHistory;
import eureca.capstone.project.batch.transaction_feed.entity.TransactionFeed;
import eureca.capstone.project.batch.transaction_feed.entity.UserDataCoupon;
import eureca.capstone.project.batch.transaction_feed.repository.DataCouponRepository;
import eureca.capstone.project.batch.transaction_feed.repository.DataTransactionHistoryRepository;
import eureca.capstone.project.batch.transaction_feed.repository.TransactionFeedRepository;
import eureca.capstone.project.batch.transaction_feed.repository.UserDataCouponRepository;
import eureca.capstone.project.batch.user.entity.User;
import eureca.capstone.project.batch.user.entity.UserData;
import eureca.capstone.project.batch.user.repository.UserDataRepository;
import eureca.capstone.project.batch.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionBatchService {

    private final UserRepository userRepository;
    private final UserDataRepository userDataRepository;
    private final UserPayRepository userPayRepository;
    private final PayHistoryRepository payHistoryRepository;
    private final DataCouponRepository dataCouponRepository;
    private final DataTransactionHistoryRepository dataTransactionHistoryRepository;
    private final TransactionFeedRepository transactionFeedRepository;
    private final StatusManager statusManager;
    private final ChangeTypeManager changeTypeManager;
    private final TelecomCompanyRepository telecomCompanyRepository;
    private final UserDataCouponRepository userDataCouponRepository;

    @Transactional
    public void processWinningBid(TransactionFeed feed, User buyer, Long finalBidAmount) {
        log.info("[AuctionBatchService] 경매 낙찰 처리 시작. 판매글 ID: {}, 낙찰자 ID: {}, 최종 낙찰가: {}",
                feed.getTransactionFeedId(), buyer.getUserId(), finalBidAmount);

        User seller = feed.getUser();

        // 1. 판매자에게 페이 지급
        UserPay sellerPay = findOrCreateUserPay(seller);
        sellerPay.charge(finalBidAmount);
        userPayRepository.save(sellerPay);
        log.info("[AuctionBatchService] 판매자 {} 에게 페이 {} 지급 완료. 현재 페이: {}", seller.getUserId(), finalBidAmount, sellerPay.getPay());

        // 2. 거래 내역 생성 (구매자 대상)
        DataTransactionHistory txHistory = createAuctionTransactionHistory(buyer, feed, finalBidAmount);
        log.info("[AuctionBatchService] 거래 내역 생성 완료. 거래 내역 ID: {}, 판매글 ID: {}", txHistory.getTransactionHistoryId(), feed.getTransactionFeedId());

        // 3. 데이터 쿠폰 발급 및 UserDataCoupon 생성
        issueDataCoupon(buyer, feed);
        log.info("[AuctionBatchService] 데이터 쿠폰 발급 및 UserDataCoupon 생성 완료. 구매자: {}, 판매글 ID: {}", buyer.getUserId(), feed.getTransactionFeedId());

        // 4. 페이 변동 내역 생성
        payHistoryRepository.save(PayHistory.builder()
                .user(buyer)
                .changeType(changeTypeManager.getChangeType("구매"))
                .changedPay(-finalBidAmount)
                .finalPay(userPayRepository.findById(buyer.getUserId()).map(UserPay::getPay).orElse(0L))
                .build());
        payHistoryRepository.save(PayHistory.builder()
                .user(seller)
                .changeType(changeTypeManager.getChangeType("판매"))
                .changedPay(finalBidAmount)
                .finalPay(userPayRepository.findById(seller.getUserId()).map(UserPay::getPay).orElse(0L))
                .build());
        log.info("[AuctionBatchService] 페이 변동 내역 생성 완료. 구매자: {}, 판매자: {}, 거래 내역 ID: {}", buyer.getUserId(), seller.getUserId(), txHistory.getTransactionHistoryId());

        // 5. 판매글 상태 업데이트
        Status completedStatus = statusManager.getStatus("FEED", "COMPLETED");
        feed.changeStatus(completedStatus);
        transactionFeedRepository.save(feed);
        log.info("[AuctionBatchService] 판매글 상태 업데이트 완료. 판매글 ID: {}, 새로운 상태: {}", feed.getTransactionFeedId(), completedStatus.getCode());
    }

    @Transactional
    public void processFailedBid(TransactionFeed feed) {
        log.info("[AuctionBatchService] 경매 유찰 처리 시작. 판매글 ID: {}", feed.getTransactionFeedId());

        // 1. 판매글 상태 업데이트
        Status expiredStatus = statusManager.getStatus("FEED", "EXPIRED");
        feed.changeStatus(expiredStatus);
        transactionFeedRepository.save(feed);
        log.info("[AuctionBatchService] 판매글 상태 업데이트 완료. 판매글 ID: {}, 새로운 상태: {}", feed.getTransactionFeedId(), expiredStatus.getCode());

        // 2. 판매자의 판매 가능 데이터 환불
        User seller = feed.getUser();
        UserData sellerUserData = userDataRepository.findByUserId(seller.getUserId())
                .orElseThrow(() -> new RuntimeException("판매자 UserData를 찾을 수 없습니다. User ID: " + seller.getUserId()));
        sellerUserData.addSellableData(feed.getSalesDataAmount());
        userDataRepository.save(sellerUserData);
        log.info("[AuctionBatchService] 판매자 {} 에게 판매 데이터 {}MB 환불 완료. 현재 판매 가능 데이터: {}",
                seller.getUserId(), feed.getSalesDataAmount(), sellerUserData.getSellableDataMb());
    }

    private UserPay findOrCreateUserPay(User user) {
        return userPayRepository.findById(user.getUserId())
                .orElseGet(() -> {
                    UserPay newUserPay = new UserPay(user);
                    return userPayRepository.save(newUserPay);
                });
    }

    private DataTransactionHistory createAuctionTransactionHistory(User buyer, TransactionFeed feed, Long finalBidAmount) {
        DataTransactionHistory history = DataTransactionHistory.builder()
                .transactionFeed(feed)
                .user(buyer)
                .transactionFinalPrice(finalBidAmount)
                .isDeleted(false)
                .build();
        return dataTransactionHistoryRepository.save(history);
    }

    private void issueDataCoupon(User buyer, TransactionFeed feed) {
        TelecomCompany telecomCompany = telecomCompanyRepository.findById(feed.getTelecomCompany().getTelecomCompanyId())
                .orElseThrow(() -> new RuntimeException("TelecomCompany not found"));

        DataCoupon dataCoupon = dataCouponRepository.findByDataAmountAndTelecomCompany(feed.getSalesDataAmount(), telecomCompany)
                .orElseGet(() -> DataCoupon.builder()
                        .couponNumber(UUID.randomUUID().toString())
                        .dataAmount(feed.getSalesDataAmount())
                        .telecomCompany(telecomCompany)
                        .build());
        dataCouponRepository.save(dataCoupon);

        Status activeStatus = statusManager.getStatus("COUPON", "ISSUED");
        UserDataCoupon userDataCoupon = UserDataCoupon.builder()
                .dataCoupon(dataCoupon)
                .user(buyer)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .status(activeStatus)
                .build();
        userDataCouponRepository.save(userDataCoupon);
    }
}
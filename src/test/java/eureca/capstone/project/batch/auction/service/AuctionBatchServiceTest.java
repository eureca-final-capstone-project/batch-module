package eureca.capstone.project.batch.auction.service;

import eureca.capstone.project.batch.common.entity.Status;
import eureca.capstone.project.batch.common.entity.TelecomCompany;
import eureca.capstone.project.batch.common.repository.TelecomCompanyRepository;
import eureca.capstone.project.batch.common.util.ChangeTypeManager;
import eureca.capstone.project.batch.common.util.StatusManager;
import eureca.capstone.project.batch.pay.entity.ChangeType;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuctionBatchServiceTest {

    @Mock
    private UserDataRepository userDataRepository;
    @Mock
    private UserPayRepository userPayRepository;
    @Mock
    private PayHistoryRepository payHistoryRepository;
    @Mock
    private DataCouponRepository dataCouponRepository;
    @Mock
    private DataTransactionHistoryRepository dataTransactionHistoryRepository;
    @Mock
    private TransactionFeedRepository transactionFeedRepository;
    @Mock
    private UserDataCouponRepository userDataCouponRepository;
    @Mock
    private TelecomCompanyRepository telecomCompanyRepository;
    @Mock
    private StatusManager statusManager;
    @Mock
    private ChangeTypeManager changeTypeManager;

    @InjectMocks
    private AuctionBatchService auctionBatchService;

    // 멤버 변수 선언
    private User seller;
    private User buyer;
    private TransactionFeed auctionFeed;
    private UserPay sellerPay;
    private UserPay buyerPay;
    private UserData sellerUserData;
    private Status completedStatus, expiredStatus, activeStatus, issuedStatus;
    private ChangeType purchaseChangeType, saleChangeType;
    private TelecomCompany telecomCompany;
    private DataCoupon dataCoupon;

    @BeforeEach
    void setUp() {
        // 순수 엔티티 객체 생성만 담당
        telecomCompany = TelecomCompany.builder().telecomCompanyId(1L).name("SKT").build();
        seller = new User(1L, telecomCompany, "seller@example.com", "password", "seller", "phoneNumber", new Status(1L, "USER", "ACTIVE", "활성"), "GOOGLE");
        buyer = new User(2L, telecomCompany, "buyer@example.com", "password", "buyer", "phoneNumber", new Status(1L, "USER", "ACTIVE", "활성"), "GOOGLE");
        auctionFeed = TransactionFeed.builder()
                .transactionFeedId(100L).user(seller).salesDataAmount(1000L)
                .salesPrice(5000L).telecomCompany(telecomCompany).build();
        sellerPay = UserPay.builder().userId(seller.getUserId()).user(seller).pay(10000L).build();
        buyerPay = UserPay.builder().userId(buyer.getUserId()).user(buyer).pay(5000L).build();
        sellerUserData = UserData.builder().userId(seller.getUserId()).sellableDataMb(2000L).build();
        completedStatus = Status.builder().statusId(1L).code("COMPLETED").domain("FEED").build();
        expiredStatus = Status.builder().statusId(2L).code("EXPIRED").domain("FEED").build();
        activeStatus = Status.builder().statusId(3L).code("ACTIVE").domain("COUPON").build();
        issuedStatus = Status.builder().statusId(4L).code("ISSUED").domain("COUPON").build();
        purchaseChangeType = ChangeType.builder().changeTypeId(1L).type("구매").content("구매").build();
        saleChangeType = ChangeType.builder().changeTypeId(2L).type("판매").content("판매").build();
        dataCoupon = DataCoupon.builder().dataCouponId(1L).dataAmount(1000L).telecomCompany(telecomCompany).couponNumber("TEST_COUPON").build();
    }

    @Test
    @DisplayName("경매 낙찰 처리 성공")
    void processWinningBid_Success() {
        // Given
        Long finalBidAmount = 6000L;

        // 낙찰 성공 플로우에 필요한 모든 Mocking을 여기에 정의
        when(userPayRepository.findById(seller.getUserId())).thenReturn(Optional.of(sellerPay));
        when(userPayRepository.findById(buyer.getUserId())).thenReturn(Optional.of(buyerPay));
        when(dataCouponRepository.findByDataAmountAndTelecomCompany(anyLong(), any(TelecomCompany.class))).thenReturn(Optional.of(dataCoupon));
        when(telecomCompanyRepository.findById(telecomCompany.getTelecomCompanyId())).thenReturn(Optional.of(telecomCompany));

        when(statusManager.getStatus("FEED", "COMPLETED")).thenReturn(completedStatus);
        when(statusManager.getStatus("COUPON", "ISSUED")).thenReturn(issuedStatus);
        when(changeTypeManager.getChangeType("구매")).thenReturn(purchaseChangeType);
        when(changeTypeManager.getChangeType("판매")).thenReturn(saleChangeType);

        when(dataTransactionHistoryRepository.save(any(DataTransactionHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(payHistoryRepository.save(any(PayHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userDataCouponRepository.save(any(UserDataCoupon.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionFeedRepository.save(any(TransactionFeed.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userPayRepository.save(any(UserPay.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        auctionBatchService.processWinningBid(auctionFeed, buyer, finalBidAmount);

        // Then
        assertEquals(16000L, sellerPay.getPay());
        verify(userPayRepository, times(1)).save(sellerPay);
        verify(dataTransactionHistoryRepository, times(1)).save(any(DataTransactionHistory.class));
        verify(userDataCouponRepository, times(1)).save(any(UserDataCoupon.class));
        verify(payHistoryRepository, times(2)).save(any(PayHistory.class));
        assertEquals(completedStatus, auctionFeed.getStatus());
        verify(transactionFeedRepository, times(1)).save(auctionFeed);
    }

    @Test
    @DisplayName("경매 유찰 처리 성공")
    void processFailedBid_Success() {
        // Given
        // 유찰 성공 플로우에 필요한 모든 Mocking을 여기에 정의
        when(userDataRepository.findByUserId(seller.getUserId())).thenReturn(Optional.of(sellerUserData));
        when(statusManager.getStatus("FEED", "EXPIRED")).thenReturn(expiredStatus);

        when(transactionFeedRepository.save(any(TransactionFeed.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userDataRepository.save(any(UserData.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        auctionBatchService.processFailedBid(auctionFeed);

        // Then
        assertEquals(expiredStatus, auctionFeed.getStatus());
        verify(transactionFeedRepository, times(1)).save(auctionFeed);
        assertEquals(3000L, sellerUserData.getSellableDataMb());
        verify(userDataRepository, times(1)).save(sellerUserData);
    }
}
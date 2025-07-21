package eureca.capstone.project.batch.component.writer;

import eureca.capstone.project.batch.common.entity.TelecomCompany;
import eureca.capstone.project.batch.common.repository.TelecomCompanyRepository;
import eureca.capstone.project.batch.market_statistic.domain.MarketStatistic;
import eureca.capstone.project.batch.market_statistic.domain.TransactionAmountStatistic;
import eureca.capstone.project.batch.market_statistic.repository.MarketStatisticRepository;
import eureca.capstone.project.batch.market_statistic.repository.TransactionAmountStatisticRepository;
import eureca.capstone.project.batch.transaction_feed.domain.DataTransactionHistory;
import eureca.capstone.project.batch.transaction_feed.domain.TransactionFeed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@StepScope
@RequiredArgsConstructor
@Slf4j
public class TransactionStatisticWriter implements ItemWriter<DataTransactionHistory> {
    private static final int PRICE_PER_MB = 100; // 100MB 단위
    private final MarketStatisticRepository marketStatisticRepository;
    private final TransactionAmountStatisticRepository transactionAmountStatisticRepository;
    private final TelecomCompanyRepository telecomCompanyRepository;

    @Value("#{jobParameters['currentTime']}")
    private String currentTimeStr;
    private LocalDateTime statisticsTime;
    private List<TelecomCompany> telecomCompanies;

    @Override
    public void write(Chunk<? extends DataTransactionHistory> chunk) throws Exception {

        if (statisticsTime == null) {
            statisticsTime = LocalDateTime.parse(currentTimeStr)
                    .minusHours(1)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0);
            log.info("통계 시간: {}", statisticsTime);
        }

        if (telecomCompanies == null) {
            telecomCompanies = telecomCompanyRepository.findAll();

            if (telecomCompanies.isEmpty()) {
                log.warn("[saveMarketStatistics] 등록된 통신사가 없습니다.");
                return;
            }
        }

        // 통신사별 통계
        Map<TelecomCompany, StatisticCalculator> telecomStatistics = getTelecomStatistics(chunk.getItems());

        // 시세 통계 저장
        saveMarketStatistics(telecomStatistics);

        // 거래량 통계 저장
        saveTransactionAmountStatistics(telecomStatistics);
    }

    private Map<TelecomCompany, StatisticCalculator> getTelecomStatistics(List<? extends DataTransactionHistory> items) {
        Map<TelecomCompany, StatisticCalculator> telecomStatistics = new HashMap<>();

        for (DataTransactionHistory history : items) {

            TransactionFeed feed = history.getTransactionFeed();
            if(feed == null){
                log.warn("[getTelecomStatistics] {}번 거래내역의 TransactionFeed가 null입니다.", history.getTransactionHistoryId());
                continue;
            }
            TelecomCompany telecom = feed.getTelecomCompany();
            if(telecom == null){
                log.warn("[getTelecomStatistics] {}번 거래내역의 TelecomCompany가 null입니다.", history.getTransactionHistoryId());
                continue;
            }

            telecomStatistics.computeIfAbsent(telecom, k -> new StatisticCalculator())
                    .addTransaction(history.getTransactionFinalPrice(), history.getTransactionFeed().getSalesDataAmount());
            log.info("[통계 누적] 통신사: {}, price: {}, amount: {}", telecom.getName(), history.getTransactionFinalPrice(), feed.getSalesDataAmount());

        }
        return telecomStatistics;
    }

    private void saveMarketStatistics(Map<TelecomCompany, StatisticCalculator> telecomStatistics) {
        List<MarketStatistic> statistics = new ArrayList<>();

        for(TelecomCompany telecom : telecomCompanies){
            StatisticCalculator calc = telecomStatistics.getOrDefault(telecom, new StatisticCalculator());

            MarketStatistic statistic = MarketStatistic.builder()
                    .telecomCompany(telecom)
                    .averagePrice(calc.getAveragePrice())
                    .transactionAmount(calc.getTransactionAmount())
                    .staticsTime(statisticsTime)
                    .build();

            statistics.add(statistic);
            log.info("[saveMarketStatistics] 시세통계 time: {}. 통신사: {}", statisticsTime, statistic.getTelecomCompany().getName());
        }

        marketStatisticRepository.saveAll(statistics);
        log.info("[saveMarketStatistics] 시세통계 db 저장 완료: {}건. time: {}", statistics.size(), statisticsTime);
    }

    private void saveTransactionAmountStatistics(Map<TelecomCompany, StatisticCalculator> telecomStatistics) {

        long amount = 0L;
        for(StatisticCalculator calc : telecomStatistics.values()){
            amount += calc.getTransactionAmount();
        }

        TransactionAmountStatistic totalAmount = TransactionAmountStatistic.builder()
                .transactionAmount(amount)
                .staticsTime(statisticsTime)
                .build();

        transactionAmountStatisticRepository.save(totalAmount);
        log.info("[saveTransactionAmountStatistics] 거래량 통계 db 저장 완료. time: {}", statisticsTime);
    }



    // 시세 / 거래량 통계 계산용 내부 클래스
    private static class StatisticCalculator {
        private long totalPrice = 0;
        private long transactionCount = 0;
        private long totalDataAmount = 0;

        public void addTransaction(Long price, Long dataAmount) {
            if (price != null) {
                this.totalPrice += price;
                this.transactionCount++;
            }
            if (dataAmount != null) {
                this.totalDataAmount += dataAmount;
            }
        }

        public long getAveragePrice() {
            if(totalPrice == 0) return 0;
            double avg = (double) totalPrice / totalDataAmount * PRICE_PER_MB;
            return Math.round(avg);
        }

        public long getTransactionAmount() {
            return transactionCount;
        }
    }
}

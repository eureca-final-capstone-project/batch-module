//package eureca.capstone.project.batch.common.config;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.boot.context.event.ApplicationReadyEvent;
//import org.springframework.context.annotation.Profile;
//import org.springframework.context.event.EventListener;
//import org.springframework.stereotype.Component;
//
//@Component
//@Profile("test")
//@RequiredArgsConstructor
//public class TestDataLoaderRunner {
//
//    private final TestDataLoader testDataLoader;
//
//    @EventListener(ApplicationReadyEvent.class)
//    public void onReady() {
//        testDataLoader.loadData(); // 이 호출은 프록시를 통해 들어가므로 @Transactional이 제대로 적용됨
//    }
//}
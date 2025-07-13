package eureca.capstone.project.batch.user.controller;


import eureca.capstone.project.batch.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;


    // 오늘 일자가 같은 사용자 조회
    @GetMapping("/reset-candidates")
    public List<Long> getResetCandidates(@RequestParam int day) {
        return userService.findUsersByCreatedDay(day);
    }

    // 사용자 데이터 초기화
    @PutMapping("/reset-monthly")
    public void resetMonthlyData(@RequestBody List<Long> userIds,
                                 @RequestParam int amount) {
        userService.resetMonthlyData(userIds, amount);
    }
}


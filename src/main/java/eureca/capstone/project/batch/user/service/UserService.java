package eureca.capstone.project.batch.user.service;


import eureca.capstone.project.batch.user.entity.User;
import eureca.capstone.project.batch.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public List<Long> findUsersByCreatedDay(int day) {
        return userRepository.findByResetDate(day);
    }

    @Transactional
    public void resetMonthlyData(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return;
        }

        List<User> usersToUpdate = userRepository.findByIdIn(userIds);
        usersToUpdate.forEach(user -> user.setSellableDataMb(0));
        userRepository.saveAll(usersToUpdate);
    }

}

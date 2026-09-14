package ScoreMate.ScoreMate.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableScheduling
@EnableJpaAuditing
public class SchedulerConfig {

    /**
     * @Scheduled 작업 전용 스레드풀. 기본 스케줄러(풀 크기 1)를 그대로 쓰면
     * syncLivePitchers(경기 수만큼 순차 블로킹 크롤링, 20초 주기)가 같은 스레드를
     * 붙잡고 있는 동안 syncTodayLiveScores(10초 주기 점수 갱신)가 밀려서 지연될 수 있다.
     */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(8);
        scheduler.setThreadNamePrefix("kbo-scheduler-");
        return scheduler;
    }
}

package com.deanrobin.yx.service;

import com.deanrobin.yx.notify.Notifier;
import com.deanrobin.yx.notify.NotifyMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyDispatcher {

    private final List<Notifier> notifiers;

    /**
     * 分发通知，返回是否至少有一个渠道发送成功
     */
    public boolean dispatch(NotifyMessage message) {
        long successCount = notifiers.stream()
                .filter(Notifier::isEnabled)
                .filter(notifier -> {
                    try {
                        notifier.send(message);
                        return true;
                    } catch (Exception e) {
                        log.warn("⚠️ [Notify] {} 发送异常（已跳过）: {}", notifier.getClass().getSimpleName(), e.getMessage());
                        return false;
                    }
                })
                .count();
        return successCount > 0;
    }
}

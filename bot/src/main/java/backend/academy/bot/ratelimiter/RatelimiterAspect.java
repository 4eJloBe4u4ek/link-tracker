package backend.academy.bot.ratelimiter;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Objects;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@Aspect
@RequiredArgsConstructor
public class RatelimiterAspect {
    private final ProxyManager<String> proxyManager;
    private final Supplier<BucketConfiguration> bucketConfigSupplier;
    private static final int TOKENS_TO_CONSUME = 1;
    private static final String RATE_LIMIT_EXCEEDED_MESSAGE = "Rate limit exceeded";

    @Around("@annotation(RateLimited)")
    public Object rateLimit(ProceedingJoinPoint joinPoint) throws Throwable {
        var attrs = (ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes());
        HttpServletRequest request = attrs.getRequest();

        String clientKey = request.getRemoteAddr();

        Bucket bucket = proxyManager.builder().build(clientKey, bucketConfigSupplier.get());

        if (bucket.tryConsume(TOKENS_TO_CONSUME)) {
            return joinPoint.proceed();
        } else {
            HttpServletResponse response = attrs.getResponse();
            if (response != null) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write(RATE_LIMIT_EXCEEDED_MESSAGE);
            }
            return null;
        }
    }
}

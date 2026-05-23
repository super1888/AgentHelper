package com.spring.ai.link.application.manager;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.spring.ai.common.exception.BusinessExceptions;
import com.spring.ai.common.repository.dao.ShortLinkAccessLogRecordMapper;
import com.spring.ai.common.repository.dao.ShortLinkRecordMapper;
import com.spring.ai.common.repository.enitiy.ShortLinkAccessLogRecord;
import com.spring.ai.common.repository.enitiy.ShortLinkRecord;
import com.spring.ai.common.utils.CommonDigestUtils;
import com.spring.ai.common.utils.CommonTextUtils;
import com.spring.ai.link.application.assmbler.ShortLinkAssembler;
import com.spring.ai.link.domain.request.ShortLinkCreateRequest;
import com.spring.ai.link.domain.response.ShortLinkAccessLogResponse;
import com.spring.ai.link.domain.response.ShortLinkResponse;
import com.spring.ai.link.domain.response.ShortLinkStatisticsResponse;
import com.spring.ai.link.support.ShortLinkBloomFilter;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 短链接应用服务。
 */
@Service
public class ShortLinkApplicationManager {

    private static final String STATUS_ENABLED = "ENABLED";
    private static final String STATUS_DISABLED = "DISABLED";
    private static final String CACHE_URL_KEY_PREFIX = "short-link:url:";
    private static final String VISITOR_SET_KEY_PREFIX = "short-link:visitor:";
    private static final String IP_SET_KEY_PREFIX = "short-link:ip:";
    private static final String RATE_LIMIT_KEY_PREFIX = "short-link:rate:";
    private static final Pattern CUSTOM_CODE_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{4,32}$");
    private static final char[] BASE62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final int MAX_GENERATE_RETRY = 12;
    private static final int RATE_LIMIT_PER_MINUTE = 120;

    @Resource
    private ShortLinkRecordMapper shortLinkRecordMapper;

    @Resource
    private ShortLinkAccessLogRecordMapper shortLinkAccessLogRecordMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ShortLinkBloomFilter shortLinkBloomFilter;

    private final SecureRandom secureRandom = new SecureRandom();

    @PostConstruct
    public void warmBloomFilter() {
        try {
            List<ShortLinkRecord> records = shortLinkRecordMapper.selectList(Wrappers.<ShortLinkRecord>lambdaQuery()
                    .select(ShortLinkRecord::getShortCode)
                    .eq(ShortLinkRecord::getDeletedFlag, 0));
            records.forEach(record -> shortLinkBloomFilter.add(record.getShortCode()));
        } catch (Exception ignored) {
        }
    }

    public List<ShortLinkResponse> listLinks(String keyword) {
        return shortLinkRecordMapper.selectList(Wrappers.<ShortLinkRecord>lambdaQuery()
                        .eq(ShortLinkRecord::getDeletedFlag, 0)
                        .and(StringUtils.hasText(keyword), wrapper -> wrapper
                                .like(ShortLinkRecord::getShortCode, keyword)
                                .or()
                                .like(ShortLinkRecord::getTitle, keyword)
                                .or()
                                .like(ShortLinkRecord::getLongUrl, keyword))
                        .orderByDesc(ShortLinkRecord::getCreateTime))
                .stream()
                .map(ShortLinkAssembler::toResponse)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public ShortLinkResponse createLink(ShortLinkCreateRequest request, HttpServletRequest servletRequest) {
        String longUrl = normalizeAndValidateUrl(request.getLongUrl());
        ShortLinkRecord existed = shortLinkRecordMapper.selectOne(Wrappers.<ShortLinkRecord>lambdaQuery()
                .eq(ShortLinkRecord::getLongUrl, longUrl)
                .eq(ShortLinkRecord::getDeletedFlag, 0)
                .last("limit 1"));
        if (existed != null) {
            return ShortLinkAssembler.toResponse(existed);
        }

        String shortCode = buildShortCode(request.getCustomCode());
        String shortUrl = buildShortUrl(request.getDomain(), servletRequest, shortCode);
        ShortLinkRecord record = ShortLinkAssembler.toCreateRecord(
                request,
                longUrl,
                shortCode,
                shortUrl,
                URI.create(longUrl).getHost()
        );
        shortLinkRecordMapper.insert(record);
        cacheLink(record);
        safeBloomAdd(shortCode);
        return ShortLinkAssembler.toResponse(record);
    }

    public ShortLinkResponse getLink(Long linkId) {
        ShortLinkRecord record = shortLinkRecordMapper.selectById(linkId);
        if (record == null || Integer.valueOf(1).equals(record.getDeletedFlag())) {
            throw BusinessExceptions.notFound("短链接不存在");
        }
        return ShortLinkAssembler.toResponse(record);
    }

    @Transactional(rollbackFor = Exception.class)
    public ShortLinkResponse updateStatus(Long linkId, String status) {
        if (!Set.of(STATUS_ENABLED, STATUS_DISABLED).contains(status)) {
            throw BusinessExceptions.badRequest("不支持的短链接状态");
        }
        ShortLinkRecord record = requireRecord(linkId);
        record.setStatus(status);
        shortLinkRecordMapper.updateById(record);
        if (STATUS_ENABLED.equals(status)) {
            cacheLink(record);
        } else {
            evictLink(record.getShortCode());
        }
        return ShortLinkAssembler.toResponse(record);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteLink(Long linkId) {
        ShortLinkRecord record = requireRecord(linkId);
        record.setDeletedFlag(1);
        shortLinkRecordMapper.updateById(record);
        evictLink(record.getShortCode());
    }

    public ShortLinkStatisticsResponse statistics() {
        List<ShortLinkRecord> records = shortLinkRecordMapper.selectList(Wrappers.<ShortLinkRecord>lambdaQuery()
                .eq(ShortLinkRecord::getDeletedFlag, 0));
        return ShortLinkAssembler.toStatisticsResponse(records, LocalDateTime.now());
    }

    public List<ShortLinkAccessLogResponse> listAccessLogs(String shortCode) {
        return shortLinkAccessLogRecordMapper.selectList(Wrappers.<ShortLinkAccessLogRecord>lambdaQuery()
                        .eq(StringUtils.hasText(shortCode), ShortLinkAccessLogRecord::getShortCode, shortCode)
                        .orderByDesc(ShortLinkAccessLogRecord::getAccessTime)
                        .last("limit 100"))
                .stream()
                .map(ShortLinkAssembler::toLogResponse)
                .toList();
    }

    public void redirect(String shortCode, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!StringUtils.hasText(shortCode)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (!mightContain(shortCode)) {
            recordFailedAccess(shortCode, request, "短链接不存在");
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String ipAddress = getClientIp(request);
        if (isRateLimited(ipAddress)) {
            recordFailedAccess(shortCode, request, "访问过于频繁");
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value());
            return;
        }

        ShortLinkRecord record = findActiveRecord(shortCode);
        if (record == null) {
            recordFailedAccess(shortCode, request, "短链接不存在或已禁用");
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (record.getExpireTime() != null && record.getExpireTime().isBefore(LocalDateTime.now())) {
            evictLink(shortCode);
            recordFailedAccess(shortCode, request, "短链接已过期");
            response.sendError(HttpServletResponse.SC_GONE);
            return;
        }

        recordSuccessAccess(record, request, ipAddress);
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader(HttpHeaders.LOCATION, record.getLongUrl());
    }

    private ShortLinkRecord findActiveRecord(String shortCode) {
        String cachedUrl = getCachedUrl(shortCode);
        if (StringUtils.hasText(cachedUrl)) {
            return ShortLinkAssembler.toCachedRecord(shortCode, cachedUrl);
        }
        ShortLinkRecord record = shortLinkRecordMapper.selectOne(Wrappers.<ShortLinkRecord>lambdaQuery()
                .eq(ShortLinkRecord::getShortCode, shortCode)
                .eq(ShortLinkRecord::getDeletedFlag, 0)
                .eq(ShortLinkRecord::getStatus, STATUS_ENABLED)
                .last("limit 1"));
        if (record != null) {
            cacheLink(record);
        }
        return record;
    }

    private void recordSuccessAccess(ShortLinkRecord record, HttpServletRequest request, String ipAddress) {
        String visitorId = resolveVisitorId(request, ipAddress);
        Long uniqueVisitorCount = addSetAndSize(VISITOR_SET_KEY_PREFIX + record.getShortCode(), visitorId);
        Long uniqueIpCount = addSetAndSize(IP_SET_KEY_PREFIX + record.getShortCode(), ipAddress);
        shortLinkRecordMapper.update(null, Wrappers.<ShortLinkRecord>lambdaUpdate()
                .eq(record.getId() != null, ShortLinkRecord::getId, record.getId())
                .eq(record.getId() == null, ShortLinkRecord::getShortCode, record.getShortCode())
                .set(ShortLinkRecord::getLastAccessTime, LocalDateTime.now())
                .set(uniqueVisitorCount != null, ShortLinkRecord::getUniqueVisitorCount, uniqueVisitorCount)
                .set(uniqueIpCount != null, ShortLinkRecord::getUniqueIpCount, uniqueIpCount)
                .setSql("total_visit_count = total_visit_count + 1"));
        insertAccessLog(record.getShortCode(), record.getLongUrl(), visitorId, ipAddress, request, 1, null);
    }

    private void recordFailedAccess(String shortCode, HttpServletRequest request, String reason) {
        String ipAddress = getClientIp(request);
        insertAccessLog(shortCode, null, resolveVisitorId(request, ipAddress), ipAddress, request, 0, reason);
    }

    private void insertAccessLog(
            String shortCode,
            String longUrl,
            String visitorId,
            String ipAddress,
            HttpServletRequest request,
            Integer successFlag,
            String failReason
    ) {
        shortLinkAccessLogRecordMapper.insert(ShortLinkAssembler.toAccessLogRecord(
                shortCode,
                longUrl,
                visitorId,
                ipAddress,
                request,
                successFlag,
                failReason
        ));
    }

    private String buildShortCode(String customCode) {
        String code = CommonTextUtils.trimToNull(customCode);
        if (code != null) {
            if (!CUSTOM_CODE_PATTERN.matcher(code).matches()) {
                throw BusinessExceptions.badRequest("自定义短码仅支持 4-32 位字母、数字、下划线或短横线");
            }
            if (existsCode(code)) {
                throw BusinessExceptions.badRequest("短码已存在，请更换后重试");
            }
            return code;
        }
        for (int i = 0; i < MAX_GENERATE_RETRY; i++) {
            String generatedCode = randomBase62(8);
            if (!mightContain(generatedCode) && !existsCode(generatedCode)) {
                return generatedCode;
            }
        }
        String fallback = randomBase62(10);
        if (existsCode(fallback)) {
            throw BusinessExceptions.badRequest("短链接生成冲突，请稍后重试");
        }
        return fallback;
    }

    private boolean existsCode(String shortCode) {
        return shortLinkRecordMapper.selectCount(Wrappers.<ShortLinkRecord>lambdaQuery()
                .eq(ShortLinkRecord::getShortCode, shortCode)
                .eq(ShortLinkRecord::getDeletedFlag, 0)) > 0;
    }

    private String normalizeAndValidateUrl(String rawUrl) {
        String longUrl = CommonTextUtils.trimToNull(rawUrl);
        if (longUrl == null) {
            throw BusinessExceptions.badRequest("长链接不能为空");
        }
        URI uri;
        try {
            uri = URI.create(longUrl);
        } catch (Exception ex) {
            throw BusinessExceptions.badRequest("长链接格式不正确");
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!Set.of("http", "https").contains(scheme) || !StringUtils.hasText(uri.getHost())) {
            throw BusinessExceptions.badRequest("仅支持 http/https 长链接");
        }
        if (isUnsafeHost(uri.getHost())) {
            throw BusinessExceptions.badRequest("不允许生成内网、localhost 或非法主机短链接");
        }
        return longUrl;
    }

    private boolean isUnsafeHost(String host) {
        String normalized = host.toLowerCase(Locale.ROOT);
        if (normalized.equals("localhost") || normalized.endsWith(".localhost") || normalized.equals("::1")) {
            return true;
        }
        if (normalized.startsWith("127.") || normalized.startsWith("10.") || normalized.startsWith("0.")) {
            return true;
        }
        if (normalized.startsWith("192.168.")) {
            return true;
        }
        if (normalized.matches("172\\.(1[6-9]|2[0-9]|3[0-1])\\..*")) {
            return true;
        }
        return normalized.startsWith("169.254.");
    }

    private String buildShortUrl(String domain, HttpServletRequest request, String shortCode) {
        String baseUrl = CommonTextUtils.trimToNull(domain);
        if (baseUrl != null) {
            if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
                baseUrl = "https://" + baseUrl;
            }
        } else {
            String port = (request.getServerPort() == 80 || request.getServerPort() == 443) ? "" : ":" + request.getServerPort();
            baseUrl = request.getScheme() + "://" + request.getServerName() + port + request.getContextPath();
        }
        return baseUrl.replaceAll("/+$", "") + "/s/" + shortCode;
    }

    private boolean isRateLimited(String ipAddress) {
        try {
            String key = RATE_LIMIT_KEY_PREFIX + ipAddress;
            long count = stringRedisTemplate.opsForValue().increment(key);
            if (count == 1L) {
                stringRedisTemplate.expire(key, Duration.ofMinutes(1));
            }
            return count > RATE_LIMIT_PER_MINUTE;
        } catch (Exception ex) {
            return false;
        }
    }

    private void cacheLink(ShortLinkRecord record) {
        try {
            Duration ttl = Duration.ofDays(7);
            if (record.getExpireTime() != null) {
                Duration expireTtl = Duration.between(LocalDateTime.now(), record.getExpireTime());
                if (expireTtl.isNegative() || expireTtl.isZero()) {
                    return;
                }
                ttl = expireTtl.compareTo(ttl) < 0 ? expireTtl : ttl;
            }
            stringRedisTemplate.opsForValue().set(CACHE_URL_KEY_PREFIX + record.getShortCode(), record.getLongUrl(), ttl);
        } catch (Exception ignored) {
        }
    }

    private void evictLink(String shortCode) {
        try {
            stringRedisTemplate.delete(CACHE_URL_KEY_PREFIX + shortCode);
        } catch (Exception ignored) {
        }
    }

    private String getCachedUrl(String shortCode) {
        try {
            return stringRedisTemplate.opsForValue().get(CACHE_URL_KEY_PREFIX + shortCode);
        } catch (Exception ex) {
            return null;
        }
    }

    private Long addSetAndSize(String key, String value) {
        try {
            stringRedisTemplate.opsForSet().add(key, value);
            stringRedisTemplate.expire(key, Duration.ofDays(365));
            return stringRedisTemplate.opsForSet().size(key);
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean mightContain(String shortCode) {
        try {
            return shortLinkBloomFilter.mightContain(shortCode);
        } catch (Exception ex) {
            return true;
        }
    }

    private void safeBloomAdd(String shortCode) {
        try {
            shortLinkBloomFilter.add(shortCode);
        } catch (Exception ignored) {
        }
    }

    private ShortLinkRecord requireRecord(Long linkId) {
        ShortLinkRecord record = shortLinkRecordMapper.selectById(linkId);
        if (record == null || Integer.valueOf(1).equals(record.getDeletedFlag())) {
            throw BusinessExceptions.notFound("短链接不存在");
        }
        return record;
    }

    private String randomBase62(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(BASE62[secureRandom.nextInt(BASE62.length)]);
        }
        return builder.toString();
    }

    private String resolveVisitorId(HttpServletRequest request, String ipAddress) {
        String visitorHeader = CommonTextUtils.trimToNull(request.getHeader("X-Visitor-Id"));
        if (visitorHeader != null) {
            return CommonTextUtils.truncate(visitorHeader, 128);
        }
        String fingerprint = ipAddress + "|" + request.getHeader(HttpHeaders.USER_AGENT);
        return CommonDigestUtils.sha256Hex(fingerprint, "访问者标识生成失败");
    }

    private String getClientIp(HttpServletRequest request) {
        for (String header : List.of("X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP")) {
            String value = CommonTextUtils.trimToNull(request.getHeader(header));
            if (value != null && !"unknown".equalsIgnoreCase(value)) {
                return CommonTextUtils.trim(value.split(",")[0]);
            }
        }
        return request.getRemoteAddr();
    }
}



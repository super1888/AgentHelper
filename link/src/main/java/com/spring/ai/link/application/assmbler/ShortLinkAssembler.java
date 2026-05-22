package com.spring.ai.link.application.assmbler;

import com.spring.ai.common.repository.enitiy.ShortLinkAccessLogRecord;
import com.spring.ai.common.repository.enitiy.ShortLinkRecord;
import com.spring.ai.common.utils.CommonTextUtils;
import com.spring.ai.link.domain.request.ShortLinkCreateRequest;
import com.spring.ai.link.domain.response.ShortLinkAccessLogResponse;
import com.spring.ai.link.domain.response.ShortLinkResponse;
import com.spring.ai.link.domain.response.ShortLinkStatisticsResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 短链接对象组装器。
 */
public final class ShortLinkAssembler {

    private ShortLinkAssembler() {
    }

    /**
     * 组装短链接新增实体。
     */
    public static ShortLinkRecord toCreateRecord(
            ShortLinkCreateRequest request,
            String longUrl,
            String shortCode,
            String shortUrl,
            String sourceDomain
    ) {
        ShortLinkRecord record = new ShortLinkRecord();
        record.setShortCode(shortCode);
        record.setShortUrl(shortUrl);
        record.setLongUrl(longUrl);
        record.setTitle(CommonTextUtils.trimToNull(request.getTitle()));
        record.setDescription(CommonTextUtils.trimToNull(request.getDescription()));
        record.setDomain(sourceDomain);
        record.setStatus("ENABLED");
        record.setExpireTime(request.getExpireTime());
        record.setTotalVisitCount(0L);
        record.setUniqueVisitorCount(0L);
        record.setUniqueIpCount(0L);
        record.setDeletedFlag(0);
        return record;
    }

    /**
     * 组装 Redis 命中的临时实体，供跳转和统计更新复用。
     */
    public static ShortLinkRecord toCachedRecord(String shortCode, String longUrl) {
        ShortLinkRecord record = new ShortLinkRecord();
        record.setShortCode(shortCode);
        record.setLongUrl(longUrl);
        return record;
    }

    /**
     * 组装访问日志实体。
     */
    public static ShortLinkAccessLogRecord toAccessLogRecord(
            String shortCode,
            String longUrl,
            String visitorId,
            String ipAddress,
            HttpServletRequest request,
            Integer successFlag,
            String failReason
    ) {
        ShortLinkAccessLogRecord log = new ShortLinkAccessLogRecord();
        log.setShortCode(shortCode);
        log.setLongUrl(longUrl);
        log.setVisitorId(visitorId);
        log.setIpAddress(ipAddress);
        log.setUserAgent(CommonTextUtils.truncate(request.getHeader(HttpHeaders.USER_AGENT), 500));
        log.setReferer(CommonTextUtils.truncate(request.getHeader(HttpHeaders.REFERER), 500));
        log.setAccessTime(LocalDateTime.now());
        log.setSuccessFlag(successFlag);
        log.setFailReason(failReason);
        return log;
    }

    /**
     * 组装短链接列表和详情响应。
     */
    public static ShortLinkResponse toResponse(ShortLinkRecord record) {
        ShortLinkResponse response = new ShortLinkResponse();
        response.setId(record.getId());
        response.setShortCode(record.getShortCode());
        response.setShortUrl(record.getShortUrl());
        response.setLongUrl(record.getLongUrl());
        response.setTitle(record.getTitle());
        response.setDescription(record.getDescription());
        response.setDomain(record.getDomain());
        response.setStatus(record.getStatus());
        response.setExpireTime(record.getExpireTime());
        response.setTotalVisitCount(valueOrZero(record.getTotalVisitCount()));
        response.setUniqueVisitorCount(valueOrZero(record.getUniqueVisitorCount()));
        response.setUniqueIpCount(valueOrZero(record.getUniqueIpCount()));
        response.setLastAccessTime(record.getLastAccessTime());
        response.setCreateTime(record.getCreateTime());
        return response;
    }

    /**
     * 组装整体统计响应。
     */
    public static ShortLinkStatisticsResponse toStatisticsResponse(List<ShortLinkRecord> records, LocalDateTime now) {
        ShortLinkStatisticsResponse response = new ShortLinkStatisticsResponse();
        response.setTotalCount((long) records.size());
        response.setEnabledCount(records.stream().filter(item -> "ENABLED".equals(item.getStatus())).count());
        response.setExpiredCount(records.stream().filter(item -> item.getExpireTime() != null && item.getExpireTime().isBefore(now)).count());
        response.setTotalVisitCount(records.stream().mapToLong(item -> valueOrZero(item.getTotalVisitCount())).sum());
        response.setUniqueVisitorCount(records.stream().mapToLong(item -> valueOrZero(item.getUniqueVisitorCount())).sum());
        response.setUniqueIpCount(records.stream().mapToLong(item -> valueOrZero(item.getUniqueIpCount())).sum());
        return response;
    }

    /**
     * 组装访问日志响应。
     */
    public static ShortLinkAccessLogResponse toLogResponse(ShortLinkAccessLogRecord record) {
        ShortLinkAccessLogResponse response = new ShortLinkAccessLogResponse();
        response.setShortCode(record.getShortCode());
        response.setVisitorId(record.getVisitorId());
        response.setIpAddress(record.getIpAddress());
        response.setUserAgent(record.getUserAgent());
        response.setReferer(record.getReferer());
        response.setAccessTime(record.getAccessTime());
        response.setSuccessFlag(record.getSuccessFlag());
        response.setFailReason(record.getFailReason());
        return response;
    }

    private static long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }
}

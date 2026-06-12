package com.spring.ai.a2a;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * A2A 测试学习示例：集中演示覆盖测试中常见的 JUnit、AssertJ、Mockito、Spring Test 工具写法。
 * 这个类只用于学习测试语法，不依赖真实数据库、网络或 Spring 容器。
 */
@ExtendWith(MockitoExtension.class)
class A2aCoverageTestingLearningTest {

    @Mock
    private LearningRepository learningRepository;

    @Captor
    private ArgumentCaptor<LearningRecord> recordCaptor;

    @InjectMocks
    private LearningService learningService;

    /**
     * 每个 @Test 方法执行前都会先执行 @BeforeEach，适合放通用初始化和默认 mock 行为。
     */
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(learningService, "defaultOwner", "tenant-a");
        lenient().when(learningRepository.findLabelByCode("default-code")).thenReturn("默认标签");
    }

    /**
     * 演示 AssertJ 的 assertThat：用于验证返回值、对象属性、集合内容和字符串内容。
     */
    @Test
    @DisplayName("assertThat 示例：验证普通返回值、对象、集合和字符串")
    void assertThatChecksActualResult() {
        LearningRecord record = new LearningRecord("agent-a", "智能体 A", 3);
        List<String> capabilities = List.of("chat", "analysis", "tool-call");

        assertThat(record).isNotNull();
        assertThat(record.code()).isEqualTo("agent-a");
        assertThat(record.name()).startsWith("智能体").contains("A");
        assertThat(record.retryTimes()).isBetween(1, 5);
        assertThat(capabilities).hasSize(3).contains("chat", "analysis").doesNotContain("image");
        assertThat(record).extracting(LearningRecord::code, LearningRecord::retryTimes)
                .containsExactly("agent-a", 3);
        assertThat((Object) null).isNull();
    }

    /**
     * 演示 Mockito.mock(...)：不使用 @Mock 字段时，也可以在方法内部手动创建 mock 对象。
     */
    @Test
    @DisplayName("mock 示例：在测试方法内部手动创建模拟对象")
    void mockCreatesDependencyInsideTestMethod() {
        LearningRepository repository = mock(LearningRepository.class);
        when(repository.existsByCode("agent-a")).thenReturn(true);

        boolean exists = repository.existsByCode("agent-a");

        assertThat(exists).isTrue();
        verify(repository).existsByCode("agent-a");
    }

    /**
     * 演示 when(...).thenReturn(...)：指定 mock 对象被调用时返回什么，从而隔离外部依赖。
     */
    @Test
    @DisplayName("when thenReturn 示例：模拟依赖返回值")
    void whenThenReturnDefinesMockBehavior() {
        when(learningRepository.findLabelByCode("agent-a")).thenReturn("数据分析智能体");

        String label = learningService.queryLabel(" agent-a ");

        assertThat(label).isEqualTo("数据分析智能体");
        verify(learningRepository).findLabelByCode("agent-a");
    }

    /**
     * 演示 when(...).thenThrow(...) 和 assertThatThrownBy：覆盖异常分支时常用。
     */
    @Test
    @DisplayName("异常断言示例：验证异常类型和提示信息")
    void assertThatThrownByChecksExceptionBranch() {
        when(learningRepository.findLabelByCode("broken-agent"))
                .thenThrow(new IllegalStateException("远程智能体不可用"));

        assertThatThrownBy(() -> learningService.queryLabel("broken-agent"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("不可用");
    }

    /**
     * 演示 lenient().when(...)：宽松桩，允许某些测试方法没有用到这个 mock 配置。
     * 普通 when 如果在严格 Mockito 模式下完全没被使用，可能触发 UnnecessaryStubbingException。
     */
    @Test
    @DisplayName("lenient 示例：默认桩没有被本测试使用也不会失败")
    void lenientWhenAllowsSharedUnusedStub() {
        String generatedCode = learningService.generateCode("A2A Agent");

        assertThat(generatedCode).isEqualTo("a2a-agent");
        verify(learningRepository, never()).findLabelByCode(anyString());
    }

    /**
     * 演示 verify：验证 mock 方法是否被调用、被调用几次、是否从未调用。
     */
    @Test
    @DisplayName("verify 示例：验证依赖调用次数")
    void verifyChecksInteractionWithMock() {
        when(learningRepository.existsByCode("agent-a")).thenReturn(false);

        learningService.create(" agent-a ", "智能体 A");

        verify(learningRepository).existsByCode("agent-a");
        verify(learningRepository, times(1)).save(any(LearningRecord.class));
        verify(learningRepository, never()).deleteByCode(anyString());
    }

    /**
     * 演示 ArgumentCaptor：捕获传给 mock 的参数，再详细断言内部字段。
     */
    @Test
    @DisplayName("ArgumentCaptor 示例：捕获保存参数并验证字段")
    void argumentCaptorReadsSavedArgument() {
        when(learningRepository.existsByCode("agent-a")).thenReturn(false);

        learningService.create(" agent-a ", " 智能体 A ");

        verify(learningRepository).save(recordCaptor.capture());
        LearningRecord savedRecord = recordCaptor.getValue();
        assertThat(savedRecord.code()).isEqualTo("agent-a");
        assertThat(savedRecord.name()).isEqualTo("智能体 A");
        assertThat(savedRecord.owner()).isEqualTo("tenant-a");
    }

    /**
     * 演示 ArgumentMatchers：any 表示任意对象，anyString 表示任意字符串，eq 表示必须等于指定值。
     */
    @Test
    @DisplayName("ArgumentMatchers 示例：匹配任意参数或指定参数")
    void argumentMatchersMakeStubbingFlexible() {
        when(learningRepository.combineLabel(eq("agent-a"), anyString())).thenReturn("agent-a:任意后缀");

        String label = learningRepository.combineLabel("agent-a", "runtime-value");

        assertThat(label).isEqualTo("agent-a:任意后缀");
        verify(learningRepository).combineLabel(eq("agent-a"), anyString());
    }

    /**
     * 演示 ReflectionTestUtils：在不启动 Spring 容器时，给私有字段注入测试值。
     */
    @Test
    @DisplayName("ReflectionTestUtils 示例：设置和读取私有字段")
    void reflectionTestUtilsSetsPrivateField() {
        ReflectionTestUtils.setField(learningService, "defaultOwner", "tenant-b");

        Object owner = ReflectionTestUtils.getField(learningService, "defaultOwner");

        assertThat(owner).isEqualTo("tenant-b");
    }

    /**
     * 演示 Arrange-Act-Assert：先准备数据，再执行被测方法，最后断言结果。
     */
    @Test
    @DisplayName("TDD 三段式示例：准备、执行、断言")
    void arrangeActAssertShowsTypicalTddShape() {
        when(learningRepository.existsByCode("agent-a")).thenReturn(true);

        boolean created = learningService.createIfAbsent(" agent-a ", "智能体 A");

        assertThat(created).isFalse();
        verify(learningRepository, never()).save(any(LearningRecord.class));
    }

    private interface LearningRepository {

        String findLabelByCode(String code);

        boolean existsByCode(String code);

        void save(LearningRecord record);

        void deleteByCode(String code);

        String combineLabel(String code, String suffix);
    }

    private static class LearningService {

        private final LearningRepository learningRepository;

        private String defaultOwner;

        private final List<LearningRecord> localRecords = new ArrayList<>();

        private LearningService(LearningRepository learningRepository) {
            this.learningRepository = learningRepository;
        }

        private String queryLabel(String code) {
            return learningRepository.findLabelByCode(normalize(code));
        }

        private String generateCode(String name) {
            return normalize(name).replace(" ", "-");
        }

        private void create(String code, String name) {
            String normalizedCode = normalize(code);
            if (learningRepository.existsByCode(normalizedCode)) {
                throw new IllegalArgumentException("智能体编码已存在");
            }
            LearningRecord record = new LearningRecord(normalizedCode, name.trim(), 3, defaultOwner);
            localRecords.add(record);
            learningRepository.save(record);
        }

        private boolean createIfAbsent(String code, String name) {
            String normalizedCode = normalize(code);
            if (learningRepository.existsByCode(normalizedCode)) {
                return false;
            }
            create(normalizedCode, name);
            return true;
        }

        private String normalize(String value) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("编码不能为空");
            }
            return value.trim().toLowerCase();
        }
    }

    private record LearningRecord(String code, String name, int retryTimes, String owner) {

        private LearningRecord(String code, String name, int retryTimes) {
            this(code, name, retryTimes, null);
        }
    }
}

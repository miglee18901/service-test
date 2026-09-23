package org.example.service;

import org.example.model.TestCase;
import org.example.dao.TestCaseValidationProperties;
import org.example.controller.BaseResponse;
import org.example.repository.TestCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TestCaseServiceTest {
    private TestCaseRepository testCaseRepository;

    @BeforeEach
    void setUp() {
        testCaseRepository = mock(TestCaseRepository.class);
    }

    @Test
    void generateTestCaseName_noExistingClone_returnsFirstCloneName() {
        TestCaseService service = new TestCaseService(testCaseRepository, mock(TestCaseValidationProperties.class));

        String name = service.generateTestCaseName("Case");

        assertEquals("Case_Clone(1)", name);
        verify(testCaseRepository).findContainNameTestCase("Case_Clone(");
    }

    @Test
    void generateTestCaseName_existingClone_incrementsNumber() {
        TestCaseService service = new TestCaseService(testCaseRepository, mock(TestCaseValidationProperties.class));
        TestCase existing = new TestCase();
        existing.setName("Case_Clone(2)");
        when(testCaseRepository.findContainNameTestCase("Case_Clone(")).thenReturn(existing);

        String name = service.generateTestCaseName("Case");

        assertEquals("Case_Clone(3)", name);
    }

    @Test
    void processCloneTestCase_validSource_savesCopyUnderNewCategory() {
        TestCaseService service = new TestCaseService(testCaseRepository, mock(TestCaseValidationProperties.class));
        TestCase source = new TestCase();
        source.setId(20L);
        source.setName("Case");
        source.setState(1);
        source.setTestCaseDefinitionId(30L);
        source.setCategoryId(10L);
        source.setDomainId(1);

        service.processCloneTestCase(source, 20L, 100L, "Case_Clone(1)", "Copied", 4, 1);

        ArgumentCaptor<TestCase> captor = ArgumentCaptor.forClass(TestCase.class);
        verify(testCaseRepository).save(captor.capture());
        TestCase copy = captor.getValue();
        assertNull(copy.getId());
        assertEquals(100L, copy.getCategoryId());
        assertEquals("Case_Clone(1)", copy.getName());
        assertEquals("Copied", copy.getDescription());
        assertEquals(5, copy.getPosIndex());
        assertEquals(30L, copy.getTestCaseDefinitionId());
        assertEquals(1, copy.getDomainId());
        assertNotNull(copy.getCreateAt());
    }

    @Test
    void getBlockedValueCharacteristics_configuredValues_returnsMap() {
        TestCaseValidationProperties properties = new TestCaseValidationProperties();
        Map<String, List<String>> values = new LinkedHashMap<>();
        values.put("600100Msisdn", Arrays.asList("3000", "3001"));
        values.put("600000BalType", Arrays.asList("3000", "3001"));
        properties.setBlockedValueCharacteristics(values);
        TestCaseService service = new TestCaseService(testCaseRepository, properties);
        BaseResponse response = service.getBlockedValueCharacteristics();
        assertEquals(200, response.getStatus());
        assertEquals("Successfully", response.getMessage());
        assertSame(values, response.getData());
        assertEquals(Arrays.asList("3000", "3001"), ((Map<?, ?>) response.getData()).get("600100Msisdn"));
        assertEquals(Arrays.asList("3000", "3001"), ((Map<?, ?>) response.getData()).get("600000BalType"));
    }

    @Test
    void getBlockedValueCharacteristics_devConfiguration_returnsConfiguredMap() throws Exception {
        StandardEnvironment environment = new StandardEnvironment();
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        for (PropertySource<?> source : loader.load("application-dev", new ClassPathResource("application-dev.yml"))) {
            environment.getPropertySources().addFirst(source);
        }
        TestCaseValidationProperties properties = Binder.get(environment)
                .bind("test-case.validation", Bindable.of(TestCaseValidationProperties.class)).get();
        TestCaseService service = new TestCaseService(testCaseRepository, properties);

        BaseResponse response = service.getBlockedValueCharacteristics();

        Map<?, ?> data = (Map<?, ?>) response.getData();
        assertEquals(Arrays.asList("3000", "3001"), data.get("600100Msisdn"));
        assertEquals(Arrays.asList("3000", "3001"), data.get("600000BalType"));
    }
}

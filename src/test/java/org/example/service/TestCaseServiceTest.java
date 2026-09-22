package org.example.service;

import org.example.model.TestCase;
import org.example.repository.TestCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
        TestCaseService service = new TestCaseService(testCaseRepository);

        String name = service.generateTestCaseName("Case");

        assertEquals("Case_Clone(1)", name);
        verify(testCaseRepository).findContainNameTestCase("Case_Clone(");
    }

    @Test
    void generateTestCaseName_existingClone_incrementsNumber() {
        TestCaseService service = new TestCaseService(testCaseRepository);
        TestCase existing = new TestCase();
        existing.setName("Case_Clone(2)");
        when(testCaseRepository.findContainNameTestCase("Case_Clone(")).thenReturn(existing);

        String name = service.generateTestCaseName("Case");

        assertEquals("Case_Clone(3)", name);
    }

    @Test
    void processCloneTestCase_validSource_savesCopyUnderNewCategory() {
        TestCaseService service = new TestCaseService(testCaseRepository);
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
}
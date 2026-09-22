package org.example.service;

import org.example.dao.BaseResponse;
import org.example.model.Category;
import org.example.model.TestCase;
import org.example.repository.CategoryRepository;
import org.example.repository.TestCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CategoryServiceTest {
    private CategoryRepository categoryRepository;
    private TestCaseRepository testCaseRepository;
    private TestCaseService testCaseService;
    private CategoryService service;

    @BeforeEach
    void setUp() {
        categoryRepository = mock(CategoryRepository.class);
        testCaseRepository = mock(TestCaseRepository.class);
        testCaseService = mock(TestCaseService.class);
        service = new CategoryService(categoryRepository, testCaseRepository, testCaseService);
    }

    @Test
    void cloneCategoryTree_treeWithTestCases_clonesIntoMatchingCategories() {
        Category source = category(10L, 1L, "Source");
        Category child = category(11L, 10L, "Child");
        Category grandchild = category(12L, 11L, "Grandchild");
        when(categoryRepository.findOne(10L)).thenReturn(source);
        when(categoryRepository.findOne(11L)).thenReturn(child);
        when(categoryRepository.findOne(12L)).thenReturn(grandchild);
        when(categoryRepository.getChildCategoryByCategoryParentId(10L))
                .thenReturn(Collections.singletonList(child));
        when(categoryRepository.getChildCategoryByCategoryParentId(11L))
                .thenReturn(Collections.singletonList(grandchild));
        when(categoryRepository.getChildCategoryByCategoryParentId(12L))
                .thenReturn(Collections.emptyList());
        when(categoryRepository.getMaxPosIndex(anyLong(), anyInt())).thenReturn(0);
        AtomicLong nextId = new AtomicLong(100L);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category saved = invocation.getArgument(0);
            saved.setCategoryId(nextId.getAndIncrement());
            return saved;
        });

        TestCase rootCase = testCase(20L, 10L, "Root case");
        TestCase grandchildCase = testCase(21L, 12L, "Grandchild case");
        when(testCaseRepository.findByCategoryIdAndDomainId(10L, 1))
                .thenReturn(Collections.singletonList(rootCase));
        when(testCaseRepository.findByCategoryIdAndDomainId(11L, 1))
                .thenReturn(Collections.emptyList());
        when(testCaseRepository.findByCategoryIdAndDomainId(12L, 1))
                .thenReturn(Collections.singletonList(grandchildCase));
        when(testCaseRepository.findOne(20L)).thenReturn(rootCase);
        when(testCaseRepository.findOne(21L)).thenReturn(grandchildCase);
        when(testCaseRepository.getMaxPosIndex(anyInt(), anyLong())).thenReturn(0);
        when(testCaseService.generateTestCaseName("Root case")).thenReturn("Root case_Clone(1)");
        when(testCaseService.generateTestCaseName("Grandchild case")).thenReturn("Grandchild case_Clone(1)");

        Category clonedRoot = service.cloneCategoryTree(1, 10L, 7, "Copy", "Copied root");

        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository, times(3)).save(categoryCaptor.capture());
        Category savedRoot = categoryCaptor.getAllValues().get(0);
        Category savedChild = categoryCaptor.getAllValues().get(1);
        Category savedGrandchild = categoryCaptor.getAllValues().get(2);
        assertSame(savedRoot, clonedRoot);
        assertEquals(1L, savedRoot.getCategoryParentId());
        assertEquals(7, savedRoot.getCategoryType());
        assertEquals("Copy", savedRoot.getCategoryName());
        assertEquals(savedRoot.getCategoryId(), savedChild.getCategoryParentId());
        assertEquals(savedChild.getCategoryId(), savedGrandchild.getCategoryParentId());
        assertEquals(1, savedChild.getDomainId());

        verify(testCaseService).generateTestCaseName("Root case");
        verify(testCaseService).generateTestCaseName("Grandchild case");
        verify(testCaseService).processCloneTestCase(rootCase, 20L, savedRoot.getCategoryId(),
                "Root case_Clone(1)", "", 0, 1);
        verify(testCaseService).processCloneTestCase(grandchildCase, 21L, savedGrandchild.getCategoryId(),
                "Grandchild case_Clone(1)", "", 0, 1);
        verify(testCaseRepository, never()).save(any(TestCase.class));
    }

    @Test
    void cloneCategoryTree_rootCategory_rejectsWithoutSaving() {
        Category source = category(10L, null, "Root");
        when(categoryRepository.findOne(10L)).thenReturn(source);

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> service.cloneCategoryTree(1, 10L, 7, "Copy", ""));

        assertTrue(error.getMessage().contains("root"));
        verify(categoryRepository, never()).save(any(Category.class));
        verifyNoInteractions(testCaseRepository);
    }

    @Test
    void clone_partialFailure_marksTransactionForRollback() {
        Category source = category(10L, 1L, "Source");
        when(categoryRepository.findOne(10L)).thenReturn(source);
        when(categoryRepository.getMaxPosIndex(1L, 7)).thenReturn(0);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category saved = invocation.getArgument(0);
            saved.setCategoryId(100L);
            return saved;
        });
        when(testCaseRepository.findByCategoryIdAndDomainId(10L, 1))
                .thenThrow(new IllegalStateException("test case lookup failed"));
        PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
        SimpleTransactionStatus status = new SimpleTransactionStatus();
        when(manager.getTransaction(any())).thenReturn(status);
        ProxyFactory proxyFactory = new ProxyFactory(service);
        proxyFactory.addAdvice(new TransactionInterceptor(
                manager, new AnnotationTransactionAttributeSource()));
        CategoryService transactionalService = (CategoryService) proxyFactory.getProxy();

        BaseResponse response = transactionalService.clone(10L, 7, "Copy", "");

        assertEquals(500, response.getStatus());
        assertTrue(response.getMessage().contains("test case lookup failed"));
        assertTrue(status.isRollbackOnly());
        verify(categoryRepository).save(any(Category.class));
        verify(manager).commit(status);
    }

    @Test
    void cloneCategoryTree_existingCloneName_incrementsCategoryAndTestCaseNumber() {
        Category source = category(10L, 1L, "Source");
        TestCase original = testCase(20L, 10L, "Case");
        when(categoryRepository.findOne(10L)).thenReturn(source);
        when(categoryRepository.getMaxPosIndex(1L, 7)).thenReturn(0);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category saved = invocation.getArgument(0);
            saved.setCategoryId(100L);
            return saved;
        });
        Category latestClone = category(31L, 1L, "Source_Clone(5)");
        when(categoryRepository.findOneContainCategoryByName("Source_Clone(")).thenReturn(latestClone);
        when(categoryRepository.getChildCategoryByCategoryParentId(10L)).thenReturn(Collections.emptyList());

        when(testCaseRepository.findByCategoryIdAndDomainId(10L, 1))
                .thenReturn(Collections.singletonList(original));
        when(testCaseRepository.findOne(20L)).thenReturn(original);
        when(testCaseRepository.getMaxPosIndex(1, 100L)).thenReturn(0);
        when(testCaseService.generateTestCaseName("Case")).thenReturn("Case_Clone(3)");

        Category result = service.cloneCategoryTree(1, 10L, 7, "Source", "");

        assertEquals("Source_Clone(6)", result.getCategoryName());
        verify(testCaseService).generateTestCaseName("Case");
        verify(testCaseService).processCloneTestCase(original, 20L, result.getCategoryId(),
                "Case_Clone(3)", "", 0, 1);
        verify(testCaseRepository, never()).save(any(TestCase.class));
    }

    private Category category(Long id, Long parentId, String name) {
        Category value = new Category();
        value.setCategoryId(id);
        value.setCategoryParentId(parentId);
        value.setCategoryName(name);
        value.setCategoryType(1);
        value.setDomainId(1);
        value.setDescription("");
        return value;
    }

    private TestCase testCase(Long id, Long categoryId, String name) {
        TestCase value = new TestCase();
        value.setId(id);
        value.setCategoryId(categoryId);
        value.setDomainId(1);
        value.setName(name);
        value.setDescription("");
        value.setTestCaseDefinitionId(40L);
        value.setPosIndex(1);
        return value;
    }
}

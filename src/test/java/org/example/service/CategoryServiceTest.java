package org.example.service;

import org.example.controller.BaseResponse;
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

    @Test
    void clone_validCategory_returnsSuccessResponse() {
        Category source = category(10L, 1L, "Source");
        prepareSavedRoot(source);
        when(testCaseRepository.findByCategoryIdAndDomainId(10L, 1))
                .thenReturn(Collections.emptyList());
        when(categoryRepository.getChildCategoryByCategoryParentId(10L))
                .thenReturn(Collections.emptyList());

        BaseResponse response = service.clone(10L, 7, "Copy", "Description");

        assertEquals(200, response.getStatus());
        assertEquals("Clone successfully", response.getMessage());
        assertNotNull(response.getData());
    }

    @Test
    void cloneCategoryTree_missingRoot_rejectsRequest() {
        when(categoryRepository.findOne(10L)).thenReturn(null);

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> service.cloneCategoryTree(1, 10L, 7, "Copy", ""));

        assertEquals("Category id = 10 does not exist", error.getMessage());
    }

    @Test
    void cloneCategoryTree_rootFromAnotherDomain_rejectsRequest() {
        Category source = category(10L, 1L, "Source");
        source.setDomainId(2);
        when(categoryRepository.findOne(10L)).thenReturn(source);

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> service.cloneCategoryTree(1, 10L, 7, "Copy", ""));

        assertEquals("Category id = 10 does not exist", error.getMessage());
    }

    @Test
    void cloneCategoryTree_nullChildren_treatsCategoryAsLeaf() {
        Category source = category(10L, 1L, "Source");
        prepareSavedRoot(source);
        when(testCaseRepository.findByCategoryIdAndDomainId(10L, 1)).thenReturn(null);
        when(categoryRepository.getChildCategoryByCategoryParentId(10L)).thenReturn(null);

        Category result = service.cloneCategoryTree(1, 10L, 7, "Copy", null);

        assertEquals("Copy", result.getCategoryName());
        assertNull(result.getDescription());
        verifyNoInteractions(testCaseService);
    }

    @Test
    void cloneCategoryTree_cycle_rejectsRequest() {
        Category source = category(10L, 1L, "Source");
        prepareSavedRoot(source);
        when(testCaseRepository.findByCategoryIdAndDomainId(10L, 1))
                .thenReturn(Collections.emptyList());
        when(categoryRepository.getChildCategoryByCategoryParentId(10L))
                .thenReturn(Collections.singletonList(source));

        RuntimeException error = assertTimeoutPreemptively(java.time.Duration.ofSeconds(2),
                () -> assertThrows(RuntimeException.class,
                        () -> service.cloneCategoryTree(1, 10L, 7, "Copy", "")));

        assertEquals("Category tree contains a cycle", error.getMessage());
    }

    @Test
    void cloneCategoryTree_missingClonedParent_rejectsRequest() {
        Category source = category(10L, 1L, "Source");
        Category child = mock(Category.class);
        when(child.getCategoryId()).thenReturn(11L, 11L, 11L, 11L, 99L);
        when(child.getCategoryParentId()).thenReturn(10L);
        when(child.getCategoryName()).thenReturn("Child");
        when(child.getDescription()).thenReturn("");
        when(child.getCategoryType()).thenReturn(1);
        when(child.getDomainId()).thenReturn(1);
        prepareSavedRoot(source);
        when(categoryRepository.findOne(11L)).thenReturn(child);
        when(categoryRepository.getChildCategoryByCategoryParentId(10L))
                .thenReturn(Collections.singletonList(child));
        when(testCaseRepository.findByCategoryIdAndDomainId(anyLong(), eq(1)))
                .thenReturn(Collections.emptyList());

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> service.cloneCategoryTree(1, 10L, 7, "Copy", ""));

        assertEquals("Cannot find cloned category for name = Child", error.getMessage());
    }

    @Test
    void cloneCategoryTree_nullOrEmptyName_rejectsRequest() {
        Category source = category(10L, 1L, "Source");
        when(categoryRepository.findOne(10L)).thenReturn(source);

        RuntimeException nullName = assertThrows(RuntimeException.class,
                () -> service.cloneCategoryTree(1, 10L, 7, null, ""));
        RuntimeException emptyName = assertThrows(RuntimeException.class,
                () -> service.cloneCategoryTree(1, 10L, 7, "", ""));

        assertTrue(nullName.getMessage().contains("required"));
        assertTrue(emptyName.getMessage().contains("required"));
    }

    @Test
    void cloneCategoryTree_categoryNameOrDescriptionTooLong_rejectsRequest() {
        Category source = category(10L, 1L, "Source");
        when(categoryRepository.findOne(10L)).thenReturn(source);
        String tooLong = text(256);

        assertThrows(RuntimeException.class,
                () -> service.cloneCategoryTree(1, 10L, 7, tooLong, ""));
        assertThrows(RuntimeException.class,
                () -> service.cloneCategoryTree(1, 10L, 7, "Copy", tooLong));
    }

    @Test
    void cloneCategoryTree_missingOrWrongDomainChild_rejectsRequest() {
        Category source = category(10L, 1L, "Source");
        Category missingChild = category(11L, 10L, "Missing");
        prepareSavedRoot(source);
        when(testCaseRepository.findByCategoryIdAndDomainId(anyLong(), eq(1)))
                .thenReturn(Collections.emptyList());
        when(categoryRepository.getChildCategoryByCategoryParentId(10L))
                .thenReturn(Collections.singletonList(missingChild));
        when(categoryRepository.findOne(11L)).thenReturn(null);

        RuntimeException missing = assertThrows(RuntimeException.class,
                () -> service.cloneCategoryTree(1, 10L, 7, "Copy", ""));
        assertEquals("Category id = 11 does not exist", missing.getMessage());

        Category wrongDomainChild = category(12L, 10L, "Wrong domain");
        wrongDomainChild.setDomainId(2);
        when(categoryRepository.getChildCategoryByCategoryParentId(10L))
                .thenReturn(Collections.singletonList(wrongDomainChild));
        when(categoryRepository.findOne(12L)).thenReturn(wrongDomainChild);

        RuntimeException wrongDomain = assertThrows(RuntimeException.class,
                () -> service.cloneCategoryTree(1, 10L, 7, "Copy", ""));
        assertEquals("Category id = 12 does not exist", wrongDomain.getMessage());
    }

    @Test
    void cloneCategoryTree_duplicateNewCategoryName_rejectsRequest() {
        Category source = category(10L, 1L, "Source");
        when(categoryRepository.findOne(10L)).thenReturn(source);
        when(categoryRepository.findName("Copy")).thenReturn(category(20L, 1L, "Copy"));

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> service.cloneCategoryTree(1, 10L, 7, " Copy ", ""));

        assertEquals("Category name clone = Copy already exist", error.getMessage());
    }

    @Test
    void cloneCategoryTree_newName_escapesLikeCharactersForLookup() {
        Category source = category(10L, 1L, "Source");
        prepareSavedRoot(source);
        when(testCaseRepository.findByCategoryIdAndDomainId(10L, 1))
                .thenReturn(Collections.emptyList());
        when(categoryRepository.getChildCategoryByCategoryParentId(10L))
                .thenReturn(Collections.emptyList());

        Category result = service.cloneCategoryTree(1, 10L, 7, " Copy%_ ", "");

        assertEquals("Copy%_", result.getCategoryName());
        verify(categoryRepository).findName("Copy\\%\\_");
    }

    @Test
    void cloneCategoryTree_generatedCategoryNameTooLong_rejectsRequest() {
        String sourceName = text(247);
        Category source = category(10L, 1L, sourceName);
        when(categoryRepository.findOne(10L)).thenReturn(source);
        when(categoryRepository.findOneContainCategoryByName(sourceName + "_Clone("))
                .thenReturn(null);

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> service.cloneCategoryTree(1, 10L, 7, sourceName, ""));

        assertTrue(error.getMessage().contains("length must be between"));
    }

    @Test
    void cloneCategoryTree_testCaseValidation_rejectsInvalidValues() {
        assertTestCaseCloneFails(testCase(20L, 10L, null), null,
                "Test case Name = null is required");
        assertTestCaseCloneFails(testCase(20L, 10L, ""), null,
                "Test case Name =  is required");
        assertTestCaseCloneFails(testCase(20L, 10L, text(256)), null,
                "length must be between");

        TestCase longDescription = testCase(20L, 10L, "Case");
        longDescription.setDescription(text(256));
        assertTestCaseCloneFails(longDescription, null, "length must be between");
    }

    @Test
    void cloneCategoryTree_missingOrWrongDomainTestCase_rejectsRequest() {
        TestCase listed = testCase(20L, 10L, "Case");
        prepareRootWithListedTestCase(listed);
        when(testCaseRepository.findOne(20L)).thenReturn(null);

        RuntimeException missing = assertThrows(RuntimeException.class,
                () -> service.cloneCategoryTree(1, 10L, 7, "Copy", ""));
        assertEquals("Test case id = 20 does not exist", missing.getMessage());

        TestCase wrongDomain = testCase(20L, 10L, "Case");
        wrongDomain.setDomainId(2);
        when(testCaseRepository.findOne(20L)).thenReturn(wrongDomain);

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> service.cloneCategoryTree(1, 10L, 7, "Copy", ""));
        assertEquals("Test case id = 20 does not exist", error.getMessage());
    }

    @Test
    void cloneCategoryTree_renamedTestCase_clonesWhenUniqueAndRejectsDuplicate() {
        TestCase listed = testCase(20L, 10L, "Requested");
        TestCase stored = testCase(20L, 10L, "Original");
        prepareRootWithListedTestCase(listed);
        when(testCaseRepository.findOne(20L)).thenReturn(stored);
        when(testCaseRepository.findName("Requested")).thenReturn(stored, null);

        RuntimeException duplicate = assertThrows(RuntimeException.class,
                () -> service.cloneCategoryTree(1, 10L, 7, "Copy", ""));
        assertTrue(duplicate.getMessage().contains("already exist"));

        Category result = service.cloneCategoryTree(1, 10L, 7, "Copy", "");

        verify(testCaseService).processCloneTestCase(stored, 20L, result.getCategoryId(),
                "Requested", "", 0, 1);
    }

    @Test
    void cloneCategoryTree_renamedTestCase_escapesLikeCharactersForLookup() {
        TestCase listed = testCase(20L, 10L, " Requested%_ ");
        TestCase stored = testCase(20L, 10L, "Original");
        prepareRootWithListedTestCase(listed);
        when(testCaseRepository.findOne(20L)).thenReturn(stored);

        Category result = service.cloneCategoryTree(1, 10L, 7, "Copy", "");

        verify(testCaseRepository).findName("Requested\\%\\_");
        verify(testCaseService).processCloneTestCase(stored, 20L, result.getCategoryId(),
                "Requested%_", "", 0, 1);
    }

    @Test
    void cloneCategoryTree_generatedTestCaseNameTooLong_rejectsRequest() {
        TestCase listed = testCase(20L, 10L, "Case");
        prepareRootWithListedTestCase(listed);
        when(testCaseRepository.findOne(20L)).thenReturn(listed);
        when(testCaseService.generateTestCaseName("Case")).thenReturn(text(256));

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> service.cloneCategoryTree(1, 10L, 7, "Copy", ""));

        assertTrue(error.getMessage().contains("length must be between"));
    }

    @Test
    void cloneCategoryTree_testCaseWithNullDescription_clonesSuccessfully() {
        TestCase listed = testCase(20L, 10L, "Case");
        listed.setDescription(null);
        prepareRootWithListedTestCase(listed);
        when(testCaseRepository.findOne(20L)).thenReturn(listed);
        when(testCaseService.generateTestCaseName("Case")).thenReturn("Case_Clone(1)");

        Category result = service.cloneCategoryTree(1, 10L, 7, "Copy", "");

        verify(testCaseService).processCloneTestCase(listed, 20L, result.getCategoryId(),
                "Case_Clone(1)", null, 0, 1);
    }

    @Test
    void generateCategoryName_noCloneOrNameWithoutNumber_startsAtOne() {
        when(categoryRepository.findOneContainCategoryByName("Source_Clone("))
                .thenReturn(null);
        assertEquals("Source_Clone(1)", service.generateCategoryName("Source"));

        Category malformed = category(30L, 1L, "Source_Clone(latest)");
        when(categoryRepository.findOneContainCategoryByName("Source_Clone("))
                .thenReturn(malformed);
        assertEquals("Source_Clone(1)", service.generateCategoryName("Source"));
    }

    private void prepareSavedRoot(Category source) {
        when(categoryRepository.findOne(source.getCategoryId())).thenReturn(source);
        when(categoryRepository.getMaxPosIndex(anyLong(), anyInt())).thenReturn(0);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category saved = invocation.getArgument(0);
            saved.setCategoryId(100L);
            return saved;
        });
    }

    private void prepareRootWithListedTestCase(TestCase listed) {
        Category source = category(10L, 1L, "Source");
        prepareSavedRoot(source);
        when(testCaseRepository.findByCategoryIdAndDomainId(10L, 1))
                .thenReturn(Collections.singletonList(listed));
        when(testCaseRepository.getMaxPosIndex(1, 100L)).thenReturn(0);
        when(categoryRepository.getChildCategoryByCategoryParentId(10L))
                .thenReturn(Collections.emptyList());
    }

    private void assertTestCaseCloneFails(TestCase listed, TestCase stored, String message) {
        reset(categoryRepository, testCaseRepository, testCaseService);
        prepareRootWithListedTestCase(listed);
        if (stored != null) {
            when(testCaseRepository.findOne(listed.getId())).thenReturn(stored);
        }

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> service.cloneCategoryTree(1, 10L, 7, "Copy", ""));

        assertTrue(error.getMessage().contains(message));
    }

    private String text(int length) {
        return new String(new char[length]).replace('\0', 'a');
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

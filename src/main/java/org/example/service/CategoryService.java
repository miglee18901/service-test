package org.example.service;

import org.example.dao.BaseResponse;
import org.example.model.Category;
import org.example.model.TestCase;
import org.example.repository.CategoryRepository;
import org.example.repository.TestCaseRepository;
import org.example.utils.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final TestCaseRepository testCaseRepository;
    private final TestCaseService testCaseService;

    public CategoryService(CategoryRepository categoryRepository, TestCaseRepository testCaseRepository,
                           TestCaseService testCaseService) {
        this.categoryRepository = categoryRepository;
        this.testCaseRepository = testCaseRepository;
        this.testCaseService = testCaseService;
    }

    @Transactional(rollbackFor = Exception.class)
    public BaseResponse clone(Long id, Integer categoryType, String name, String description) {
        Integer domainId = SecurityUtil.getDomainId();
        try {
            Category categoryClone = cloneCategoryTree(domainId, id, categoryType, name, description);
            return new BaseResponse(200, "Clone successfully", categoryClone);
        } catch (Exception ex) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new BaseResponse(500, "Clone fail! " + ex.getMessage(), null);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Category cloneCategoryTree(Integer domainId, Long rootCategoryId, Integer categoryType, String name, String description) {
        Category categoryRoot = categoryRepository.findOne(rootCategoryId);
        if (categoryRoot == null || !domainId.equals(categoryRoot.getDomainId())) {
            throw new RuntimeException("Category id = " + rootCategoryId + " does not exist");
        }
        Map<Long, Category> mapping = new HashMap<>();
        Category newCategoryRoot = cloneCategory(domainId, categoryRoot.getCategoryId(), categoryRoot.getCategoryParentId(), name, description, categoryType);
        mapping.put(categoryRoot.getCategoryId(), newCategoryRoot);
        cloneTestCasesOfCategory(domainId, categoryRoot.getCategoryId(), newCategoryRoot.getCategoryId());
        Queue<Category> queue = new ArrayDeque<>();
        queue.offer(categoryRoot);
        while (!queue.isEmpty()) {
            Category oldParent = queue.poll();
            Category newCategoryParent = mapping.get(oldParent.getCategoryId());
            if (newCategoryParent == null) {
                throw new RuntimeException("Cannot find cloned category for name = " + oldParent.getCategoryName());
            }
            List<Category> children = categoryRepository.getChildCategoryByCategoryParentId(oldParent.getCategoryId());
            if (children == null) {
                continue;
            }
            for (Category oldChild : children) {
                if (mapping.containsKey(oldChild.getCategoryId())) {
                    throw new RuntimeException("Category tree contains a cycle");
                }
                Category newChild = cloneCategory(domainId, oldChild.getCategoryId(), newCategoryParent.getCategoryId()
                        , oldChild.getCategoryName(), oldChild.getDescription(), oldChild.getCategoryType());
                mapping.put(oldChild.getCategoryId(), newChild);
                cloneTestCasesOfCategory(domainId, oldChild.getCategoryId(), newChild.getCategoryId());
                queue.offer(oldChild);
            }
        }
        return newCategoryRoot;
    }

    private void cloneTestCasesOfCategory(Integer domainId, Long oldCategoryId, Long newCategoryId) {
        List<TestCase> testCases = testCaseRepository.findByCategoryIdAndDomainId(oldCategoryId, domainId);
        if (testCases == null || testCases.isEmpty()) {
            return;
        }
        for (TestCase oldTestCase : testCases) {
            cloneTestCase(domainId, oldTestCase.getId(), newCategoryId, oldTestCase.getName(), oldTestCase.getDescription());
        }
    }

    private Category cloneCategory(Integer domainId, Long categoryId, Long newCategoryParentId, String name, String description, Integer categoryType) {
        if (name == null || name.isEmpty()) {
            throw new RuntimeException("Name = " + name + " is required");
        }
        if (name.length() > 255 || description != null && description.length() > 255) {
            throw new RuntimeException("Name = " + name + " or description = " + description + " length must be between 0 and 255");
        }
        Category category = categoryRepository.findOne(categoryId);
        if (category == null || !domainId.equals(category.getDomainId())) {
            throw new RuntimeException("Category id = " + categoryId + " does not exist");
        }
        if (category.getCategoryParentId() == null) {
            throw new RuntimeException("Category name = " + category.getCategoryName() + " must be different from the root");
        }
        String nameClone;
        int posIndex = categoryRepository.getMaxPosIndex(newCategoryParentId, categoryType);
        if (category.getCategoryName().trim().equals(name.trim())) {
            nameClone = generateCategoryName(name.trim());
        } else {
            String nameTemp = name.trim();
            if (categoryRepository.existsByCategoryNameAndDomainId(nameTemp, domainId)) {
                throw new RuntimeException("Category name clone = " + nameTemp + " already exist");
            }
            nameClone = nameTemp;
        }
        if (nameClone.length() > 255) {
            throw new RuntimeException("Category name clone = " + nameClone + " length must be between 0 and 255");
        }
        Category categoryClone = new Category();
        categoryClone.setCategoryType(categoryType);
        categoryClone.setCategoryName(nameClone);
        categoryClone.setCategoryParentId(newCategoryParentId);
        categoryClone.setRemark(category.getRemark());
        categoryClone.setTreeType(category.getTreeType());
        categoryClone.setDomainId(domainId);
        categoryClone.setPosIndex(posIndex + 1);
        categoryClone.setDescription(description);
        return categoryRepository.save(categoryClone);
    }

    private void cloneTestCase(Integer domainId, Long testCaseId, Long newCategoryParentId, String name, String description) {
        if (name == null || name.isEmpty()) {
            throw new RuntimeException("Test case Name = " + name + " is required");
        }
        if (name.length() > 255 || description != null && description.length() > 255) {
            throw new RuntimeException("Test case name = " + name + " or description = " + description + " length must be between 0 and 255");
        }
        TestCase testCase = testCaseRepository.findOne(testCaseId);
        if (testCase == null || !domainId.equals(testCase.getDomainId())) {
            throw new RuntimeException("Test case id = " + testCaseId + " does not exist");
        }
        String nameClone;
        int posIndex = testCaseRepository.getMaxPosIndex(domainId, newCategoryParentId);
        if (testCase.getName().trim().equals(name.trim())) {
            nameClone = testCaseService.generateTestCaseName(name.trim());
        } else {
            String nameTemp = name.trim();
            if (testCaseRepository.existsByNameAndDomainId(nameTemp, domainId)) {
                throw new RuntimeException("Test case name clone = " + nameTemp
                        + " already exist, please try again");
            }
            nameClone = nameTemp;
        }
        if (nameClone.length() > 255) {
            throw new RuntimeException("Test case name clone = " + nameClone
                    + " length must be between 0 and 255");
        }
        testCaseService.processCloneTestCase(testCase, testCaseId, newCategoryParentId,
                nameClone, description, posIndex, domainId);
    }

    public String generateCategoryName(String name) {
        Category category = this.categoryRepository.findOneContainCategoryByName(name + "_Clone(");
        if (category == null) {
            return name + "_Clone(1)";
        }
        Pattern pattern = Pattern.compile("\\((\\d+)\\)");
        Matcher matcher = pattern.matcher(category.getCategoryName());
        if (matcher.find()) {
            String number = matcher.group(1);
            int numberParse = Integer.parseInt(number);
            return name + "_Clone(" + (numberParse + 1) + ")";
        }
        return name + "_Clone(1)";
    }

}
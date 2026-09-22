package org.example.service;

import org.example.model.TestCase;
import org.example.repository.TestCaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TestCaseService {
    private final TestCaseRepository testCaseRepository;

    public TestCaseService(TestCaseRepository testCaseRepository) {
        this.testCaseRepository = testCaseRepository;
    }

    public String generateTestCaseName(String name) {
        TestCase testCase = this.testCaseRepository.findContainNameTestCase(name + "_Clone(");
        if (testCase == null) {
            return name + "_Clone(1)";
        }
        Pattern pattern = Pattern.compile("\\((\\d+)\\)");
        Matcher matcher = pattern.matcher(testCase.getName());
        if (matcher.find()) {
            String number = matcher.group(1);
            int numberParse = Integer.parseInt(number);
            return name + "_Clone(" + (numberParse + 1) + ")";
        }
        return name + "_Clone(1)";
    }

    @Transactional(rollbackFor = Exception.class)
    public void processCloneTestCase(TestCase source, Long sourceId, Long newCategoryId,
                                     String name, String description, Integer posIndex, Integer domainId) {
        TestCase clone = new TestCase();
        clone.setName(name);
        clone.setDescription(description);
        clone.setState(source.getState());
        clone.setVersion(source.getVersion());
        clone.setVersionDescription(source.getVersionDescription());
        clone.setOfferVersionId(source.getOfferVersionId());
        clone.setTimeout(source.getTimeout());
        clone.setCategoryId(newCategoryId);
        clone.setTestCaseDefinitionId(source.getTestCaseDefinitionId());
        clone.setPosIndex(posIndex + 1);
        clone.setDomainId(domainId);
        clone.setCreateAt(new Date());
        clone.setUpdateDate(new Date());
        testCaseRepository.save(clone);
    }
}